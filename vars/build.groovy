#!/usr/bin/groovy
import io.fabric8.Events
import io.fabric8.Utils

def call(Map args) {
    stage("Build application") {
        if(args.container) {
            shell image: args.container.image, version: args.container.image {
                build(args)
            }
        } else {
            build(args)
        }
    }
}

def build(args) {
    try {
        def namespace = args.namespace ?: new Utils().getUsersNamespace()
        createImageStream(args.app.ImageStream, namespace)
        buildProject(args.app.BuildConfig, namespace)
        Events.emit("build.success", namespace)
    } catch (e) {
        Events.emit("build.failure", e)
    }
}

def createImageStream(imageStream, namespace) {
    def isName = imageStream.metadata.name
    def isFound = shWithOutput("oc get is/$isName -n $namespace --ignore-not-found")
    if (!isFound) {
        ocApplyResource(imageStream, namespace)
    } else {
        echo "image stream exist ${isName}"
    }
}

def buildProject(buildConfig, namespace) {
    ocApplyResource(buildConfig, namespace)
    openshiftBuild(buildConfig: "${buildConfig.metadata.name}", showBuildLogs: 'true')
}

def shWithOutput(String command) {
    return sh(
            script: command,
            returnStdout: true
    ).trim()
}

def ocApplyResource(resource, namespace) {
    def resourceFile = "/tmp/${namespace}-${env.BUILD_NUMBER}-${resource.kind}.yaml"
    writeYaml file: resourceFile, data: resource
    sh "oc apply -f ${resourceFile} -n ${namespace}"
}
