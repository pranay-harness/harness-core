package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import software.wings.beans.SettingAttribute;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class HttpHelmRepoConfigYamlHandler extends HelmRepoYamlHandler<Yaml, HttpHelmRepoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(httpHelmRepoConfig.getType())
                    .url(httpHelmRepoConfig.getChartRepoUrl())
                    .build();

    if (isNotBlank(httpHelmRepoConfig.getUsername())) {
      yaml.setUsername(httpHelmRepoConfig.getUsername());
      yaml.setPassword(getEncryptedValue(httpHelmRepoConfig, "password", false));
    }

    toYaml(yaml, settingAttribute, appId);

    return yaml;
  }

  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    HttpHelmRepoConfig httpHelmRepoConfig = HttpHelmRepoConfig.builder()
                                                .accountId(accountId)
                                                .chartRepoUrl(yaml.getUrl())
                                                .username(yaml.getUsername())
                                                .encryptedPassword(yaml.getPassword())
                                                .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, httpHelmRepoConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
