/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateScope.DelegateScopeKeys;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrateDelegateScopesToInfraDefinition {
  @Inject private DelegateScopeService delegateScopeService;
  @Inject private WingsPersistence wingsPersistence;

  public void migrate(Account account) {
    log.info("Running infra migration for Delegate Scopes.Retrieving applications for accountId: "
        + account.getAccountName());
    try (HIterator<DelegateScope> scopes = new HIterator<>(wingsPersistence.createQuery(DelegateScope.class)
                                                               .filter(DelegateScopeKeys.accountId, account.getUuid())
                                                               .fetch())) {
      log.info("[Delegate Scoping Migration]: Updating Delegate Scopes.");
      while (scopes.hasNext()) {
        DelegateScope scope = scopes.next();
        try {
          log.info(
              "[Delegate Scoping Migration]: Starting to migrate" + scope.getName() + " with id: " + scope.getUuid());
          migrate(scope, account);
        } catch (Exception e) {
          log.error("[Delegate Scoping Migration]: Migration Failed for scope " + scope.getName()
              + " with id: " + scope.getUuid());
        }
      }
    }
  }

  private void migrate(DelegateScope scope, Account account) {
    List<String> infraMappingIds = scope.getServiceInfrastructures();
    if (EmptyPredicate.isNotEmpty(infraMappingIds)) {
      // Using wings persistence here as no appId present
      List<InfrastructureMapping> infrastructureMappings =
          wingsPersistence.createQuery(InfrastructureMapping.class)
              .filter(InfrastructureMapping.ACCOUNT_ID_KEY2, account.getUuid())
              .field(InfrastructureMapping.ID_KEY2)
              .in(infraMappingIds)
              .asList();

      List<String> serviceIds =
          infrastructureMappings.stream().map(InfrastructureMapping::getServiceId).collect(Collectors.toList());
      List<String> infraDefinitionIds = infrastructureMappings.stream()
                                            .map(InfrastructureMapping::getInfrastructureDefinitionId)
                                            .collect(Collectors.toList());

      log.info("[Delegate Scoping Migration]: Setting " + serviceIds.size() + " on scope " + scope.getName()
          + " with id: " + scope.getUuid());
      scope.setServices(serviceIds);

      log.info("[Delegate Scoping Migration]: Setting " + infraDefinitionIds.size() + " on scope " + scope.getName()
          + " with id: " + scope.getUuid());
      scope.setInfrastructureDefinitions(infraDefinitionIds);

      log.info("[Delegate Scoping Migration]: Updating scope " + scope.getName() + " with id: " + scope.getUuid());
      delegateScopeService.update(scope);
      log.info("[Delegate Scoping Migration]: Updated scope " + scope.getName() + " with id: " + scope.getUuid());
    } else {
      log.info("[Delegate Scoping Migration]: No Service Infras Found for scope " + scope.getName()
          + " with id: " + scope.getUuid());
    }
  }
}
