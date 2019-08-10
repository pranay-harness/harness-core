package software.wings.service.impl.yaml.handler.InfraDefinition;

import static java.lang.String.format;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.GoogleKubernetesEngine.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Singleton
public class GoogleKubernetesEngineYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, GoogleKubernetesEngine> {
  @Inject private YamlHelper yamlHelper;
  @Inject private SettingsService settingsService;

  @Override
  public Yaml toYaml(GoogleKubernetesEngine bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .clusterName(bean.getClusterName())
        .namespace(bean.getNamespace())
        .releaseName(bean.getReleaseName())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.GCP_KUBERNETES_ENGINE)
        .expressions(bean.getExpressions())
        .build();
  }

  @Override
  public GoogleKubernetesEngine upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws WingsException {
    GoogleKubernetesEngine current = GoogleKubernetesEngine.builder().build();
    toBean(current, changeContext);
    return current;
  }

  private void toBean(GoogleKubernetesEngine bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setClusterName(yaml.getClusterName());
    bean.setReleaseName(yaml.getReleaseName());
    bean.setNamespace(yaml.getNamespace());
    bean.setExpressions(yaml.getExpressions());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
