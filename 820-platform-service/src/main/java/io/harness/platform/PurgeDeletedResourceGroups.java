/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.platform;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.model.ResourceGroup;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class PurgeDeletedResourceGroups {
  private final ResourceGroupRepository resourceGroupRepository;

  public void cleanUp() {
    log.info("Purging Deleted Resource Groups");
    Page<ResourceGroup> deletedResourceGroups =
        resourceGroupRepository.findAll(Criteria.where("deleted").is(Boolean.TRUE), Pageable.unpaged());
    resourceGroupRepository.deleteAll(deletedResourceGroups);
    log.info("Purged Deleted Resource Groups");
  }
}
