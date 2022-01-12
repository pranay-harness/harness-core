/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class InfrastructureDefinitionDao {
  static final String cloudProviderField = InfrastructureDefinitionKeys.infrastructure + "."
      + "cloudProviderId";

  private HPersistence persistence;

  @Inject
  public InfrastructureDefinitionDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public List<InfrastructureDefinition> list(String cloudProviderId) {
    return persistence.createQuery(InfrastructureDefinition.class, excludeValidate)
        .filter(cloudProviderField, cloudProviderId)
        .asList();
  }
}
