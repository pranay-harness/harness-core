/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.impl;

import static io.harness.persistence.HQuery.excludeCount;

import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.ccm.commons.entities.events.PublishedMessage;
import io.harness.ccm.commons.entities.events.PublishedMessage.PublishedMessageKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@Singleton
public class PublishedMessageDaoImpl implements PublishedMessageDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public List<PublishedMessage> fetchPublishedMessage(
      String accountId, String messageType, Long startTime, Long endTime, int batchSize) {
    Query<PublishedMessage> query = hPersistence.createQuery(PublishedMessage.class, excludeCount)
                                        .filter(PublishedMessageKeys.accountId, accountId)
                                        .filter(PublishedMessageKeys.type, messageType)
                                        .order(PublishedMessageKeys.createdAt);

    query.and(query.criteria(PublishedMessageKeys.createdAt).greaterThanOrEq(startTime),
        query.criteria(PublishedMessageKeys.createdAt).lessThan(endTime));
    return query.asList(new FindOptions().limit(batchSize));
  }
}
