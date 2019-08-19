#!/usr/bin/groovy

def call(config = [:]) {

    config = config as JavaLibraryPipelineConfig

    podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker
    image: $config.dockerImage
    command: ['cat']
    tty: true
    volumeMounts:
    - name: dockersock
      mountPath: /var/run/docker.sock
  - name: maven
    image: $config.mavenImage
    command: ['cat']
    tty: true
    volumeMounts:
    - name: m2
      mountPath: /root/.m2
  volumes:
  - name: dockersock
    hostPath:
      path: /var/run/docker.sock
  - name: m2
    hostPath:
      path: /root/.m2
""") {
        node(POD_LABEL) {
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
