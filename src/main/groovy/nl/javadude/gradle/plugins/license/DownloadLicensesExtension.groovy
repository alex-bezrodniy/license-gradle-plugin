package nl.javadude.gradle.plugins.license

/**
 * Extension contains attributes fow {@link DownloadLicenses}.
 */
class DownloadLicensesExtension {

    /**
     * File with metadata for missing licenses.
     */
    File missingLicenses

    /**
     * Custom license mapping.
     */
    Map<String, LicenseMetadata> customLicensesMapping

    /**
     * License aliases.
     */
    Map<LicenseMetadata, LicenseMetadata> aliases

    /**
     * Generate report for each dependency.
     */
    boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    boolean reportByLicenseType

    /**
     * Include transitive dependencies to report.
     */
    boolean includeTransitiveDependencies

    /**
     * Report format.
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

    LicenseMetadata license(String name, String url = null) {
        new LicenseMetadata(name, url)
    }
}
