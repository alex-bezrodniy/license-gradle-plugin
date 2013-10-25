package nl.javadude.gradle.plugins.license

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact

import static com.google.common.base.Preconditions.checkState
import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.collect.Sets.newHashSet
import static DependencyMetadata.noLicenseMetaData

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

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
                                                                      boolean includeTransitiveDependencies) {
        Set<DependencyMetadata> licenseSet = newHashSet()

        Closure<Set<String>> projectNonTransitiveDependencies = { provideNonTransitiveDependencies() }
        Closure<Set<String>> ignoreDependencies = { provideIgnoredDependencies() }
        Closure<Set<ResolvedArtifact>> dependenciesToHandle = {
            provideDependenciesToHandle(ignoreDependencies,
                    projectNonTransitiveDependencies,
                    includeTransitiveDependencies)
        }
        Set<String> fileDependencies = provideFileDependencies()

        // Resolve each dependency
        dependenciesToHandle().each {
            String dependencyDesc = "$it.moduleVersion.id.group:$it.moduleVersion.id.name:$it.moduleVersion.id.version"

            Closure<DependencyMetadata> licenseMetadata = {
                if (missingLicensesProps.containsKey(dependencyDesc)) {
                    new DependencyMetadata(dependency: dependencyDesc,
                                    licenseMetadataList: [ missingLicensesProps[dependencyDesc] ])
                } else {
                    retrieveLicensesForDependency(dependencyDesc)
                }

            }

            licenseSet << licenseMetadata()
        }

        fileDependencies.each {
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
     * Provide full list of dependencies to handle.
     * Filter ignored, transitive dependencies if needed.
     *
     * @param ignoreDependencies dependencies to ignore
     * @param projectNonTransitiveDependencies list of project dependencies (non-transitive)
     * @param includeTransitiveDependencies whether include or not transitive dependencies
     * @return Set with resolved artifacts
     */
    Set<ResolvedArtifact> provideDependenciesToHandle(ignoreDependencies,
                                                      projectNonTransitiveDependencies,
                                                      includeTransitiveDependencies) {
        Set<ResolvedArtifact> dependenciesToHandle = newHashSet()

        project.rootProject.allprojects.each {
            Project p ->
                if (p.configurations.any { it.name == DEFAULT_CONFIGURATION_TO_HANDLE }) {

                    Configuration runtimeConfiguration = p.configurations.getByName(DEFAULT_CONFIGURATION_TO_HANDLE)
                    Iterator<ResolvedArtifact> iterator = runtimeConfiguration.resolvedConfiguration.resolvedArtifacts.iterator()

                    while (iterator.hasNext()) {
                        ResolvedArtifact d = iterator.next()
                        String dependencyDesc = "$d.moduleVersion.id.group:$d.moduleVersion.id.name:$d.moduleVersion.id.version"
                        if (!ignoreDependencies().contains(dependencyDesc)) {
                            if (includeTransitiveDependencies) {
                                dependenciesToHandle.add(d)
                            } else {
                                if (projectNonTransitiveDependencies().contains(dependencyDesc)) {
                                    dependenciesToHandle.add(d)
                                }
                            }
                        }
                    }

                }
        }

        dependenciesToHandle
    }

    Set<String> provideFileDependencies() {
        Set<String> fileDependencies = newHashSet()

        project.rootProject.allprojects.each {
            Project itp ->
                itp.configurations.each {
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
        }

        fileDependencies
    }

    /**
     * Provide set with ignored dependencies.
     * Includes: project dependencies.
     *
     * @return set with dependencies to ignore
     */
    Set<String> provideIgnoredDependencies() {
        Set<String> dependenciesToIgnore = newHashSet()

        project.rootProject.allprojects.each {
            Project itp ->
                itp.configurations.each {
                    Configuration configuration ->
                        Set<Dependency> d = configuration.allDependencies.findAll {
                            it instanceof ProjectDependency
                        }
                        d.each {
                            dependenciesToIgnore.add("$it.group:$it.name:$it.version")
                        }
                }
        }

        dependenciesToIgnore
    }

    /**
     * Provide set with non-transitive dependencies.
     *
     * @return set with project dependencies
     */
    Set<String> provideNonTransitiveDependencies() {
        Set<String> dependencies = newHashSet()

        project.rootProject.allprojects.each {
            Project prj ->
                if (prj.configurations.any { it.name == DEFAULT_CONFIGURATION_TO_HANDLE }) {
                    prj.configurations.getByName(DEFAULT_CONFIGURATION_TO_HANDLE).allDependencies.each {
                       dependency -> dependencies.add("$dependency.group:$dependency.name:$dependency.version".toString())
                    }
                }
        }

        dependencies
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
    private DependencyMetadata retrieveLicensesForDependency(String dependencyDesc, String initialDependency = dependencyDesc) {
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
            pomData.addLicense(new LicenseMetadata(licenseName: it.name.text().trim(),
                    licenseTextUrl: it.url.text().trim()))
        }

        if(pomData.hasLicense()) {
            pomData
        } else if(!isNullOrEmpty(xml.parent.text())) {
            String parentGroup = xml.parent.groupId.text().trim()
            String parentName = xml.parent.artifactId.text().trim()
            String parentVersion = xml.parent.version.text().trim()

            retrieveLicensesForDependency("$parentGroup:$parentName:$parentVersion", initialDependency)
        } else {
            noLicenseMetaData(dependencyDesc)
        }
    }

}
