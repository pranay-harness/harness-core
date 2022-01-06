/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.join;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisioner.InfrastructureProvisionerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetAccountIdProvisioners implements Migration {
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  private static String DEBUG_LINE = "SET_ACCOUNTID_PROVISIONERS";

  @Override
  public void migrate() {
    Map<String, Object> updateAccountId = new HashMap<>();
    int corrections = 0;
    int errors = 0;
    try (HIterator<InfrastructureProvisioner> provisioners =
             new HIterator<>(wingsPersistence.createQuery(InfrastructureProvisioner.class)
                                 .field(InfrastructureProvisionerKeys.accountId)
                                 .doesNotExist()
                                 .field(InfrastructureProvisionerKeys.appId)
                                 .exists()
                                 .fetch())) {
      for (InfrastructureProvisioner provisioner : provisioners) {
        String appId = provisioner.getAppId();
        if (isNotEmpty(appId)) {
          try {
            String accountId = appService.getAccountIdByAppId(appId);
            if (isNotEmpty(accountId)) {
              updateAccountId.put(InfrastructureProvisionerKeys.accountId, accountId);
              wingsPersistence.updateFields(InfrastructureProvisioner.class, provisioner.getUuid(), updateAccountId);
              log.info(join(SPACE, DEBUG_LINE,
                  format("Set accountId [%s] for provisioner [%s]", accountId, provisioner.getUuid())));
              corrections++;
            } else {
              errors++;
              log.error(join(SPACE, DEBUG_LINE, "empty accountId for appId-", appId));
            }
          } catch (Exception ex) {
            errors++;
            log.error(join(SPACE, DEBUG_LINE, "could not fetch accountId for appId -", appId));
          }
        } else {
          errors++;
          log.error(join(SPACE, DEBUG_LINE, "This should not be happening, We need appId here"));
        }
      }
    }
    log.info(HarnessStringUtils.join(SPACE, DEBUG_LINE, "Set AccountId for provisioners done", SPACE, "Corrections -",
        String.valueOf(corrections), "Errors -", String.valueOf(errors)));
  }
}
