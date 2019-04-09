package software.wings.verification;

import io.harness.exception.WingsException;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration.ElkCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import java.util.List;

public class ElkCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public LogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    final ElkCVConfigurationYaml yaml = (ElkCVConfigurationYaml) super.toYaml(bean, appId);
    if (!(bean instanceof ElkCVConfiguration)) {
      throw new WingsException("Unexpected type of cluster configuration");
    }

    ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) bean;
    yaml.setQueryType(elkCVConfiguration.getQueryType().name());
    yaml.setIndex(elkCVConfiguration.getIndex());
    yaml.setHostnameField(elkCVConfiguration.getHostnameField());
    yaml.setMessageField(elkCVConfiguration.getMessageField());
    yaml.setTimestampField(elkCVConfiguration.getTimestampField());
    yaml.setTimestampFormat(elkCVConfiguration.getTimestampFormat());
    return yaml;
  }

  @Override
  public LogsCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    final ElkCVConfiguration bean = (ElkCVConfiguration) super.upsertFromYaml(changeContext, changeSetContext);

    ElkCVConfigurationYaml yaml = (ElkCVConfigurationYaml) changeContext.getYaml();
    bean.setQueryType(ElkQueryType.valueOf(yaml.getQueryType()));
    bean.setIndex(yaml.getIndex());
    bean.setHostnameField(yaml.getHostnameField());
    bean.setMessageField(yaml.getMessageField());
    bean.setTimestampField(yaml.getTimestampField());
    bean.setTimestampFormat(yaml.getTimestampFormat());
    return bean;
  }

  @Override
  public Class getYamlClass() {
    return ElkCVConfigurationYamlHandler.class;
  }
}
