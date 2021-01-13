package io.harness.springdata;

import static java.time.Duration.ofSeconds;

import io.harness.exception.ExceptionUtils;
import io.harness.health.HealthMonitor;

import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.lang.Nullable;

@SuppressWarnings("NullableProblems")
@Slf4j
public class HMongoTemplate extends MongoTemplate implements HealthMonitor {
  private static final int RETRIES = 3;

  public static final FindAndModifyOptions upsertReturnNewOptions =
      new FindAndModifyOptions().upsert(true).returnNew(true);
  public static final FindAndModifyOptions upsertReturnOldOptions =
      new FindAndModifyOptions().upsert(true).returnNew(false);

  public HMongoTemplate(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
    super(mongoDbFactory, mongoConverter);
  }

  @Nullable
  @Override
  public <T> T findAndModify(Query query, Update update, Class<T> entityClass) {
    return retry(
        () -> findAndModify(query, update, new FindAndModifyOptions(), entityClass, getCollectionName(entityClass)));
  }

  @Nullable
  @Override
  public <T> T findAndModify(Query query, Update update, FindAndModifyOptions options, Class<T> entityClass) {
    return retry(() -> findAndModify(query, update, options, entityClass, getCollectionName(entityClass)));
  }

  @Override
  public Duration healthExpectedResponseTimeout() {
    return ofSeconds(5);
  }

  @Override
  public Duration healthValidFor() {
    return ofSeconds(15);
  }

  @Override
  public void isHealthy() {
    executeCommand("{ buildInfo: 1 }");
  }

  public interface Executor<R> {
    R execute();
  }

  public static <R> R retry(Executor<R> executor) {
    for (int i = 1; i < RETRIES; ++i) {
      try {
        return executor.execute();
      } catch (MongoSocketOpenException | MongoSocketReadException | OptimisticLockingFailureException e) {
        log.error("Exception ignored on retry ", e);
      } catch (RuntimeException exception) {
        if (ExceptionUtils.cause(MongoSocketOpenException.class, exception) != null) {
          continue;
        }
        if (ExceptionUtils.cause(MongoSocketReadException.class, exception) != null) {
          continue;
        }
        throw exception;
      }
    }
    // one last try
    return executor.execute();
  }
}
