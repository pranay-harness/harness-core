/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.ValidationResult;
import software.wings.beans.ce.CEGcpConfig;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class GcpOrganizationServiceImpl implements GcpOrganizationService {
  private GcpOrganizationDao gcpOrganizationDao;
  private SettingsService settingsService;
  private CeConnectorDao ceConnectorDao;
  private CEGcpServiceAccountService ceGcpServiceAccountService;
  private GcpBillingAccountService gcpBillingAccountService;

  @Inject
  public GcpOrganizationServiceImpl(GcpOrganizationDao gcpOrganizationDao, CeConnectorDao ceConnectorDao,
      SettingsService settingsService, CEGcpServiceAccountService ceGcpServiceAccountService,
      GcpBillingAccountService gcpBillingAccountService) {
    this.gcpOrganizationDao = gcpOrganizationDao;
    this.ceConnectorDao = ceConnectorDao;
    this.settingsService = settingsService;
    this.ceGcpServiceAccountService = ceGcpServiceAccountService;
    this.gcpBillingAccountService = gcpBillingAccountService;
  }

  @Override
  public ValidationResult validate(GcpOrganization organization) {
    list(organization.getUuid());
    return ValidationResult.builder().valid(true).build();
  }

  @Override
  public GcpOrganization upsert(GcpOrganization organization) {
    if (gcpOrganizationDao.count(organization.getAccountId()) > 1) {
      throw new InvalidRequestException("Cannot enable Cloud Cost Management for more than one GCP cloud account.");
    }
    GcpServiceAccount gcpServiceAccount = ceGcpServiceAccountService.getByAccountId(organization.getAccountId());
    checkArgument(
        null != gcpServiceAccount && organization.getServiceAccountEmail().equals(gcpServiceAccount.getEmail()),
        format("The organization %s is missing a valid service account.", organization.getOrganizationName()));
    GcpOrganization upsertedOrganization = gcpOrganizationDao.upsert(organization);
    SettingAttribute prevCeConnector =
        ceConnectorDao.getCEGcpConfig(upsertedOrganization.getAccountId(), upsertedOrganization.getUuid());
    SettingAttribute currCeConnector = toSettingAttribute(upsertedOrganization);
    if (null == prevCeConnector) {
      settingsService.save(currCeConnector);
    } else {
      currCeConnector.setUuid(prevCeConnector.getUuid());
      settingsService.update(currCeConnector);
    }
    return upsertedOrganization;
  }

  private SettingAttribute toSettingAttribute(GcpOrganization organization) {
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setName(organization.getOrganizationName());
    settingAttribute.setAccountId(organization.getAccountId());
    settingAttribute.setCategory(SettingCategory.CE_CONNECTOR);
    settingAttribute.setAppId(GLOBAL_APP_ID);
    CEGcpConfig ceGcpConfig = CEGcpConfig.builder().organizationSettingId(organization.getUuid()).build();
    settingAttribute.setValue(ceGcpConfig);
    return settingAttribute;
  }

  @Override
  public GcpOrganization get(String uuid) {
    if (uuid == null) {
      return null;
    }
    return gcpOrganizationDao.get(uuid);
  }

  @Override
  public List<GcpOrganization> list(String accountId) {
    return gcpOrganizationDao.list(accountId);
  }

  @Override
  public boolean delete(String accountId, String uuid) {
    gcpBillingAccountService.delete(accountId, uuid);
    SettingAttribute settingAttribute = ceConnectorDao.getCEGcpConfig(accountId, uuid);
    settingsService.delete(GLOBAL_APP_ID, settingAttribute.getUuid());
    return gcpOrganizationDao.delete(accountId, uuid);
  }
}
