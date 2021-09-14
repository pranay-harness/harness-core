/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.tracing.TraceMode;
import io.harness.mongo.tracing.Tracer;
import io.harness.observer.Subject;
import io.harness.persistence.HQuery;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import lombok.Getter;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.DefaultQueryFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;

@OwnedBy(HarnessTeam.PL)
public class QueryFactory extends DefaultQueryFactory {
  private final TraceMode traceMode;
  @Getter private final Subject<Tracer> tracerSubject = new Subject<>();

  public QueryFactory() {
    this.traceMode = TraceMode.DISABLED;
  }

  public QueryFactory(TraceMode traceMode) {
    this.traceMode = traceMode;
  }

  @Override
  public <T> Query<T> createQuery(
      final Datastore datastore, final DBCollection collection, final Class<T> type, final DBObject query) {
    final QueryImpl<T> item = new HQuery<>(type, collection, datastore, traceMode, tracerSubject);

    if (query != null) {
      item.setQueryObject(query);
    }
    return item;
  }
}
