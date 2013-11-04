package nl.javadude.gradle.plugins.license

/**
 * Extension contains attributes for {@link DownloadLicenses}.
 */
class DownloadLicensesExtension {

    /**
     * Custom license mapping.
     */
    Map<String, LicenseMetadata> customLicensesMapping

    /**
     * License aliases.
     */
    Map<String, LicenseMetadata> aliases

    /**
     * Generate report for each dependency.
     */
    boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    boolean reportByLicenseType

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

    /**
     * Generate xml report.
     */
    boolean xml

    /**
     * Generate html report.
     */
    boolean html

    /**
     * Create instance of license metadata with specified name and url (optional).
     *
     * @param name license name
     * @param url URL for license text
     * @return license meta data instance
     */
    static LicenseMetadata license(String name, String url = null) {
        new LicenseMetadata(name, url)
    }
}
