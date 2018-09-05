#!/usr/bin/groovy

import io.fabric8.Events
import io.fabric8.Plugins

def call(body) {
    node {
        // TODO: move registration to a different file; perhaps
        Plugins.register()
        Events.emit("pipeline.start", "testarg")
        //spawn(image: "oc") {
          checkout scm
          body()
        //}
        Events.emit("pipeline.end", "testarg")
    }
}
