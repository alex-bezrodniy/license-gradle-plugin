package nl.javadude.gradle.plugins.license

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.collect.Sets.newHashSet
import static DependencyMetadata.noLicenseMetaData

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

    private static final Logger logger = Logging.getLogger(LicenseResolver);

    private static final String DEFAULT_CONFIGURATION_TO_HANDLE = "runtime"

    /**
     * Reference to gradle project.
     */
    private Project project

    /**
     * Provide set with dependencies metadata.
     *
     * For cases when we have no license information we try to use licenses file that can contains licenses.
     * Otherwise we put 'No license was found' into report and group dependencies without licenses.
     *
     * @param licenses property file with missing licenses for some dependencies
     * @return set with licenses
     */
    public Set<DependencyMetadata> provideLicenseMap4Dependencies(Map<String, Object> licenses,
                                                                  Map<LicenseMetadata, List<String>> aliases) {
        Set<DependencyMetadata> licenseSet = newHashSet()

        // Resolve each dependency
        resolveProjectDependencies(project).each {
            String dependencyDesc = "$it.moduleVersion.id.group:$it.moduleVersion.id.name:$it.moduleVersion.id.version"
            if (licenses.containsKey(dependencyDesc)) {
                def license = licenses[dependencyDesc]
                def licenseMetadata = license instanceof String ? DownloadLicensesExtension.license(license) : license
                licenseSet << new DependencyMetadata(
                        dependency: dependencyDesc, licenseMetadataList: [ licenseMetadata ]
                )
            } else {
                licenseSet << retrieveLicensesForDependency(dependencyDesc, aliases)
            }
        }

        provideFileDependencies().each {
            fileDependency ->
                Closure<DependencyMetadata> licenseMetadata = {
                    if (licenses.containsKey(fileDependency)) {
                        def license = licenses[fileDependency]
                        LicenseMetadata licenseMetadata = license instanceof String ? DownloadLicensesExtension.license(license) : license
                        def alias =  aliases.find { it.value.contains(licenseMetadata.licenseName) }
                        if (alias) {
                            licenseMetadata.licenseName = alias.key.licenseName
                            if(alias.key.licenseTextUrl) {
                                licenseMetadata.licenseTextUrl = alias.key.licenseTextUrl
                            }
                        }
                        new DependencyMetadata(dependency: fileDependency, licenseMetadataList: [licenseMetadata])
                    } else {
                        noLicenseMetaData(fileDependency)
                    }
                }

                licenseSet << licenseMetadata()
        }

        licenseSet
    }

    /**
     * Provide full list of resolved artifacts to handle for a given project.
     *
     * @param project                       the project
     * @return Set with resolved artifacts
     */
    Set<ResolvedArtifact> resolveProjectDependencies(Project project) {

        Set<ResolvedArtifact> dependenciesToHandle = newHashSet()
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString()}

        if (project.configurations.any { it.name == DEFAULT_CONFIGURATION_TO_HANDLE }) {
            def runtimeConfiguration = project.configurations.getByName(DEFAULT_CONFIGURATION_TO_HANDLE)
            runtimeConfiguration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact d ->
                String dependencyDesc = "$d.moduleVersion.id.group:$d.moduleVersion.id.name:$d.moduleVersion.id.version".toString()
                Project subproject = subprojects[dependencyDesc]?.first()
                if (subproject) {
                    dependenciesToHandle.addAll(resolveProjectDependencies(subproject))
                } else if (!subproject) {
                    dependenciesToHandle.add(d)
                }
            }
        }

        logger.debug("Project $project.name found ${dependenciesToHandle.size()} dependencies to handle.")
        dependenciesToHandle
    }

    Set<String> provideFileDependencies() {
        Set<String> fileDependencies = newHashSet()

        if (project.configurations.any { it.name == DEFAULT_CONFIGURATION_TO_HANDLE }) {
            def configuration = project.configurations.getByName(DEFAULT_CONFIGURATION_TO_HANDLE)

            Set<Dependency> d = configuration.allDependencies.findAll {
                it instanceof FileCollectionDependency
            }

            d.each {
                FileCollectionDependency fileDependency ->
                    fileDependency.source.files.each {
                        fileDependencies.add(it.name)
                    }
            }

        }

        fileDependencies
    }

    /**
     * Recursive function for retrieving licenses via creating
     * and resolving detached configuration with "pom" extension.
     *
     * If no license was found in pom, we try to find it in parent pom declaration.
     * Parent pom descriptors are resolved in recursive way until we have no parent.
     *
     * Implementation note: We rely that while resolving configuration with one dependency we get one pom.
     * Otherwise we have IllegalStateException
     *
     * @param dependencyDesc dependency description
     * @param aliases alias mapping for similar license names
     * @param initialDependency base dependency (not parent)
     * @return dependency metadata, includes license info
     */
    private DependencyMetadata retrieveLicensesForDependency(String dependencyDesc,
                                                             Map<LicenseMetadata, List<String>> aliases,
                                                             String initialDependency = dependencyDesc) {
        Dependency d = project.dependencies.create("$dependencyDesc@pom")
        Configuration pomConfiguration = project.configurations.detachedConfiguration(d)

        File pStream = pomConfiguration.resolve().asList().first()
        GPathResult xml = new XmlSlurper().parse(pStream)
        DependencyMetadata pomData = new DependencyMetadata(dependency: initialDependency)

        xml.licenses.license.each {
            def license = new LicenseMetadata(licenseName: it.name.text().trim(), licenseTextUrl: it.url.text().trim())
            def alias =  aliases.find { it.value.contains(license.licenseName) }
            if (alias != null) {
                license = alias.key
            }
            pomData.addLicense(license)
        }

        if(pomData.hasLicense()) {
            pomData
        } else if(!isNullOrEmpty(xml.parent.text())) {
            String parentGroup = xml.parent.groupId.text().trim()
            String parentName = xml.parent.artifactId.text().trim()
            String parentVersion = xml.parent.version.text().trim()

            retrieveLicensesForDependency("$parentGroup:$parentName:$parentVersion", aliases, initialDependency)
        } else {
            noLicenseMetaData(dependencyDesc)
        }
    }

}
