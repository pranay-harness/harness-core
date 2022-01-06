/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskService;

import software.wings.beans.InfrastructureMapping;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.instance.InstanceService;

import com.google.inject.Inject;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED)
@OwnedBy(CDP)
public abstract class AbstractInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject InstanceService instanceService;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject AppService appService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject EnvironmentService environmentService;

  @Nullable
  protected String getTaskDescription(InfrastructureMapping infraMapping) {
    try {
      return getTaskDescription(appService.get(infraMapping.getAppId()).getName(),
          serviceResourceService.get(infraMapping.getAppId(), infraMapping.getServiceId()).getName(),
          environmentService.get(infraMapping.getAppId(), infraMapping.getEnvId()).getName(),
          infraMapping.getDisplayName());
    } catch (Exception ex) {
      return null;
    }
  }

  private String getTaskDescription(String appName, String serviceName, String envName, String infraName) {
    return String.format("Application: [%s], Service: [%s], Environment: [%s], Infrastructure: [%s]", appName,
        serviceName, envName, infraName);
  }
}
