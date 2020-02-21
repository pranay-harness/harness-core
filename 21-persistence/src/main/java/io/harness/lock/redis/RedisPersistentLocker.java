package io.harness.lock.redis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.SRE;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;
import io.harness.health.HealthMonitor;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.redis.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class RedisPersistentLocker implements PersistentLocker, HealthMonitor, Managed {
  private RedissonClient client;
  private String lockNamespace;
  private static final String LOCK_PREFIX = "locks";
  private static final String ERROR_MESSAGE = "Failed to acquire distributed lock for %s";

  @Inject
  RedisPersistentLocker(RedisConfig redisConfig) {
    Config config = new Config();
    if (!redisConfig.isSentinel()) {
      config.useSingleServer().setAddress(redisConfig.getRedisUrl());
    } else {
      config.useSentinelServers().setMasterName(redisConfig.getMasterName());
      for (String sentinelUrl : redisConfig.getSentinelUrls()) {
        config.useSentinelServers().addSentinelAddress(sentinelUrl);
      }
    }
    logger.info("Starting redis client");
    this.client = Redisson.create(config);
    logger.info("Started redis client");
    String envNamespace = redisConfig.getEnvNamespace();
    this.lockNamespace = EmptyPredicate.isEmpty(envNamespace) ? LOCK_PREFIX.concat(":")
                                                              : String.format("%s:%s:", envNamespace, LOCK_PREFIX);
  }

  private String getLockName(String name) {
    return lockNamespace.concat(name);
  }

  @Override
  public AcquiredLock acquireLock(String name, Duration timeout) {
    try {
      name = getLockName(name);
      RLock lock = client.getLock(name);
      boolean locked = lock.tryLock(0, timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (locked) {
        logger.info("Lock acquired on {} for timeout {}", name, timeout);
        return RedisAcquiredLock.builder().lock(lock).build();
      }
    } catch (Exception ex) {
      throw new GeneralException(format(ERROR_MESSAGE, name), ex, SRE);
    }
    throw new GeneralException(format(ERROR_MESSAGE, name), SRE);
  }

  @Override
  public AcquiredLock acquireEphemeralLock(String name, Duration timeout) {
    return acquireLock(name, timeout);
  }

  @Override
  public AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout) {
    return acquireLock(entityClass.getName() + "-" + entityId, timeout);
  }

  @Override
  public AcquiredLock tryToAcquireLock(Class entityClass, String entityId, Duration timeout) {
    try {
      return acquireLock(entityClass, entityId, timeout);
    } catch (WingsException exception) {
      return null;
    }
  }

  @Override
  public AcquiredLock tryToAcquireEphemeralLock(Class entityClass, String entityId, Duration timeout) {
    return tryToAcquireLock(entityClass, entityId, timeout);
  }

  @Override
  public AcquiredLock tryToAcquireLock(String name, Duration timeout) {
    try {
      return acquireLock(name, timeout);
    } catch (WingsException exception) {
      return null;
    }
  }

  @Override
  public AcquiredLock tryToAcquireEphemeralLock(String name, Duration timeout) {
    return tryToAcquireLock(name, timeout);
  }

  @Override
  public AcquiredLock waitToAcquireLock(
      Class entityClass, String entityId, Duration lockTimeout, Duration waitTimeout) {
    return waitToAcquireLock(entityClass.getName() + "-" + entityId, lockTimeout, waitTimeout);
  }

  @Override
  public AcquiredLock waitToAcquireLock(String name, Duration lockTimeout, Duration waitTimeout) {
    try {
      name = getLockName(name);
      RLock lock = client.getLock(name);
      boolean locked = lock.tryLock(waitTimeout.toMillis(), lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (locked) {
        logger.info("Acquired lock on {} for {} having a wait Timeout of {}", name, lockTimeout, waitTimeout);
        return RedisAcquiredLock.builder().lock(lock).build();
      }
    } catch (Exception ex) {
      throw new GeneralException(format(ERROR_MESSAGE, name), ex, SRE);
    }
    throw new GeneralException(format(ERROR_MESSAGE, name), SRE);
  }

  @Override
  public void destroy(AcquiredLock acquiredLock) {
    acquiredLock.close();
  }

  @Override
  public Duration healthExpectedResponseTimeout() {
    return ofSeconds(10);
  }

  @Override
  public Duration healthValidFor() {
    return ofSeconds(15);
  }

  @Override
  public void isHealthy() {
    try (AcquiredLock dummy = acquireEphemeralLock("HEALTH_CHECK - " + generateUuid(), ofSeconds(1))) {
      // nothing to do
    }
  }

  @Override
  public void start() throws Exception {
    // Nothing to do here
  }

  @Override
  public void stop() throws Exception {
    if (client != null) {
      client.shutdown();
    }
  }
}
