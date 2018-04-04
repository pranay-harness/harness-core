package software.wings.app;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.DefaultQueryFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import software.wings.dl.HQuery;

/**
 * Created by anubhaw on 3/30/18.
 */
public class HQueryFactory extends DefaultQueryFactory {
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
