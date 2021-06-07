package io.harness.springdata;

import static java.time.Duration.ofSeconds;

import io.harness.exception.ExceptionUtils;
import io.harness.health.HealthMonitor;
import io.harness.mongo.tracing.TraceMode;
import io.harness.mongo.tracing.Tracer;
import io.harness.observer.Subject;

import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadException;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.lang.Nullable;

/**
 * This Template will be updated with out inegrations
 */

@SuppressWarnings("NullableProblems")
@Slf4j
public class HMongoTemplate extends MongoTemplate implements HealthMonitor {
  private static final int RETRIES = 3;

  public static final FindAndModifyOptions upsertReturnNewOptions =
      new FindAndModifyOptions().upsert(true).returnNew(true);
  public static final FindAndModifyOptions upsertReturnOldOptions =
      new FindAndModifyOptions().upsert(true).returnNew(false);

  private final TraceMode traceMode;

  @Getter private final Subject<Tracer> tracerSubject = new Subject<>();

  public HMongoTemplate(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
    this(mongoDbFactory, mongoConverter, TraceMode.DISABLED);
  }

  public HMongoTemplate(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter, TraceMode traceMode) {
    super(mongoDbFactory, mongoConverter);
    this.traceMode = traceMode;
  }

  @Nullable
  @Override
  public <T> T findAndModify(Query query, Update update, Class<T> entityClass) {
    return retry(() -> {
      if (traceMode == TraceMode.ENABLED) {
        traceQuery(query, entityClass);
      }
      return findAndModify(query, update, new FindAndModifyOptions(), entityClass, getCollectionName(entityClass));
    });
  }

  @Nullable
  @Override
  public <T> T findAndModify(Query query, Update update, FindAndModifyOptions options, Class<T> entityClass) {
    return retry(() -> {
      if (traceMode == TraceMode.ENABLED) {
        traceQuery(query, entityClass);
      }
      return findAndModify(query, update, options, entityClass, getCollectionName(entityClass));
    });
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

  @Override
  public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
    if (traceMode == TraceMode.ENABLED) {
      traceQuery(query.getQueryObject(), query.getSortObject(), collectionName);
    }
    return super.find(query, entityClass, collectionName);
  }

  private <T> void traceQuery(Query query, Class<T> entityClass) {
    traceQuery(query.getQueryObject(), query.getSortObject(), getCollectionName(entityClass));
  }

  public void traceQuery(Document queryDoc, Document sortDoc, String collectionName) {
    tracerSubject.fireInform(Tracer::trace, queryDoc, sortDoc, collectionName, this);
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
