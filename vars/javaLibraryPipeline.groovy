#!/usr/bin/groovy

def call(config = [:]) {

    config = config as JavaLibraryPipelineConfig

    podTemplate(
            inheritFrom: 'default',
            containers: [ containerTemplate(name: 'docker', image: config.dockerImage, ttyEnabled: true, command: 'cat') ],
            volumes: [ hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock') ]
    ) {
        podTemplate(
                containers: [ containerTemplate(name: 'maven', image: config.mavenImage, ttyEnabled: true, command: 'cat') ],
                volumes: [ hostPathVolume(hostPath: '/root/.m2', mountPath: '/root/.m2') ]
        ) {
            node('docker') {
                GitUtils gitUtils = new GitUtils()
                try {
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
    }
}
