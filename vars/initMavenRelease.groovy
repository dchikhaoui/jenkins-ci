#!/usr/bin/groovy

def call() {
    def releaseVersion
    stage('Init Maven Release') {
        String proposedVersion = readMavenPom().getVersion().replace("-SNAPSHOT", "")
        releaseVersion = input(id: 'releaseVersionInput', message: "Please specify release version", parameters: [
                [$class: 'StringParameterDefinition', defaultValue: "$proposedVersion", description: 'releaseVersion', name: 'releaseVersion']
        ])
    }
    return releaseVersion
}
