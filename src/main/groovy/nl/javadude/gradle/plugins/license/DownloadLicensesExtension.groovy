package nl.javadude.gradle.plugins.license

import org.gradle.util.ConfigureUtil

/**
 * Extension contains attributes for {@link DownloadLicenses}.
 */
class DownloadLicensesExtension {

    /**
     * Custom license mapping.
     */
    Map<String, LicenseMetadata> licenses

    /**
     * License aliases.
     */
    Map<Object, List<String>> aliases

    /**
     * Generate report for each dependency.
     */
    boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    boolean reportByLicenseType

    /**
     * File name for reports by dependency.
     */
    String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    String reportByLicenseFileName

    /**
     * Generate xml report.
     */
    boolean xml

    /**
     * Generate html report.
     */
    boolean html

    /**
     * Report extension.
     */
    DownloadLicensesReportExtenstion report = new DownloadLicensesReportExtenstion()

    /**
     * Create instance of license metadata with specified name and url (optional).
     *
     * @param name license name
     * @param url URL for license text
     * @return license meta data instance
     */
    static LicenseMetadata license(name, url = null) {
        new LicenseMetadata(name, url)
    }

    /**
     * Configure report container.
     * @param closure configuring closure
     */
    def report(Closure closure) {
        ConfigureUtil.configure(closure, report)
    }
}
