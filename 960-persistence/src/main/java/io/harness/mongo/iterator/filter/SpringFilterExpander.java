package io.harness.mongo.iterator.filter;

import org.springframework.data.mongodb.core.query.Query;

public interface SpringFilterExpander extends FilterExpander {
  void filter(Query query);
}
