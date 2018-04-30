Be sure you have kubectl installed and credentials to access your
kubernetes cluster.

Edit harness-delegate.yaml to change namespace, set proxy settings, or to
enter a delegate description.

If you're installing into more than one cluster or namespace then also edit
the name of the StatefulSet to be unique, keeping the 6 letter account
identifier as part of the name.

Install the Harness Delegate by executing:

  kubectl create -f harness-delegate.yaml

Replace if needed with:

  kubectl replace -f harness-delegate.yaml

See startup logs with:

  kubectl logs [StatefulSet name]-0 -n harness-delegate


