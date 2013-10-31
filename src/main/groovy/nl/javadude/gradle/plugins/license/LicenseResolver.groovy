package nl.javadude.gradle.plugins.license

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.google.common.base.Preconditions.checkState
import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.collect.Sets.newHashSet
import static DependencyMetadata.noLicenseMetaData

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

    private static Logger logger = Logging.getLogger(LicenseResolver);

    private static final String DEFAULT_CONFIGURATION_TO_HANDLE = "runtime"

    /**
     * Reference to gradle project.
     */
    private Project project

    /**
     * Provide multimap with dependencies and it's licenses.
     * Multimap is used as we have many-2-many between dependencies and licenses.
     *
     * Keys are presented in the next format : "group-name-version", values - as list of licenses.
     *
     * If includeTransitiveDependencies is enabled we include to map transitive dependencies.
     * For cases when we have no license information we try to use missingLicensesProps file that can contains licenses.
     * Otherwise we put 'License wasn't found' into report and group dependencies without licenses.
     *
     * @param missingLicensesProps property file with missing licenses for some dependencies
     * @param includeTransitiveDependencies if enabled we include transitive dependencies
     * @return set with licenses
     */
    public HashSet<DependencyMetadata> provideLicenseMap4Dependencies(Map<String, LicenseMetadata> missingLicensesProps,
                                                                      Map<LicenseMetadata, LicenseMetadata> aliases,
                                                                      boolean includeTransitiveDependencies) {
        Set<DependencyMetadata> licenseSet = newHashSet()

        // Resolve each dependency
        resolveProjectDependencies(project, includeTransitiveDependencies).each {
            String dependencyDesc = "$it.moduleVersion.id.group:$it.moduleVersion.id.name:$it.moduleVersion.id.version"
            if (missingLicensesProps.containsKey(dependencyDesc)) {
                licenseSet << new DependencyMetadata(dependency: dependencyDesc, licenseMetadataList: [ missingLicensesProps[dependencyDesc] ])
            } else {
                licenseSet << retrieveLicensesForDependency(dependencyDesc, aliases)
            }
        }

        provideFileDependencies().each {
            fileDependency ->
                Closure<DependencyMetadata> licenseMetadata = {
                    if (missingLicensesProps.containsKey(it)) {
                        new DependencyMetadata(dependency: fileDependency, licenseMetadataList: [missingLicensesProps[it]])
                    } else {
                        noLicenseMetaData(fileDependency)
                    }
                }

                licenseSet << licenseMetadata()
        }
        licenseSet
    }

    /**
     * Provide full list of dependencies to handle for a given project.
     *
     * @param project                       the project
     * @param includeTransitiveDependencies whether include or not transitive dependencies
     * @return Set with resolved artifacts
     */
    Set<ResolvedArtifact> resolveProjectDependencies(Project project, includeTransitiveDependencies) {
        Set<ResolvedArtifact> dependenciesToHandle = newHashSet()
        if (project.configurations.any { it.name == DEFAULT_CONFIGURATION_TO_HANDLE }) {
            def runtimeConfiguration = project.configurations.getByName(DEFAULT_CONFIGURATION_TO_HANDLE)
            def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString()}
            runtimeConfiguration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact d ->
                String dependencyDesc = "$d.moduleVersion.id.group:$d.moduleVersion.id.name:$d.moduleVersion.id.version".toString()
                Project subproject = subprojects[dependencyDesc]?.first()
                if (subproject && includeTransitiveDependencies) {
                    dependenciesToHandle.addAll(resolveProjectDependencies(subproject, includeTransitiveDependencies))
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

        project.configurations.each {
            Configuration configuration ->
                Set<Dependency> d = configuration.allDependencies.findAll {
                    it instanceof FileCollectionDependency
                }
                d.each {
                    FileCollectionDependency fileDependency -> fileDependency.source.files.each {
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
     * @param group dependency group
     * @param name dependency name
     * @param version dependency version
     * @return when found license\s return list with them, otherwise return empty list
     */
    private DependencyMetadata retrieveLicensesForDependency(String dependencyDesc, Map<LicenseMetadata, LicenseMetadata> aliases, String initialDependency = dependencyDesc) {
        Dependency d = project.dependencies.create("$dependencyDesc@pom")
        Configuration pomConfiguration = project.configurations.detachedConfiguration(d)

        List<File> filesByPom = pomConfiguration.resolve().asList()

        int filesByPomCount = filesByPom.size()
        checkState(!filesByPom.empty || filesByPomCount == 1, "Error while resolving configuration, " +
                "dependency resolved with wrong number of files - $filesByPomCount")

        File pStream = filesByPom.first()
        GPathResult xml = new XmlSlurper().parse(pStream)

        DependencyMetadata pomData = new DependencyMetadata(dependency: initialDependency)

        xml.licenses.license.each {
            def license = new LicenseMetadata(licenseName: it.name.text().trim(), licenseTextUrl: it.url.text().trim())
            if (aliases[license]) {
                license = aliases[license]
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
