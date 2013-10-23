package nl.javadude.gradle.plugins.license

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimaps
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

        def fileName4DependencyToLicense = getReportByDependencyFileName() + "." + getFormat()
        def fileName4LicenseToDependencies = getReportByLicenseFileName() + "." + getFormat()

        // Lazy load for missing licenses
        def missingLicensesProps = { getMissingLicensesProperties() }

        // Lazy dependency resolving
        def dependencyToLicenseMap = {
            def licenseResolver = new LicenseResolver(project: project)
            licenseResolver.provideLicenseMap4Dependencies(missingLicensesProps, isIncludeTransitiveDependencies())
        }

        // Lazy reporter resolving
        def reporter = { new LicenseReporter(outputDir: getOutputDir()) }

        // Generate report that groups dependencies
        if (isReportByDependency()) {
            reporter().generateXMLReport4DependencyToLicense(dependencyToLicenseMap().asMap(), fileName4DependencyToLicense)
        }

        // Generate report that groups licenses
        if (isReportByLicenseType()) {
            def inverseMultimap = Multimaps.invertFrom(dependencyToLicenseMap(), ArrayListMultimap.create())
            reporter().generateXMLReport4LicenseToDependency(inverseMultimap.asMap(), fileName4LicenseToDependencies)
        }
    }

    /**
     * Get properties with missing licenses.
     * If no file specified we use empty properties.
     * @return missing license properties
     */
    private def getMissingLicensesProperties() {
        def licensesProp = new Properties()
        if (getMissingLicenses() != null) {
            licensesProp.load(getMissingLicenses().newInputStream())
        }

        licensesProp
    }

    // Getters
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
