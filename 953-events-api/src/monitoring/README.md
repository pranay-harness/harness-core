# Redis Streams Monitoring

## Installation
1. Install virtualenv
```
python3 -m venv monitoring
```
2. Activate the venv
```
source monitoring/bin/activate
```
3. Install the requirements
```
pip install -r requirements.txt
```
4. Run the python script
```
redis_connection="localhost:6379" python3 stream_stats.py
```

## Usage

1. Local usage
```
redis_connection="localhost:6379" python3 stream_stats.py
```

2. Running the docker instance
```
docker build -t redis_streams_monitoring .
docker run --env redis_connection=host.docker.internal:6379 redis_streams_monitoring
```
Note: You can replace the host.docker.internal:6379 with the actual redis host in some other pod