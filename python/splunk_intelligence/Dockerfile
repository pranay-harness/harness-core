FROM python:2.7-slim
COPY VERSION dist/splunk_pyml /home/harness/
WORKDIR /home/harness
ENV SPLUNKML_ROOT /home/harness
RUN apt-get update && apt-get install -y make \
    build-essential \
    libssl-dev && \
    pip install -r requirements.txt  && \
    make clean init-docker && \
    apt-get remove -y build-essential && \
    rm -rf /var/lib/apt/lists/* /root/.cache/

# for now, we build toli/harness-learning-engine, in the future, will build harness/learning-engine
# to build: make clean dist; docker build -t toli/harness-learning-engine .

# the equivalent of dist, ie python compilation, should be done inside Docker built; this could be a multi-stage build
# copy the python files and compile; then copy only the .pyc files into second stage to be smaller