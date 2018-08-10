#!/usr/bin/groovy
import io.fabric8.Utils

def call(body) {
    def utils = new Utils()

    if (utils.isDisabledITests()) {
        echo "WARNING: Integration tests DISABLED for these pipelines!"
        return
    }

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def envName = config.environment
    def kubeNS = "-Dfabric8.environment=${envName}"

    if (envName && !config.integrationTestCmd) {
        try {
            def ns = utils.environmentNamespace(envName)
            if (ns) {
                kubeNS = "-Dnamespace.use.existing=${ns}"
                echo "Running the integration tests in namespace : ${ns}"
            }
        } catch (e) {
            echo "ERROR: failed to find the environment namespace for ${envName} due to ${e}"
            e.printStackTrace()
        }
    }

    def testCmd = config.integrationTestCmd ?: "mvn org.apache.maven.plugins:maven-failsafe-plugin:integration-test -P openshift-it ${kubeNS} -Dit.test=${config.itestPattern} -DfailIfNoTests=${config.failIfNoTests} org.apache.maven.plugins:maven-failsafe-plugin:verify"
    sh testCmd
    junitResults(body);

}
