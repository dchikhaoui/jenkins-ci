#!/usr/bin/groovy

def call(config = [:]) {
    node('docker') {
        GitUtils gitUtils = new GitUtils()
        try {
            config = config as JavaLibraryPipelineConfig

            checkout scm

            gitUtils.withGitCredentials {
                switch (env.BRANCH_NAME) {
                    case ~/master/:
                        releaseVersion = initMavenRelease()
                        buildMavenRelease(config.mavenImage, releaseVersion)
                        gitUtils.publishLibraryRelease(releaseVersion)
                        break
                    case ~/hotfix\/.+/:
                        releaseVersion = initMavenRelease()
                        buildMavenRelease(config.mavenImage, releaseVersion)
                        gitUtils.publishHotfix(releaseVersion)
                        break
                    default:
                        releaseVersion = readMavenPom().getVersion()
                        buildMavenSnapshot(config.mavenImage)
                }
            }
            currentBuild.displayName = releaseVersion
        } finally {
            cleanWs()
        }
    }
}
