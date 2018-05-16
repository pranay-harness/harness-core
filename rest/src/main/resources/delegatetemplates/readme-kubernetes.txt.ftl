
Be sure you have kubectl installed and credentials to access your
kubernetes cluster.

Edit harness-delegate.yaml to change namespace, set proxy settings, or to
enter a delegate description.

Install or replace the Harness Delegate:

  kubectl apply -f harness-delegate.yaml

Get pod names:

   kubectl get pods -n harness-delegate

See startup logs:

   kubectl logs <pod-name> -n harness-delegate -f

Run a shell in a pod:

   kubectl exec <pod-name> -n harness-delegate -it -- bash

The editor 'nano' is pre-installed and you can install other tools from the shell
using 'apt-get'.

To use this delegate to deploy in the same cluster without providing credentials
in Harness the delegate needs to have access to a service account with cluster
admin credentials. Here's an example of granting this role to the default service
account in the harness-delegate namespace:

  kubectl create clusterrolebinding harness-delegate-cluster-admin \
    --clusterrole=cluster-admin \
    --serviceaccount=harness-delegate:default

Note: If you're installing more than one Kubernetes delegate then make sure the
name is unique, keeping the 6 letter account identifier as part of the name. You
can download again with a new name from the Harness > Setup > Installations
page, or edit the YAML. If you edit the YAML be sure to change the prefix of
the Stateful Set name, the 'harness.io/name' labels and selector, and the
DELEGATE_NAME environment variable value.


