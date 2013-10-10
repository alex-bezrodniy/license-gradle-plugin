package nl.javadude.gradle.plugins.license

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimaps
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

/**
 * Task for downloading dependency licenses and generating reports.
 */
public class DownloadLicenses extends DefaultTask {

    /**
     * File with metadata for missing licenses.
     */
    File missingLicenses

    /**
     * Generate report for each dependency.
     */
    boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    boolean reportByLicenseType

    /**
     * Report format.
     * Supportable formats are : XML.
     */
    String format

    /**
     * Output directory for reports.
     */
    File outputDir

    /**
     * File name for reports by dependency.
     */
    String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    String reportByLicenseFileName

    @TaskAction
    def downloadLicenses() {
        if (!enabled) {
            didWork = false;
            return;
        }
        def dependencyToLicenseMap = [:]

        def fileName4DependencyToLicense = getReportByDependencyFileName() + "." + getFormat()
        def fileName4LicenseToDependencies = getReportByLicenseFileName() + "." + getFormat()

        // TODO use maven repo, specified in project
        def mavenRepoUrl = project.repositories.mavenCentral().url

        project.configurations.each {
            it.dependencies.each {
                def d -> dependencyToLicenseMap.put("$d.name-$d.version",
                        searchLicenseInRepo(mavenRepoUrl, d.group, d.name, d.version))
            }
        }

        if (isReportByDependency()) {
            def xml = getMarkupBuilder(fileName4DependencyToLicense)
            generateXMLReport4DependencyToLicense(xml, dependencyToLicenseMap)
        }

        // TODO create mapping for known dependencies to group the same licenses
        if (isReportByLicenseType()) {
            def xml = getMarkupBuilder(fileName4LicenseToDependencies)
            def inverseMap = Multimaps.invertFrom(Multimaps.forMap(dependencyToLicenseMap),
                    ArrayListMultimap.create())
            generateXMLReport4LicenseToDependency(xml, inverseMap.asMap())
        }
    }

    private String searchLicenseInRepo(URI repoUrl, String group, String name, String version) {
        def groupName = getGroupAndNameWithoutDots(group, name)
        def pomResource = "$groupName/$version/$name-$version" + ".pom"
        def url = new URL(repoUrl.toURL(), pomResource)
        def xml = new XmlSlurper().parseText url.text
        def pomDescriptor = xml.licenses.license.name.text()

        if (pomDescriptor.empty) {
            if (!xml.parent.text().empty) {
                def parentGroup = xml.parent.groupId.text()
                def parentName = xml.parent.artifactId.text()
                def parentVersion = xml.parent.version.text()
                searchLicenseInRepo(repoUrl, parentGroup, parentName, parentVersion)
            } else {
                "No license was found"
            }
        } else {
            pomDescriptor
        }
    }

    private def getGroupAndNameWithoutDots(String group, String name) {
        "$group/$name".replaceAll("\\.", "/")
    }

    private def getMarkupBuilder(String fileName4LicenseToDependencies) {
        def licenseReport = new File(getOutputDir(), fileName4LicenseToDependencies)
        licenseReport.createNewFile()
        def writer = new FileWriter(licenseReport)
        new MarkupBuilder(writer)
    }

    private def generateXMLReport4DependencyToLicense(xml, dependencyToLicenseMap) {
        xml.dependencies() {
            dependencyToLicenseMap.each {
                entry ->
                    dependency(name: "$entry.key") {
                        license("$entry.value")
                    }
            }
        }
    }

    private def void generateXMLReport4LicenseToDependency(xml, dependencyToLicenseMap) {
        xml.licenses() {
            dependencyToLicenseMap.each {
                entry ->
                    license(name: "$entry.key") {
                        entry.value.each {
                            d -> dependency(d)
                        }
                    }
            }
        }
    }

    // Getters
    @Input
    boolean isReportByDependency() {
        reportByDependency
    }

    @Input
    boolean isReportByLicenseType() {
        reportByLicenseType
    }

    @Input
    String getReportByDependencyFileName() {
        reportByDependencyFileName
    }

    @Input
    String getReportByLicenseFileName() {
        reportByLicenseFileName
    }

    @Input
    def getFormat() {
        format
    }

    @Optional
    @InputFile
    def getMissingLicenses() {
        missingLicenses
    }

    @OutputDirectory
    def getOutputDir() {
        outputDir
    }
}
