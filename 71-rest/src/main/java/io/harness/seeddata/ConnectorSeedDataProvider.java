package io.harness.seeddata;

import static io.harness.seeddata.SeedDataProviderConstants.DOCKER_HUB_ARTIFACT_SERVER;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.intfc.SettingsService;

@Singleton
public class ConnectorSeedDataProvider {
  @Inject private SettingsService settingsService;

  public SettingAttribute createDockerConnector(String accountId) {
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(SeedDataProviderConstants.DOCKER_CONNECTOR_NAME)
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(
                DockerConfig.builder().accountId(accountId).dockerRegistryUrl(DOCKER_HUB_ARTIFACT_SERVER).build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();

    return settingsService.forceSave(dockerSettingAttribute);
  }
}
