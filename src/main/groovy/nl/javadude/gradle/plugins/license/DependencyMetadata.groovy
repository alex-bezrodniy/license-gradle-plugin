package nl.javadude.gradle.plugins.license

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode

/**
 *
 */
@Canonical
@EqualsAndHashCode(excludes = "licenseMetadataList")
class DependencyMetadata {

    /**
     *
     */
    public static final DependencyMetadata noLicenseMetaData(String dependencyName) {
        return new DependencyMetadata(dependency: dependencyName,
                licenseMetadataList: [new LicenseMetadata(licenseName: "No license found")]
        )
    }

    /**
     * List with license metadata.
     */
    List<LicenseMetadata> licenseMetadataList = []

    /**
     *
     */
    String dependency

    /**
     *
     * @return
     */
    boolean hasLicense() {
       !licenseMetadataList.empty
    }

    /**
     *
     * @param licenseName
     * @param url
     */
    void addLicense(String licenseName, String url = null) {
        licenseMetadataList.add(new LicenseMetadata(licenseName: licenseName, licenseTextUrl: url))
    }

    /**
     *
     * @param licenseName
     * @param url
     */
    DependencyMetadata addLicense(LicenseMetadata licenseMetadata) {
        licenseMetadataList.clear()
        licenseMetadataList.add(licenseMetadata)
        return this
    }

    DependencyMetadata clearLicenses() {
        licenseMetadataList.clear()
        return this
    }
}
