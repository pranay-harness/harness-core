/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import jenkins.model.Jenkins

def jobs = [
        'portal-feature',
]

def jobId(name) {
    splits = name.split()
    if (splits.length != 2) {
        return null
    }
    return splits[0]
}

jobs.each {
    def jobName = it

    def map = [:]
    Jenkins.instance.getItemByFullName(jobName).builds.each {
        def id = jobId(it.getDisplayName())

        if (id == null) {
            if (it.getExecutor() == null) {
                println("Delete failed job #" + it.getNumber())
                it.delete()
            }
            return;
        }

        def current = map.get(id)
        if (current == null || current.getNumber() < it.getNumber()) {
            map.put(id, it)
        }
    }

    Jenkins.instance.getItemByFullName(jobName).builds.each {
        def current = map.get(jobId(it.getDisplayName()))
        if (current == null) {
            return;
        }

        if (current.getNumber() == it.getNumber()) {
            return;
        }

        def executor = it.getExecutor()
        if (executor != null) {
            println("Interrupt not needed job " + jobName +" - " + it.getDisplayName() + " #" + it.getNumber())
            executor.interrupt()
        }

        if (!current.isBuilding()) {
            println("Delete old job " + jobName +" - " + it.getDisplayName() + " #" + it.getNumber())
            it.delete()
        }
    }
}
