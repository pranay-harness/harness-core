import jenkins.model.Jenkins

def jobs = [
        'pr-cancel-old-checks',
        'pr-portal-author-check',
        'pr-portal-checkstyle',
        'pr-portal-clang-format',
        'pr-portal-cpd',
        'pr-portal-findbugs',
        'pr-portal-functional-tests',
        'pr-portal-integration-test',
        'pr-portal-message-check',
        'pr-portal-pmd',
        'pr-portal-release-number',
        'pr-portal-unit-tests',
        'pr-portal-warnings'
]


jobs.each {
    def jobName = it

    def map = [:]
    Jenkins.instance.getItemByFullName(jobName).builds.each {
        def current = map.get(it.getDisplayName())
        if (current == null || current.getNumber() < it.getNumber()) {
            map.put(it.getDisplayName(), it)
        }
    }

    Jenkins.instance.getItemByFullName(jobName).builds.each {
        def current = map.get(it.getDisplayName())
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