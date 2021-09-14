/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.repositories.custom;

import io.harness.resourcegroup.model.ResourceGroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ResourceGroupRepositoryCustom {
  Page<ResourceGroup> findAll(Criteria criteria, Pageable pageable);
}
