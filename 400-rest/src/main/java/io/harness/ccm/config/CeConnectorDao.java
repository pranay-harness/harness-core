/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeValidate;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.ce.CEGcpConfig.CEGcpConfigKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CE)
public class CeConnectorDao {
  public static final String gcpOrganizationUuidField =
      SettingAttributeKeys.value + "." + CEGcpConfigKeys.organizationSettingId;
  @Inject private HPersistence persistence;

  public SettingAttribute getCEGcpConfig(String accountId, String gcpOrganizationUuid) {
    return persistence.createQuery(SettingAttribute.class, excludeValidate)
        .filter(SettingAttributeKeys.accountId, accountId)
        .filter(SettingAttributeKeys.category, CE_CONNECTOR)
        .filter(gcpOrganizationUuidField, gcpOrganizationUuid)
        .get();
  }
}
