package nl.javadude.gradle.plugins.license

import groovy.transform.Canonical

/**
 *
 */
@Canonical
class LicenseMetadata implements Serializable {

    /**
     *
     */
    String licenseName

    /**
     *
     */
    String licenseTextUrl
}
