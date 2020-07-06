# Filename: Dockerfile
FROM alpine:3.11.6
CMD ["/bin/sh"]

# replace with  secure repositories
RUN sed -i '1 c https://alpine.global.ssl.fastly.net/alpine/v3.11/main' /etc/apk/repositories
RUN sed -i '2 c https://alpine.global.ssl.fastly.net/alpine/v3.11/community' /etc/apk/repositories

# install Utilities
RUN apk add --no-cache dpkg bash util-linux make strace procps curl gnupg nmap net-tools openntpd

# install Openjdk8
RUN apk add --no-cache openjdk8

# install python3
RUN apk add --no-cache python3

#install pip and Upgrade
RUN apk add --no-cache py-pip
RUN pip3 install -U --no-cache-dir pip

#install more packages like awscli & Kubectl
RUN pip3 install --no-cache pyarmor 
RUN pip3 install --no-cache awscli
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl
RUN chmod +x kubectl
RUN mv kubectl /usr/local/bin/kubectl
RUN kubectl version --client

#install few more Utilities
RUN apk add --no-cache protobuf build-base shadow
