/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.repositories.custom;

import io.harness.resourcegroup.model.ResourceGroup;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class ResourceGroupRepositoryCustomImpl implements ResourceGroupRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<ResourceGroup> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<ResourceGroup> resourceGroups = mongoTemplate.find(query, ResourceGroup.class);
    return PageableExecutionUtils.getPage(
        resourceGroups, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ResourceGroup.class));
  }
}
