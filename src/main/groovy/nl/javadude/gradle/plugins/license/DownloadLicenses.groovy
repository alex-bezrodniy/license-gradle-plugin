package nl.javadude.gradle.plugins.license

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

/**
 * Task for downloading dependency licenses and generating reports.
 */
public class DownloadLicenses extends DefaultTask {

    /**
     * Custom license mapping that overrides existent if needed.
     */
    @Input Map<String, LicenseMetadata> customLicensesMapping

    /**
     * Aliases for licences that has different names spelling.
     */
    @Input Map<String, LicenseMetadata> aliases

    /**
     * Generate report for each dependency.
     */
    @Input boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    @Input boolean reportByLicenseType

    /**
     * Output directory for reports.
     */
    @OutputDirectory File outputDir

    /**
     * File name for reports by dependency.
     */
    @Input String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    @Input String reportByLicenseFileName

    /**
     * Is XML report enabled.
     */
    @Input boolean xml

    /**
     * Is HTML report enabled.
     */
    @Input boolean html

    @TaskAction
    def downloadLicenses() {
        if (!enabled || (!isReportByDependency() && !isReportByLicenseType()) || (!isXml() && !isHtml())) {
            didWork = false;
            return;
        }

        // Lazy dependency resolving
        def dependencyLicensesSet = {
            def licenseResolver = new LicenseResolver(project: project)
            licenseResolver.provideLicenseMap4Dependencies(getCustomLicensesMapping(), getAliases())
        }.memoize()

        // Lazy reporter resolving
        def reporter = { new LicenseReporter(outputDir: getOutputDir()) }.memoize()

        // Generate report that groups dependencies
        if (isReportByDependency()) {
            if(isHtml()) {
                reporter().generateHTMLReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".html")
            }
            if(isXml()) {
                reporter().generateXMLReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".xml")
            }
        }

        // Generate report that groups licenses
        if (isReportByLicenseType()) {
            if(isHtml()) {
                reporter().generateHTMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".html")
            }
            if(isXml()) {
                reporter().generateXMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".xml")
            }
        }
    }

}
