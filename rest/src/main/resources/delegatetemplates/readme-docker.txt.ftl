
Be sure you have docker installed.

Edit launch-harness-delegate.sh to set proxy settings or to enter a delegate description.

Install the Harness Delegate by executing launch-harness-delegate.sh.

Get container IDs:

   sudo docker container ls

See startup logs:

   sudo docker logs -f <container-id>

Run a shell in a container:

   sudo docker container exec -it <container-id> bash

The editor 'nano' is pre-installed and you can install other tools from the shell
using 'apt-get'.

