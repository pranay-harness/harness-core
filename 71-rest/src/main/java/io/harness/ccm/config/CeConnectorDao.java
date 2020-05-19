package io.harness.ccm.config;

import static io.harness.persistence.HQuery.excludeValidate;
import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.ce.CEGcpConfig.CEGcpConfigKeys;

@Slf4j
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
