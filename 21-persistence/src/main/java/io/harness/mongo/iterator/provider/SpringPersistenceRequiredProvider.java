package io.harness.mongo.iterator.provider;

import static io.harness.govern.Switch.unhandled;

import static java.lang.System.currentTimeMillis;

import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType;
import io.harness.mongo.iterator.filter.SpringFilterExpander;

import com.mongodb.BasicDBObject;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.mongodb.morphia.query.FilterOperator;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class SpringPersistenceRequiredProvider<T extends PersistentIterable>
    implements PersistenceProvider<T, SpringFilterExpander> {
  private final MongoTemplate persistence;

  public SpringPersistenceRequiredProvider(MongoTemplate persistence) {
    this.persistence = persistence;
  }

  public Query createQuery(Class<T> clazz, String fieldName, SpringFilterExpander filterExpander) {
    Query query = new Query();
    query.restrict(clazz).with(Sort.by(new Order(Sort.Direction.DESC, fieldName)));
    if (filterExpander != null) {
      filterExpander.filter(query);
    }
    return query;
  }

  public Query createQuery(long now, Class<T> clazz, String fieldName, SpringFilterExpander filterExpander) {
    Criteria criteria = new Criteria();
    Query query = createQuery(clazz, fieldName, filterExpander);
    if (filterExpander == null) {
      query.addCriteria(Criteria.where(fieldName).lt(now));
    } else {
      query.addCriteria(criteria.andOperator(Criteria.where(fieldName).lt(now)));
    }
    return query;
  }

  @Override
  public void updateEntityField(T entity, List<Long> nextIterations, Class<T> clazz, String fieldName) {
    Update update = new Update();
    update.set(fieldName, nextIterations);
    persistence.updateFirst(new Query(Criteria.where("_id").is(entity.getUuid())), update, clazz);
  }

  @Override
  public T obtainNextInstance(long base, long throttled, Class<T> clazz, String fieldName,
      SchedulingType schedulingType, Duration targetInterval, SpringFilterExpander filterExpander) {
    long now = currentTimeMillis();
    Query query = createQuery(now, clazz, fieldName, filterExpander);
    Update update = new Update();
    switch (schedulingType) {
      case REGULAR:
        update.set(fieldName, base + targetInterval.toMillis());
        break;
      case IRREGULAR:
        update.pop(fieldName, Update.Position.FIRST);
        break;
      case IRREGULAR_SKIP_MISSED:
        update.pull(fieldName, new BasicDBObject(FilterOperator.LESS_THAN_OR_EQUAL.val(), throttled));
        break;
      default:
        unhandled(schedulingType);
    }
    return persistence.findAndModify(
        query, update, FindAndModifyOptions.options().upsert(false).returnNew(false), clazz);
  }

  @Override
  public T findInstance(Class<T> clazz, String fieldName, SpringFilterExpander filterExpander) {
    return persistence.findOne(createQuery(clazz, fieldName, filterExpander), clazz);
  }

  @Override
  public void recoverAfterPause(Class<T> clazz, String fieldName) {
    throw new NotImplementedException("TODO");
  }
}
