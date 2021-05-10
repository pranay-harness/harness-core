FROM harness/alpine:safe-alpine3.12-sec1096-apm

WORKDIR /code
RUN apk add --no-cache python3 py3-pip
RUN apk add --no-cache gcc musl-dev linux-headers redis
COPY requirements.txt .
RUN pip3 install -r requirements.txt
COPY . .
RUN chmod +x run.sh

CMD [ "sh", "./run.sh" ]
