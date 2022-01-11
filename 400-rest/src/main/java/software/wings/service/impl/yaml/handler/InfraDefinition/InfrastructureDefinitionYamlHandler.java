/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.CollectionUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinitionYaml;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class InfrastructureDefinitionYamlHandler
    extends BaseYamlHandler<InfrastructureDefinitionYaml, InfrastructureDefinition> {
  @Inject private YamlHelper yamlHelper;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private CustomDeploymentTypeService customDeploymentTypeService;

  @Override
  public InfrastructureDefinitionYaml toYaml(InfrastructureDefinition bean, String appId) {
    CloudProviderInfrastructureYamlHandler cloudProviderInfrastructureYamlHandler = yamlHandlerFactory.getYamlHandler(
        YamlType.CLOUD_PROVIDER_INFRASTRUCTURE, bean.getInfrastructure().getInfrastructureType());
    String provisionerName = StringUtils.EMPTY;
    if (isNotEmpty(bean.getProvisionerId())) {
      provisionerName = infrastructureProvisionerService.get(appId, bean.getProvisionerId()).getName();
    }
    List<String> scopedToServiceNames = new ArrayList<>();
    for (String serviceId : CollectionUtils.emptyIfNull(bean.getScopedToServices())) {
      scopedToServiceNames.add(getServiceName(appId, serviceId));
    }

    String deploymentTypeTemplateUri = null;
    if (isNotEmpty(bean.getDeploymentTypeTemplateId())) {
      deploymentTypeTemplateUri =
          customDeploymentTypeService.fetchDeploymentTemplateUri(bean.getDeploymentTypeTemplateId());
    }

    return InfrastructureDefinitionYaml.builder()
        .type(YamlType.INFRA_DEFINITION.name())
        .harnessApiVersion(getHarnessApiVersion())
        .scopedServices(scopedToServiceNames)
        .infrastructure(Arrays.asList(cloudProviderInfrastructureYamlHandler.toYaml(bean.getInfrastructure(), appId)))
        .deploymentType(bean.getDeploymentType())
        .cloudProviderType(bean.getCloudProviderType())
        .provisioner(provisionerName)
        .deploymentTypeTemplateUri(deploymentTypeTemplateUri)
        .build();
  }

  @Override
  public InfrastructureDefinition upsertFromYaml(
      ChangeContext<InfrastructureDefinitionYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);

    InfrastructureDefinition previous = yamlHelper.getInfraDefinitionIdByAppIdYamlPath(appId, envId, yamlFilePath);

    InfrastructureDefinition current = InfrastructureDefinition.builder().build();
    toBean(current, changeContext, changeSetContext, appId, envId);
    return upsertInfraDefinition(current, previous);
  }

  private void toBean(InfrastructureDefinition bean, ChangeContext<InfrastructureDefinitionYaml> changeContext,
      List<ChangeContext> changeSetContext, String appId, String envId) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    InfrastructureDefinitionYaml yaml = changeContext.getYaml();
    CloudProviderInfrastructureYaml infraYaml = yaml.getInfrastructure().get(0);
    CloudProviderInfrastructureYamlHandler cloudProviderInfrastructureYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.CLOUD_PROVIDER_INFRASTRUCTURE, infraYaml.getType());
    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, infraYaml);
    InfraMappingInfrastructureProvider cloudProviderInfrastructure =
        cloudProviderInfrastructureYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
    List<String> scopedToServicesId = new ArrayList<>();
    for (String serviceName : CollectionUtils.emptyIfNull(yaml.getScopedServices())) {
      scopedToServicesId.add(getServiceId(appId, serviceName));
    }
    String infraDefinitionName = yamlHelper.getInfraDefinitionNameByAppIdYamlPath(yamlFilePath);

    // name is set for the case when infra definition is created on the git side since yaml does
    // not have a name field
    bean.setName(infraDefinitionName);
    bean.setAppId(appId);
    bean.setEnvId(envId);
    bean.setCloudProviderType(yaml.getCloudProviderType());
    bean.setDeploymentType(yaml.getDeploymentType());
    bean.setScopedToServices(scopedToServicesId);
    bean.setInfrastructure(cloudProviderInfrastructure);
    if (isNotEmpty(yaml.getDeploymentTypeTemplateUri())) {
      bean.setDeploymentTypeTemplateId(customDeploymentTypeService.fetchDeploymentTemplateIdFromUri(
          changeContext.getChange().getAccountId(), yaml.getDeploymentTypeTemplateUri()));
    }
    if (isNotEmpty(yaml.getProvisioner())) {
      InfrastructureProvisioner infrastructureProvisioner =
          infrastructureProvisionerService.getByName(appId, yaml.getProvisioner());
      notNullCheck(
          "Couldn't retrieve infrastructure provisioner from name:" + yaml.getProvisioner(), infrastructureProvisioner);
      bean.setProvisionerId(infrastructureProvisioner.getUuid());
    }
  }

  private InfrastructureDefinition upsertInfraDefinition(
      InfrastructureDefinition current, InfrastructureDefinition previous) {
    if (previous != null) {
      current.setUuid(previous.getUuid());
      return infrastructureDefinitionService.update(current);
    } else {
      return infrastructureDefinitionService.save(current, false);
    }
  }

  @Override
  public InfrastructureDefinition get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String infraDefinitionName = yamlHelper.extractEntityNameFromYamlPath(
        YamlType.INFRA_DEFINITION.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    return infrastructureDefinitionService.getInfraDefByName(appId, envId, infraDefinitionName);
  }

  @Override
  public Class getYamlClass() {
    return InfrastructureDefinitionYaml.class;
  }

  @Override
  public void delete(ChangeContext<InfrastructureDefinitionYaml> changeContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (optionalApplication.isPresent()) {
      Application application = optionalApplication.get();
      Optional<Environment> optionalEnvironment = yamlHelper.getEnvIfPresent(application.getUuid(), yamlFilePath);
      if (optionalEnvironment.isPresent()) {
        Environment environment = optionalEnvironment.get();
        InfrastructureDefinition infrastructureDefinition =
            yamlHelper.getInfraDefinitionIdByAppIdYamlPath(application.getUuid(), environment.getUuid(), yamlFilePath);
        if (infrastructureDefinition != null) {
          infrastructureDefinitionService.deleteByYamlGit(application.getUuid(), infrastructureDefinition.getUuid());
        }
      }
    }
  }

  private String getServiceName(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    notNullCheck("Service can't be found for Id:" + serviceId, service, USER);
    return service.getName();
  }

  private String getServiceId(String appId, String serviceName) {
    Service service = serviceResourceService.getServiceByName(appId, serviceName);
    notNullCheck("Invalid Service:" + serviceName, service, USER);
    return service.getUuid();
  }
}
