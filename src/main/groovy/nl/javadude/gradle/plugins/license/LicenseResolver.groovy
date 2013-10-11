package nl.javadude.gradle.plugins.license

import com.google.common.collect.HashMultimap

import static com.google.common.base.Preconditions.checkState
import static com.google.common.base.Strings.isNullOrEmpty

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

    /**
     * File with metadata for missing licenses.
     */
    private static final String LICENSE_NOT_FOUND_TEXT = "License wasn't found"

    /**
     * Reference to gradle project.
     */
    def project

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
    public def provideLicenseMap4Dependencies(missingLicensesProps, includeTransitiveDependencies) {
        def licenseMap = HashMultimap.create()

        def dependenciesToHandle

        // In case of transitive dependencies we resolve base configuration to get the list of artifacts.
        if(includeTransitiveDependencies) {
            def resolvedArtifacts = project.configurations.compile.resolvedConfiguration.resolvedArtifacts
            dependenciesToHandle = resolvedArtifacts*.moduleVersion.id
        } else {
            // For non-transitive dependencies we just use dependency list from the project
            dependenciesToHandle = project.configurations.compile.dependencies
        }

        // Resolve each dependency
        dependenciesToHandle.each {
            retrieveLicensesForDependency(it.group, it.name, it.version).each {
                l ->
                    //
                    def dependencyDesc = "$it.group:$it.name:$it.version"
                    if (!l.empty) {
                        licenseMap.put(dependencyDesc, l)
                    } else {
                        if(properties.containsKey(dependencyDesc)) {
                            licenseMap.put(dependencyDesc, missingLicensesProps().getProperty(dependencyDesc))
                        } else {
                            licenseMap.put(dependencyDesc, missingLicensesProps().getProperty(LICENSE_NOT_FOUND_TEXT))
                        }
                    }
            }
        }

        licenseMap
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
    private List<String> retrieveLicensesForDependency(group, name, version) {
        def d = project.dependencies.create("$group:$name:$version@pom")
        def pomConfiguration = project.configurations.detachedConfiguration(d)

        def filesByPom = pomConfiguration.resolve().asList()

        def filesByPomCount = filesByPom.size()
        checkState(!filesByPom.empty || filesByPomCount == 1, "Error while resolving configuration, " +
                "dependency resolved with wrong number of files - $filesByPomCount")

        def pStream = filesByPom.get(0)
        def xml = new XmlSlurper().parse(pStream)
        def licenseResult = []

        xml.licenses.license.name.each {
            licenseResult.add(it.text())
        }

        if(licenseResult!= null && !licenseResult.empty) {
            licenseResult
        } else if(!isNullOrEmpty(xml.parent.text())) {
            def parentGroup = xml.parent.groupId.text()
            def parentName = xml.parent.artifactId.text()
            def parentVersion = xml.parent.version.text()

            retrieveLicensesForDependency(parentGroup, parentName, parentVersion)
        } else {
            []
        }
    }

}
