/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.service;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.setup.service.impl.AwsCEInfraSetupHandler;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.ce.CEAwsConfig;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CE)
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
