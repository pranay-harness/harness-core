/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.framework.repositories.custom.ResourceGroupRepositoryCustom;
import io.harness.resourcegroup.model.ResourceGroup;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface ResourceGroupRepository
    extends PagingAndSortingRepository<ResourceGroup, String>, ResourceGroupRepositoryCustom {
  Optional<ResourceGroup> findOneByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);
  List<ResourceGroup> deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
