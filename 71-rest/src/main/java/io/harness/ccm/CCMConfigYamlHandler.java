package io.harness.ccm;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

@Slf4j
public class CCMConfigYamlHandler extends BaseYamlHandler<CCMConfig.Yaml, CCMConfig> {
  @Override
  public CCMConfig.Yaml toYaml(CCMConfig ccmConfig, String appId) {
    boolean isCloudCostEnabled = false;
    if (ccmConfig != null) {
      isCloudCostEnabled = ccmConfig.isCloudCostEnabled();
    }
    return CCMConfig.Yaml.builder().continuousEfficiencyEnabled(isCloudCostEnabled).build();
  }

  @Override
  public CCMConfig upsertFromYaml(ChangeContext<CCMConfig.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return CCMConfig.Yaml.class;
  }

  @Override
  public CCMConfig get(String accountId, String yamlFilePath) {
    logger.error("This method is not supported.");
    return null;
  }

  @Override
  public void delete(ChangeContext<CCMConfig.Yaml> changeContext) {
    // do nothing
  }

  private CCMConfig toBean(ChangeContext<CCMConfig.Yaml> changeContext) {
    CCMConfig.Yaml yaml = changeContext.getYaml();
    boolean isContinuousEfficiencyEnabled = yaml.isContinuousEfficiencyEnabled();
    return CCMConfig.builder().cloudCostEnabled(isContinuousEfficiencyEnabled).build();
  }
}
