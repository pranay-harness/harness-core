/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.GCP_PLAYGROUND;
import static io.harness.govern.Switch.unhandled;

import static software.wings.sm.StateType.STACK_DRIVER_LOG;

import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.ServiceGenerator.Services;

import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.StackdriverCVConfiguration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pranjal on 06/07/2019
 */
@Singleton
public class ServiceGuardGenerator {
  @Inject private OwnerManager ownerManager;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private CVConfigurationService cvConfigurationService;

  public CVConfiguration ensurePredefined(Randomizer.Seed seed, Owners owners, StateType stateType) {
    switch (stateType) {
      case STACK_DRIVER_LOG:
        return ensureStackDriverConfiguration(seed, owners);
      default:
        unhandled(stateType);
    }
    return null;
  }

  private CVConfiguration ensureStackDriverConfiguration(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
      owners.add(service);
    }

    final SettingAttribute gcpCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_PLAYGROUND);

    long currentTime = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());

    StackdriverCVConfiguration stackdriverCVConfiguration = new StackdriverCVConfiguration();
    stackdriverCVConfiguration.setAccountId(service.getAccountId());
    stackdriverCVConfiguration.setAppId(service.getAppId());
    stackdriverCVConfiguration.setEnvId(environment.getUuid());
    stackdriverCVConfiguration.setServiceId(service.getUuid());
    stackdriverCVConfiguration.setConnectorId(gcpCloudProvider.getUuid());

    stackdriverCVConfiguration.setLogsConfiguration(true);
    stackdriverCVConfiguration.setQuery("textPayload:exception AND resource.labels.project_id=exploration-161417");

    stackdriverCVConfiguration.setEnabled24x7(false);
    stackdriverCVConfiguration.setBaselineStartMinute(currentTime - 30);
    stackdriverCVConfiguration.setBaselineEndMinute(currentTime);
    stackdriverCVConfiguration.setName("stackdriver");
    stackdriverCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    stackdriverCVConfiguration.setAlertEnabled(false);
    stackdriverCVConfiguration.setAlertThreshold(0.1);
    stackdriverCVConfiguration.setStateType(STACK_DRIVER_LOG);

    cvConfigurationService.saveConfiguration(
        service.getAccountId(), service.getAppId(), StateType.STACK_DRIVER_LOG, stackdriverCVConfiguration);
    return stackdriverCVConfiguration;
  }
}
