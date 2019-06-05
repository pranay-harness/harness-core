
Be sure you have docker installed.

Edit launch-harness-delegate.sh to set proxy settings or to enter a delegate description.

Install the Harness Delegate by executing launch-harness-delegate.sh.

Get container IDs:

   docker ps

See startup logs:

   docker logs -f <container-id>

Run a shell in a container:

   docker container exec -it <container-id> bash

