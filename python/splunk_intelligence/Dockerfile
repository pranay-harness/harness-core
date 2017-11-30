FROM ubuntu:16.04
RUN apt-get update && apt-get install -y make \
virtualenv \
python2.7 \
python-dev \
build-essential \
libssl-dev
RUN mkdir /home/harness
COPY dist/splunk_pyml /home/harness
RUN cd /home/harness; make init
ENV SPLUNKML_ROOT /home/harness
