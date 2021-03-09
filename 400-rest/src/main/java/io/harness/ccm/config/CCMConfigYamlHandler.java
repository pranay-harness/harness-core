package io.harness.ccm.config;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

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
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<CCMConfig.Yaml> changeContext) {
    // do nothing
  }

  private CCMConfig toBean(ChangeContext<CCMConfig.Yaml> changeContext) {
    CCMConfig.Yaml yaml = changeContext.getYaml();
    if (null == yaml) {
      return null;
    }
    boolean isContinuousEfficiencyEnabled = yaml.isContinuousEfficiencyEnabled();
    return CCMConfig.builder().cloudCostEnabled(isContinuousEfficiencyEnabled).build();
  }
}
