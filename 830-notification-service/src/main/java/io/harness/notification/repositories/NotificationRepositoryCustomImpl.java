/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.repositories;

import io.harness.notification.entities.Notification;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Notification> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria);
    query.with(pageable);
    List<Notification> notificationList = mongoTemplate.find(query, Notification.class);
    return PageableExecutionUtils.getPage(
        notificationList, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Notification.class));
  }
}
