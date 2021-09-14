/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.service.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.repositories.service.custom.ServiceRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ServiceRepository extends PagingAndSortingRepository<ServiceEntity, String>, ServiceRepositoryCustom {
  Optional<ServiceEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      boolean notDeleted);
}
