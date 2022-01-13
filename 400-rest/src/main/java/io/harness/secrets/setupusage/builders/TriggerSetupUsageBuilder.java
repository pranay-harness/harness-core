/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.setupusage.builders;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import static software.wings.beans.trigger.WebHookTriggerCondition.WEBHOOK_SECRET;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SearchFilter;
import io.harness.secrets.setupusage.EncryptionDetail;
import io.harness.secrets.setupusage.SecretSetupUsage;
import io.harness.secrets.setupusage.SecretSetupUsageBuilder;

import software.wings.beans.trigger.Trigger;
import software.wings.service.intfc.TriggerService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@OwnedBy(CDC)
public class TriggerSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject TriggerService triggerService;

  @Override
  public Set<SecretSetupUsage> buildSecretSetupUsages(String accountId, String secretId,
      Map<String, Set<EncryptedDataParent>> parentsByParentIds, EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<Trigger> triggers = getTriggers(accountId, parentIds);
    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (Trigger trigger : triggers) {
      secretSetupUsages.add(SecretSetupUsage.builder()
                                .entityId(trigger.getUuid())
                                .type(trigger.getSettingType())
                                .fieldName(WEBHOOK_SECRET)
                                .entity(trigger)
                                .build());
    }
    return secretSetupUsages;
  }

  private List<Trigger> getTriggers(String accountId, Set<String> parentIds) {
    return triggerService
        .list(aPageRequest()
                  .addFilter(ID_KEY, SearchFilter.Operator.IN, parentIds.toArray())
                  .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.IN, accountId)
                  .build(),
            false, null)
        .getResponse();
  }

  @Override
  public Map<String, Set<String>> buildAppEnvMap(
      String accountId, String secretId, Map<String, Set<EncryptedDataParent>> parentsByParentIds) {
    return Collections.emptyMap();
  }
}
