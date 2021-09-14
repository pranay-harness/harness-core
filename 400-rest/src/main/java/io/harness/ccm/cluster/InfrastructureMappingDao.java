/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class InfrastructureMappingDao {
  private HPersistence persistence;

  @Inject
  public InfrastructureMappingDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public List<InfrastructureMapping> list(String cloudProviderId) {
    return persistence.createQuery(InfrastructureMapping.class, excludeValidate)
        .filter(InfrastructureMappingKeys.computeProviderSettingId, cloudProviderId)
        .asList();
  }
}
