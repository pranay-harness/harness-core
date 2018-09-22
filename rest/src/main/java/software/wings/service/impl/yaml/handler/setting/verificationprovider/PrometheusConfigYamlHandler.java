package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.PrometheusConfig;
import software.wings.beans.PrometheusConfig.PrometheusYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * Created by rsingh on 2/12/18.
 */
public class PrometheusConfigYamlHandler extends VerificationProviderYamlHandler<PrometheusYaml, PrometheusConfig> {
  @Override
  public PrometheusYaml toYaml(SettingAttribute settingAttribute, String appId) {
    PrometheusConfig config = (PrometheusConfig) settingAttribute.getValue();

    return PrometheusYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(config.getType())
        .prometheusUrl(config.getUrl())
        .build();
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<PrometheusYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    PrometheusYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    PrometheusConfig config = PrometheusConfig.builder().accountId(accountId).url(yaml.getPrometheusUrl()).build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return PrometheusYaml.class;
  }
}
