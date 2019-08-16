#!/usr/bin/groovy

def call(String mavenImage) {

    stage('Build Maven Snapshot') {
        def mavenCredentials = file(credentialsId: 'maven-settings', variable: 'mavenSettings')

        def buildEnvironment = docker.image(mavenImage).pull()

        buildEnvironment.inside('-v /root/.m2:/root/.m2') {
            withCredentials([mavenCredentials]) {
                def mavenGoals = "clean install"

                sh "mvn --settings=$mavenSettings $mavenGoals"
            }
        }
    }
}
