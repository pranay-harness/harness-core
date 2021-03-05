package io.harness.ccm.config;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CCMConfigYamlHandler extends BaseYamlHandler<CCMConfigYaml, CCMConfig> {
  @Override
  public CCMConfigYaml toYaml(CCMConfig ccmConfig, String appId) {
    boolean isCloudCostEnabled = false;
    if (ccmConfig != null) {
      isCloudCostEnabled = ccmConfig.isCloudCostEnabled();
    }
    return CCMConfigYaml.builder().continuousEfficiencyEnabled(isCloudCostEnabled).build();
  }

  @Override
  public CCMConfig upsertFromYaml(ChangeContext<CCMConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return CCMConfigYaml.class;
  }

  @Override
  public CCMConfig get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<CCMConfigYaml> changeContext) {
    // do nothing
  }

  private CCMConfig toBean(ChangeContext<CCMConfigYaml> changeContext) {
    CCMConfigYaml yaml = changeContext.getYaml();
    if (null == yaml) {
      return null;
    }
    boolean isContinuousEfficiencyEnabled = yaml.isContinuousEfficiencyEnabled();
    return CCMConfig.builder().cloudCostEnabled(isContinuousEfficiencyEnabled).build();
  }
}
