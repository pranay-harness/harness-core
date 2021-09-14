/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.user.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserProjectMap;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class UserProjectMapRepositoryCustomImpl implements UserProjectMapRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<UserProjectMap> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, UserProjectMap.class);
  }

  @Override
  public Page<UserProjectMap> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<UserProjectMap> userProjectMaps = mongoTemplate.find(query, UserProjectMap.class);
    return PageableExecutionUtils.getPage(
        userProjectMaps, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), UserProjectMap.class));
  }

  @Override
  public Optional<UserProjectMap> findFirstByCriteria(Criteria criteria) {
    Query query = new Query(criteria);
    return Optional.ofNullable(mongoTemplate.findOne(query, UserProjectMap.class));
  }
}
