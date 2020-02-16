package io.harness.lock.redis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import io.harness.exception.GeneralException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.health.HealthMonitor;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class RedisPersistentLocker implements PersistentLocker, HealthMonitor, Managed {
  private RedissonClient client;
  private static final String ERROR_MESSAGE = "Failed to acquire distributed lock for %s";

  @Inject
  RedisPersistentLocker(RedissonClient redissonClient) {
    if (redissonClient == null) {
      throw new UnauthorizedException("Redis was not setup in the environment", USER);
    }
    this.client = redissonClient;
  }

  @Override
  public AcquiredLock acquireLock(String name, Duration timeout) {
    try {
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
