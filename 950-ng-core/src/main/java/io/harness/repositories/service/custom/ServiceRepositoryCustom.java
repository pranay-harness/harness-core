/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.service.custom;

import io.harness.ng.core.service.entity.ServiceEntity;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ServiceRepositoryCustom {
  Page<ServiceEntity> findAll(Criteria criteria, Pageable pageable);
  ServiceEntity upsert(Criteria criteria, ServiceEntity serviceEntity);
  ServiceEntity update(Criteria criteria, ServiceEntity serviceEntity);
  UpdateResult delete(Criteria criteria);
  Long findActiveServiceCountAtGivenTimestamp(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);

  List<ServiceEntity> findAllRunTimePermission(Criteria criteria);
}
