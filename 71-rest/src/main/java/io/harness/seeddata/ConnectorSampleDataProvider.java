package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR_URL;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Singleton
public class ConnectorSampleDataProvider {
  @Inject private SettingsService settingsService;

  public SettingAttribute createDockerConnector(String accountId) {
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_DOCKER_HUB_CONNECTOR)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(accountId)
            .withValue(
                DockerConfig.builder().accountId(accountId).dockerRegistryUrl(HARNESS_DOCKER_HUB_CONNECTOR_URL).build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();

    SettingAttribute existing = settingsService.fetchSettingAttributeByName(
        accountId, HARNESS_DOCKER_HUB_CONNECTOR, SettingVariableTypes.DOCKER);
    if (existing != null) {
      return existing;
    }
    return settingsService.forceSave(dockerSettingAttribute);
  }
}
