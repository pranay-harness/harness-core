/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.AwsInfrastructureMappingKeys;
import software.wings.beans.HostConnectionType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.dl.WingsPersistence;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.AwsInstanceInfrastructureKeys;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

import com.google.inject.Inject;
import java.util.Collections;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostConnectionTypeMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  private static final String DEBUG_LINE = "HOST_CONNECTION_TYPE_MIGRATION: ";

  @Override
  public void migrate() {
    log.info(DEBUG_LINE + "Starting migration for host connection type migration");
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();

        try (HIterator<Application> applications =
                 new HIterator<>(wingsPersistence.createQuery(Application.class)
                                     .filter(ApplicationKeys.accountId, account.getUuid())
                                     .fetch())) {
          while (applications.hasNext()) {
            Application application = applications.next();

            try (HIterator<InfrastructureDefinition> infraDefinitions =
                     new HIterator<>(wingsPersistence.createQuery(InfrastructureDefinition.class)
                                         .filter(InfrastructureDefinitionKeys.appId, application.getUuid())
                                         .fetch())) {
              while (infraDefinitions.hasNext()) {
                InfrastructureDefinition infrastructureDefinition = infraDefinitions.next();

                if (!(infrastructureDefinition.getInfrastructure() instanceof AwsInstanceInfrastructure)) {
                  continue;
                }
                updateInfraDefinition(infrastructureDefinition);
              }
            }

            try (HIterator<InfrastructureMapping> infraMappings =
                     new HIterator<>(wingsPersistence.createQuery(InfrastructureMapping.class)
                                         .filter(InfrastructureMappingKeys.appId, application.getUuid())
                                         .fetch())) {
              while (infraMappings.hasNext()) {
                InfrastructureMapping infrastructureMapping = infraMappings.next();

                if (!(infrastructureMapping instanceof AwsInfrastructureMapping)) {
                  continue;
                }
                updateInfraMapping((AwsInfrastructureMapping) infrastructureMapping);
              }
            }
          }
        }
      }
    }
    log.info(DEBUG_LINE + "Ended migration for Host Connection Type");
  }

  private void updateInfraMapping(@Nonnull AwsInfrastructureMapping infrastructureMapping) {
    try {
      if (isNotEmpty(infrastructureMapping.getHostConnectionType())) {
        return;
      }

      String hostConnectionType = infrastructureMapping.isUsePublicDns() ? HostConnectionType.PUBLIC_DNS.name()
                                                                         : HostConnectionType.PRIVATE_DNS.name();

      wingsPersistence.updateFields(AwsInfrastructureMapping.class, infrastructureMapping.getUuid(),
          Collections.singletonMap(AwsInfrastructureMappingKeys.hostConnectionType, hostConnectionType));
      log.info(format("%s Updated hostConnectionType to %s for infra mapping Id : %s", DEBUG_LINE, hostConnectionType,
          infrastructureMapping.getUuid()));
    } catch (Exception ex) {
      log.error(
          format("%s Failure occurred in updating infra mapping id : %s", DEBUG_LINE, infrastructureMapping.getUuid()),
          ex);
    }
  }

  private void updateInfraDefinition(@Nonnull InfrastructureDefinition infrastructureDefinition) {
    try {
      AwsInstanceInfrastructure infrastructure =
          (AwsInstanceInfrastructure) infrastructureDefinition.getInfrastructure();

      if (isNotEmpty(infrastructure.getHostConnectionType())) {
        return;
      }

      String hostConnectionType = infrastructure.isUsePublicDns() ? HostConnectionType.PUBLIC_DNS.name()
                                                                  : HostConnectionType.PRIVATE_DNS.name();

      wingsPersistence.updateFields(InfrastructureDefinition.class, infrastructureDefinition.getUuid(),
          Collections.singletonMap(
              InfrastructureDefinitionKeys.infrastructure + "." + AwsInstanceInfrastructureKeys.hostConnectionType,
              hostConnectionType));
      log.info(format("%s Updated hostConnectionType to %s for infra definition Id : %s", DEBUG_LINE,
          hostConnectionType, infrastructureDefinition.getUuid()));
    } catch (Exception ex) {
      log.error(format("%s Failure occurred in updating infra definition id : %s", DEBUG_LINE,
                    infrastructureDefinition.getUuid()),
          ex);
    }
  }
}
