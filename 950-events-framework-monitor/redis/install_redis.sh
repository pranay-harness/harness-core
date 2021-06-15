# Installing Redis from source as alpine doesn't have Redis 6 yet
# We need Redis 6 for connecting to SSL enabled instance
wget https://download.redis.io/releases/redis-6.2.3.tar.gz
tar xvzf redis-6.2.3.tar.gz
cd redis-6.2.3
make BUILD_TLS=yes
cp src/redis-server src/redis-cli /usr/bin/