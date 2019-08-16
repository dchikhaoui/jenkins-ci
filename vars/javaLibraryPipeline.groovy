#!/usr/bin/groovy

def call(config = [:]) {

    podTemplate(
            label: 'slave-pod',
            inheritFrom: 'default',
            containers: [
                    containerTemplate(name: 'maven', image: config.mavenImage, ttyEnabled: true, command: 'cat'),
                    containerTemplate(name: 'docker', image: 'docker:18.02', ttyEnabled: true, command: 'cat')
            ],
            volumes: [
                    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                    hostPathVolume(hostPath: '/root/.m2', mountPath: '/root/.m2')
            ]
    ) {
        node('slave-pod') {
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
}
