package nl.javadude.gradle.plugins.license

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

import static nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license

/**
 * Task for downloading dependency licenses and generating reports.
 */
public class DownloadLicenses extends DefaultTask {

    /**
     * Custom license mapping that overrides existent if needed.
     */
    @Input Map<String, LicenseMetadata> licenses

    /**
     * Aliases for licences that has different names spelling.
     */
    @Input Map<Object, List<String>> aliases

    /**
     * Generate report for each dependency.
     */
    @Input boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    @Input boolean reportByLicenseType

    /**
     * Output directory for xml reports.
     */
    @OutputDirectory File xmlDestination

    /**
     * Output directory for html reports.
     */
    @OutputDirectory File htmlDestination

    /**
     * File name for reports by dependency.
     */
    @Input String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    @Input String reportByLicenseFileName

    /**
     * Is xml reports are enabled.
     */
    @Input boolean xml

    /**
     * Is html reports are enabled.
     */
    @Input boolean html

    @TaskAction
    def downloadLicenses() {
        if (!enabled || (!isReportByDependency() && !isReportByLicenseType())
           || (!isXml() && !isHtml())) {
            didWork = false;
            return;
        }

        // Lazy dependency resolving
        def dependencyLicensesSet = {
            def licenseResolver = new LicenseResolver(project: project)
            licenseResolver.provideLicenseMap4Dependencies(getLicenses(), aliases.collectEntries {
                if(it.key instanceof String) {
                    new MapEntry(license(it.key), it.value)
                } else {
                    it
                }
            })
        }.memoize()

        // Lazy reporter resolving
        def reporter = { new LicenseReporter(xmlOutputDir: getXmlDestination(), htmlOutputDir: getHtmlDestination()) }.memoize()

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
            if( isXml()) {
                reporter().generateXMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".xml")
            }
        }
    }

}
