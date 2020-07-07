package io.harness.mongo.iterator.provider;

import static io.harness.govern.Switch.unhandled;
import static java.lang.System.currentTimeMillis;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.FilterOperator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.util.List;

@Singleton
public class MorphiaPersistenceRequiredProvider<T extends PersistentIterable>
    implements PersistenceProvider<T, MorphiaFilterExpander<T>> {
  @Inject private HPersistence persistence;

  public Query<T> createQuery(Class<T> clazz, String fieldName, MorphiaFilterExpander<T> filterExpander) {
    Query<T> query = persistence.createQuery(clazz).order(Sort.ascending(fieldName));
    if (filterExpander != null) {
      filterExpander.filter(query);
    }
    return query;
  }

  public Query<T> createQuery(long now, Class<T> clazz, String fieldName, MorphiaFilterExpander<T> filterExpander) {
    Query<T> query = createQuery(clazz, fieldName, filterExpander);
    if (filterExpander == null) {
      query.field(fieldName).lessThan(now);
    } else {
      query.and(query.criteria(fieldName).lessThan(now));
    }
    return query;
  }

  @Override
  public void updateEntityField(T entity, List<Long> nextIterations, Class<T> clazz, String fieldName) {
    UpdateOperations<T> operations = persistence.createUpdateOperations(clazz).set(fieldName, nextIterations);
    persistence.update(entity, operations);
  }

  @Override
  public T obtainNextInstance(long base, long throttled, Class<T> clazz, String fieldName,
      SchedulingType schedulingType, Duration targetInterval, MorphiaFilterExpander<T> filterExpander) {
    long now = currentTimeMillis();
    Query<T> query = createQuery(now, clazz, fieldName, filterExpander);
    UpdateOperations<T> updateOperations = persistence.createUpdateOperations(clazz);
    switch (schedulingType) {
      case REGULAR:
        updateOperations.set(fieldName, base + targetInterval.toMillis());
        break;
      case IRREGULAR:
        updateOperations.removeFirst(fieldName);
        break;
      case IRREGULAR_SKIP_MISSED:
        updateOperations.removeAll(fieldName, new BasicDBObject(FilterOperator.LESS_THAN_OR_EQUAL.val(), throttled));
        break;
      default:
        unhandled(schedulingType);
    }
    return persistence.findAndModifySystemData(query, updateOperations, HPersistence.returnOldOptions);
  }

  @Override
  public T findInstance(Class<T> clazz, String fieldName, MorphiaFilterExpander<T> filterExpander) {
    Query<T> resultQuery = createQuery(clazz, fieldName, filterExpander).project(fieldName, true);
    return resultQuery.get();
  }
}
