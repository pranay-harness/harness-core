/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.trigger.ManifestTriggerCondition;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import java.util.List;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class ManifestTriggerConditionHandler extends TriggerConditionYamlHandler<ManifestTriggerConditionYaml> {
  @Override
  public ManifestTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    ManifestTriggerCondition manifestTriggerCondition = (ManifestTriggerCondition) bean;
    return ManifestTriggerConditionYaml.builder()
        .serviceName(yamlHelper.getServiceNameFromServiceId(appId, manifestTriggerCondition.getServiceId()))
        .versionRegex(manifestTriggerCondition.getVersionRegex())
        .build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<ManifestTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    ManifestTriggerConditionYaml yaml = changeContext.getYaml();
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceName = yaml.getServiceName();
    String versionRegex = yaml.getVersionRegex();
    String serviceId;
    String manifestId;
    if (EmptyPredicate.isNotEmpty(serviceName)) {
      Service service = yamlHelper.getServiceByName(appId, serviceName);
      notNullCheck("Service with name " + serviceName + " might be deleted", service, USER);
      serviceId = service.getUuid();
      ApplicationManifest applicationManifest = yamlHelper.getManifestByServiceId(appId, serviceId);
      notNullCheck("No manifest exists for service name: " + serviceName, applicationManifest, USER);
      manifestId = applicationManifest.getUuid();
    } else {
      throw new InvalidRequestException("Service name cannot be null or empty.", USER);
    }

    return ManifestTriggerCondition.builder()
        .appManifestId(manifestId)
        .serviceName(serviceName)
        .serviceId(serviceId)
        .versionRegex(versionRegex)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return ManifestTriggerConditionYaml.class;
  }
}
