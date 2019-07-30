# This harness delegate values.yaml file is compatible with version 1.0.0
# of the harness-delegate helm chart.

# You can download the harness-delegate helm chart at


# Account Id to which the delegate will be connecting
accountId: ${accountId}

# Secret identifier associated with the account
accountSecret: ${accountSecret}

# Short 6 character identifier of the account
accountIdShort: ${kubernetesAccountLabel}

delegateName: ${delegateName}

# Id of the delegate profile that needs to run when the delegate is
# coming up
delegateProfile: "${delegateProfile}"

managerHostAndPort: ${managerHostAndPort}
watcherStorageUrl: ${watcherStorageUrl}
watcherCheckLocation: ${watcherCheckLocation}
delegateStorageUrl: ${delegateStorageUrl}
delegateCheckLocation: ${delegateCheckLocation}
