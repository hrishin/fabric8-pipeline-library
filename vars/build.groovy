#!/usr/bin/groovy
import io.fabric8.Events
import io.fabric8.Utils


def call(Map args) {
    stage("Build application") {
        Events.emit("build.start")
        def status = ""
        def util = new Utils()
        def namespace = args.namespace ?: util.getUsersNamespace()

        try {
            createImageStream(args.app.ImageStream, namespace, util)
            buildProject(args.app.BuildConfig, namespace, util)
            status = "pass"
        } catch (e) {
            status = "fail"
            echo "build failed"
            throw e
        } finally {
          Events.emit(["build.end", "build.${status}"], [status: status, namespace: namespace])
        }
    }
}

def createImageStream(imageStream, namespace, util) {
    def isName = imageStream.metadata.name
    def isFound = shWithOutput("oc get is/$isName -n $namespace --ignore-not-found")
    if (!isFound) {
        util.ocApplyResource(imageStream, namespace)
    } else {
        echo "image stream exist ${isName}"
    }
}

def buildProject(buildConfig, namespace, util) {
    util.ocApplyResource(buildConfig, namespace)
    openshiftBuild(buildConfig: "${buildConfig.metadata.name}", showBuildLogs: 'true')
}

def shWithOutput(String command) {
    return sh(
            script: command,
            returnStdout: true
    ).trim()
}
