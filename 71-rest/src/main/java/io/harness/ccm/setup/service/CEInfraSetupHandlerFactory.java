package io.harness.ccm.setup.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.setup.service.impl.AwsCEInfraSetupHandler;
import io.harness.exception.InvalidRequestException;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.settings.SettingValue;

@Singleton
public class CEInfraSetupHandlerFactory {
  private final AwsCEInfraSetupHandler awsCEInfraSetupHandler;

  @Inject
  public CEInfraSetupHandlerFactory(AwsCEInfraSetupHandler awsCEInfraSetupHandler) {
    this.awsCEInfraSetupHandler = awsCEInfraSetupHandler;
  }

  public CEInfraSetupHandler getCEInfraSetupHandler(SettingValue settingValue) {
    if (settingValue instanceof CEAwsConfig) {
      return awsCEInfraSetupHandler;
    }
    throw new InvalidRequestException("CE infra handler not found");
  }
}
