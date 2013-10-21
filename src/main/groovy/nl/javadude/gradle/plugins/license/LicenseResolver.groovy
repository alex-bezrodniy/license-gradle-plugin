package nl.javadude.gradle.plugins.license

import com.google.common.collect.HashMultimap
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact

import static com.google.common.base.Preconditions.checkState
import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.collect.Sets.newHashSet

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

    /**
     * File with metadata for missing licenses.
     */
    private static final String LICENSE_NOT_FOUND_TEXT = "License was not found"

    /**
     * Scopes that are handled by default while collecting unresolved project dependencies.
     */
    private static final List<String> DEFAULT_CONFIGURATIONS_TO_PROCESS = ["compile", "runtime"]

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
     * @return multimap with licenses
     */
    public def provideLicenseMap4Dependencies(missingLicensesProps, boolean includeTransitiveDependencies) {
        def licenseMap = HashMultimap.create()

        Closure<HashSet<String>> projectNonTransitiveDependencies = { provideNonTransitiveDependencies() }
        Closure<HashSet<String>> ignoreDependencies = { provideIgnoredDependencies() }
        Closure<HashSet<ResolvedArtifact>> dependenciesToHandle =  {
            provideDependenciesToHandle(ignoreDependencies,
                                        projectNonTransitiveDependencies,
                                        includeTransitiveDependencies)
        }

        // Resolve each dependency
        dependenciesToHandle().each {
            def dependencyDesc = "$it.moduleVersion.id.group:$it.moduleVersion.id.name:$it.moduleVersion.id.version"
            retrieveLicensesForDependency(dependencyDesc).each {
                l ->
                    if (!l.empty) {
                        licenseMap.put(dependencyDesc, l)
                    } else {
                        if (missingLicensesProps().containsKey(dependencyDesc.toString())) {
                            licenseMap.put(dependencyDesc, missingLicensesProps().getProperty("$dependencyDesc"))
                        } else {
                            licenseMap.put(dependencyDesc, LICENSE_NOT_FOUND_TEXT)
                        }
                    }
            }
        }

        licenseMap
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
    def provideDependenciesToHandle(ignoreDependencies,
                                    projectNonTransitiveDependencies,
                                    includeTransitiveDependencies) {
        Set<ResolvedArtifact> dependenciesToHandle = newHashSet()

        project.rootProject.allprojects.each {
            Project p ->
                if (p.configurations.any { it.name == "runtime" }) {

                    Configuration runtimeConfiguration = p.configurations.getByName("runtime")
                    Iterator<ResolvedArtifact> iterator = runtimeConfiguration.resolvedConfiguration.resolvedArtifacts.iterator()

                    while (iterator.hasNext()) {
                        def d = iterator.next()
                        def dependencyDesc = "$d.moduleVersion.id.group:$d.moduleVersion.id.name:$d.moduleVersion.id.version"
                        // Types GString && String aren't compatible here
                        if (!ignoreDependencies().contains(dependencyDesc)) {
                            if (includeTransitiveDependencies) {
                                dependenciesToHandle.add(d)
                            } else {
                                // Types GString && String aren't compatible here
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

    /**
     * Provide set with ignored dependencies.
     * Includes: project dependencies.
     *
     * @return set with dependencies to ignore
     */
    def provideIgnoredDependencies() {
        HashSet<String> dependenciesToIgnore = newHashSet()

        project.rootProject.allprojects.each {
            Project itp ->
                itp.configurations.each {
                    Configuration configuration ->
                        Set<Dependency> d = configuration.dependencies.findAll {
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
    def provideNonTransitiveDependencies() {
        HashSet<String> dependencies = newHashSet()

        DEFAULT_CONFIGURATIONS_TO_PROCESS.each {
            configuration ->
                project.rootProject.allprojects.each {
                    prj ->
                        if (prj.configurations.any { it.name == configuration }) {
                            prj.configurations.getByName(configuration).dependencies.each {
                                dependency -> dependencies.add("$dependency.group:$dependency.name:$dependency.version")
                            }
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
    private List<String> retrieveLicensesForDependency(name) {
        def d = project.dependencies.create("$name@pom")
        def pomConfiguration = project.configurations.detachedConfiguration(d)

        def filesByPom = pomConfiguration.resolve().asList()

        def filesByPomCount = filesByPom.size()
        checkState(!filesByPom.empty || filesByPomCount == 1, "Error while resolving configuration, " +
                "dependency resolved with wrong number of files - $filesByPomCount")

        def pStream = filesByPom[0]
        def xml = new XmlSlurper().parse(pStream)
        def licenseResult = []

        xml.licenses.license.name.each {
            licenseResult.add(it.text().trim())
        }

        if(!licenseResult.empty) {
            licenseResult
        } else if(!isNullOrEmpty(xml.parent.text())) {
            def parentGroup = xml.parent.groupId.text()
            def parentName = xml.parent.artifactId.text()
            def parentVersion = xml.parent.version.text()

            retrieveLicensesForDependency("$parentGroup:$parentName:$parentVersion")
        } else {
            [""]
        }
    }

}
