/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.api.DeploymentType;
import software.wings.beans.Service;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.PcfServiceSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CDP)
public class PcfServiceSpecificationYamlHandler
    extends DeploymentSpecificationYamlHandler<Yaml, PcfServiceSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(PcfServiceSpecification bean, String appId) {
    Service service = serviceResourceService.getWithDetails(appId, bean.getServiceId());
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(DeploymentType.PCF.name())
        .serviceName(service.getName())
        .manifestYaml(bean.getManifestYaml())
        .build();
  }

  @Override
  public PcfServiceSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    PcfServiceSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    PcfServiceSpecification pcfServiceSpecification = toBean(changeContext);
    pcfServiceSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      pcfServiceSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updatePcfServiceSpecification(pcfServiceSpecification);
    } else {
      return serviceResourceService.createPcfServiceSpecification(pcfServiceSpecification);
    }
  }

  private PcfServiceSpecification toBean(ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId);

    PcfServiceSpecification pcfServiceSpecification =
        PcfServiceSpecification.builder().manifestYaml(yaml.getManiefstYaml()).serviceId(serviceId).build();
    pcfServiceSpecification.setAppId(appId);
    return pcfServiceSpecification;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public PcfServiceSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId);

    return serviceResourceService.getPcfServiceSpecification(appId, serviceId);
  }

  @Override
  public void delete(ChangeContext<PcfServiceSpecification.Yaml> changeContext) {
    PcfServiceSpecification pcfServiceSpecification =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (pcfServiceSpecification != null) {
      serviceResourceService.resetToDefaultPcfServiceSpecification(pcfServiceSpecification);
    }
  }
}
