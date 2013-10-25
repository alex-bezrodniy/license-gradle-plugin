package nl.javadude.gradle.plugins.license

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

/**
 * Task for downloading dependency licenses and generating reports.
 */
public class DownloadLicenses extends DefaultTask {

    /**
     * Property file with dependencies mapped to their licence.
     */
    File missingLicenses

    /**
     *
     */
    Map<String, LicenseMetadata> customLicensesMapping

    /**
     * Generate report for each dependency.
     */
    boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    boolean reportByLicenseType

    /**
     * Include transitive dependencies in report.
     */
    boolean includeTransitiveDependencies

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

        String fileName4DependencyToLicense = getReportByDependencyFileName() + "." + getFormat()
        String fileName4LicenseToDependencies = getReportByLicenseFileName() + "." + getFormat()

        // Lazy dependency resolving
        def dependencyLicensesSet = {
            def licenseResolver = new LicenseResolver(project: project)
            licenseResolver.provideLicenseMap4Dependencies(getCustomLicensesMapping(), isIncludeTransitiveDependencies())
        }.memoize()

        // Lazy reporter resolving
        def reporter = { new LicenseReporter(outputDir: getOutputDir()) }.memoize()

        // Generate report that groups dependencies
        if (isReportByDependency()) {
            reporter().generateXMLReport4DependencyToLicense(dependencyLicensesSet(), fileName4DependencyToLicense)
        }

        // Generate report that groups licenses
        if (isReportByLicenseType()) {
            reporter().generateXMLReport4LicenseToDependency(dependencyLicensesSet(), fileName4LicenseToDependencies)
        }
    }

    // Getters
    @Input
    Map<String, LicenseMetadata> getCustomLicensesMapping() {
        return customLicensesMapping
    }

    @Input
    boolean isIncludeTransitiveDependencies() {
        return includeTransitiveDependencies
    }

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
