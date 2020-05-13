package io.harness.ccm.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.ValidationResult;
import software.wings.beans.ce.CEGcpConfig;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Slf4j
@Singleton
public class GcpOrganizationServiceImpl implements GcpOrganizationService {
  private GcpOrganizationDao gcpOrganizationDao;
  private SettingsService settingsService;
  private CEGcpServiceAccountService ceGcpServiceAccountService;

  @Inject
  public GcpOrganizationServiceImpl(GcpOrganizationDao gcpOrganizationDao, SettingsService settingsService,
      CEGcpServiceAccountService ceGcpServiceAccountService) {
    this.gcpOrganizationDao = gcpOrganizationDao;
    this.settingsService = settingsService;
    this.ceGcpServiceAccountService = ceGcpServiceAccountService;
  }

  @Override
  public ValidationResult validate(GcpOrganization organization) {
    list(organization.getUuid());
    return ValidationResult.builder().valid(true).build();
  }

  @Override
  public GcpOrganization upsert(GcpOrganization organization) {
    GcpServiceAccount gcpServiceAccount = ceGcpServiceAccountService.getByAccountId(organization.getAccountId());
    checkArgument(
        null != gcpServiceAccount && organization.getServiceAccountEmail().equals(gcpServiceAccount.getEmail()),
        format("The organization %s is missing a valid service account.", organization.getOrganizationName()));
    if (null
        == settingsService.getByName(organization.getAccountId(), GLOBAL_APP_ID, organization.getOrganizationName())) {
      settingsService.save(toSettingAttribute(organization));
    }
    return gcpOrganizationDao.upsert(organization);
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
}
