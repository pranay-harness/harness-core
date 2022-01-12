/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
