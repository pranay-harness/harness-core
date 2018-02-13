This directory contains a collection of Dockerfiles and supporting files needed to create Docker images

| File | Project | Notes |
| -------| -------|------| 
| Dockerfile-build-base | common | Dockerfile for creating an image that can be used to run Java builds inside a container on Jenkins. |
| Dockerfile-manager-jenkins | manager | Dockerfile for using inside a Jenkins job to build a manager |

Future work would be to create a Dockerfile that does a multi-stage build to compile/test and then produce an image of a s
pecific project (ie manager, delegate, etc) from the 1st stage that generates the compiled classes.
We are avoiding that for now by taking the artificats produced by `portal` Jenkins build job.

 
