package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR_URL;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.intfc.SettingsService;

@Singleton
public class ConnectorSampleDataProvider {
  @Inject private SettingsService settingsService;

  public SettingAttribute createDockerConnector(String accountId) {
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR)
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(
                DockerConfig.builder().accountId(accountId).dockerRegistryUrl(HARNESS_DOCKER_HUB_CONNECTOR_URL).build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();

    return settingsService.forceSave(dockerSettingAttribute);
  }
}
