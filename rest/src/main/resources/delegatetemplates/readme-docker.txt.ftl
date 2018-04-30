
Be sure you have docker installed.

Edit launch-harness-delegate.sh to set proxy settings or to enter a delegate description.

Install the Harness Delegate by executing launch-harness-delegate.sh.

See startup logs with:

   sudo docker logs -f [container_ID]

Get a shell in the running container with

   sudo docker container exec -it [container ID] bash


