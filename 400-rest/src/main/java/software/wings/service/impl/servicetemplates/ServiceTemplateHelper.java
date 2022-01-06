/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.servicetemplates;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;

import io.harness.annotations.dev.TargetModule;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.ServiceTemplateService;

import com.google.inject.Inject;

@TargetModule(_870_CG_ORCHESTRATION)
public class ServiceTemplateHelper {
  @Inject private ServiceTemplateService serviceTemplateService;

  public String fetchServiceTemplateId(InfrastructureMapping infrastructureMapping) {
    ServiceTemplate serviceTemplate = serviceTemplateService.getOrCreate(
        infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId());
    if (serviceTemplate == null) {
      return null;
    }
    return serviceTemplate.getUuid();
  }

  public ServiceTemplate fetchServiceTemplate(InfrastructureMapping infrastructureMapping) {
    return serviceTemplateService.getOrCreate(
        infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId());
  }
}
