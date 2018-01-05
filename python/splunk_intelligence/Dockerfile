FROM python:2.7-slim
COPY VERSION dist/splunk_pyml /home/harness/
WORKDIR /home/harness
ENV SPLUNKML_ROOT /home/harness
RUN apt-get update && apt-get install -y make \
    build-essential \
    libssl-dev && \
    pip install virtualenv  && \
    rm .pyenv && \
    make init-docker && \
    apt-get remove -y build-essential && \
    rm -rf /var/lib/apt/lists/* /root/.cache/
