#!/usr/bin/groovy

def call(String mavenImage, String releaseVersion) {

    stage('Build Maven Release') {
        def mavenCredentials = file(credentialsId: 'maven-settings', variable: 'mavenSettings')
        def buildEnvironment = docker.image(mavenImage).pull()

        buildEnvironment.inside('-v /root/.m2:/root/.m2') {
            withCredentials([mavenCredentials]) {
                def releaseGoals = "deploy"
                def releaseArguments = "-Dmaven.javadoc.skip=true"

                def mavenGoals = ["release:prepare", "release:perform"]
                def mavenArguments = [
                    "--batch-mode",
                    "-DautoVersionSubmodules",
                    "-DpushChanges=false",
                    "-DlocalCheckout=true",
                    "-Dtag=$releaseVersion",
                    "-DreleaseVersion=$releaseVersion",
                    "-Dgoals='$releaseGoals'",
                    "-Darguments='$releaseArguments'"
                ]

                def mavenGoalsString = mavenGoals.join(" ")
                def mavenArgumentsString = mavenArguments.join(" ")

                gitUtils.withGitCredentials {
                    sh "mvn --settings=$mavenSettings $mavenArgumentsString $mavenGoalsString"
                }
            }
        }
    }
}
