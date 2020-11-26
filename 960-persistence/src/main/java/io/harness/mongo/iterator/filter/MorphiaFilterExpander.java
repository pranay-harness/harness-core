package io.harness.mongo.iterator.filter;

import org.mongodb.morphia.query.Query;

public interface MorphiaFilterExpander<T> extends FilterExpander {
  void filter(Query<T> query);
}
