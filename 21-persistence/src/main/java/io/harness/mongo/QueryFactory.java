package io.harness.mongo;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.persistence.HQuery;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.DefaultQueryFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;

public class QueryFactory extends DefaultQueryFactory {
  @Override
  public <T> Query<T> createQuery(
      final Datastore datastore, final DBCollection collection, final Class<T> type, final DBObject query) {
    final QueryImpl<T> item = new HQuery<>(type, collection, datastore);

    if (query != null) {
      item.setQueryObject(query);
    }
    return item;
  }
}
