package software.wings.service.intfc;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.WINRM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.ContainerInfrastructureMapping.ContainerInfrastructureMappingKeys;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping.GcpKubernetesInfrastructureMappingKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.infra.CloudProviderInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class InfrastructureDefinitionServiceImpl implements InfrastructureDefinitionService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public PageResponse<InfrastructureDefinition> list(
      PageRequest<InfrastructureDefinition> pageRequest, String serviceId, String appId) {
    if (EmptyPredicate.isNotEmpty(serviceId)) {
      Service service = serviceResourceService.get(appId, serviceId);
      if (service == null) {
        throw new InvalidRequestException(format("No service exists for id : [%s]", serviceId));
      }
      if (service.getDeploymentType() != null) {
        pageRequest.addFilter(
            InfrastructureDefinitionKeys.deploymentType, Operator.EQ, service.getDeploymentType().name());
      }
      pageRequest.addFilter(InfrastructureDefinitionKeys.scopedToServices, Operator.CONTAINS, serviceId);
    }
    return wingsPersistence.query(InfrastructureDefinition.class, pageRequest);
  }

  @Override
  public InfrastructureDefinition save(InfrastructureDefinition infrastructureDefinition) {
    validateInfraDefinition(infrastructureDefinition);
    String uuid;
    try {
      uuid = wingsPersistence.save(infrastructureDefinition);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(
          format("Infra definition already exists with the name : [%s]", infrastructureDefinition.getName()),
          WingsException.USER);
    }
    infrastructureDefinition.setUuid(uuid);
    return infrastructureDefinition;
  }

  private void validateInfraDefinition(@Valid InfrastructureDefinition infraDefinition) {
    if (!(infraDefinition.getInfrastructure() instanceof GoogleKubernetesEngine)) {
      // TODO: add other infra mappings as well
      return;
    }
    InfrastructureMapping infrastructureMapping = getInfraMappingObject(infraDefinition);
    infrastructureMappingService.validateInfraMapping(infrastructureMapping, false);
  }

  private InfrastructureMapping getInfraMappingObject(InfrastructureDefinition infraDefinition) {
    InfrastructureMapping infrastructureMapping;
    if (infraDefinition.getInfrastructure() instanceof GoogleKubernetesEngine) {
      infrastructureMapping =
          GcpKubernetesInfrastructureMapping.builder()
              .appId(infraDefinition.getAppId())
              .envId(infraDefinition.getEnvId())
              .deploymentType(infraDefinition.getDeploymentType().name())
              .computeProviderType(CloudProviderType.GCP.name())
              .computeProviderName(infraDefinition.getCloudProviderType().name())
              .computeProviderSettingId(infraDefinition.getInfrastructure().getCloudProviderId())
              .clusterName(((GoogleKubernetesEngine) infraDefinition.getInfrastructure()).getClusterName())
              .namespace(((GoogleKubernetesEngine) infraDefinition.getInfrastructure()).getNamespace())
              .releaseName(((GoogleKubernetesEngine) infraDefinition.getInfrastructure()).getReleaseName())
              .provisionerId(infraDefinition.getProvisionerId())
              .accountId(appService.getAccountIdByAppId(infraDefinition.getAppId()))
              .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
              .serviceTemplateId("dummy")
              .build();
    } else {
      return null;
    }
    return infrastructureMapping;
  }

  @Override
  public InfrastructureDefinition get(String appId, String infraDefinitionId) {
    return wingsPersistence.getWithAppId(InfrastructureDefinition.class, appId, infraDefinitionId);
  }

  @Override
  public InfrastructureDefinition update(InfrastructureDefinition infrastructureDefinition) {
    validateInfraDefinition(infrastructureDefinition);
    InfrastructureDefinition savedInfraDefinition =
        get(infrastructureDefinition.getAppId(), infrastructureDefinition.getUuid());
    if (savedInfraDefinition == null) {
      throw new InvalidRequestException(
          format("Infra Definition does not exist with id: [%s]", infrastructureDefinition.getUuid()));
    }

    try {
      wingsPersistence.save(infrastructureDefinition);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(
          format("Infra definition already exists with the name : [%s]", infrastructureDefinition.getName()),
          WingsException.USER);
    }
    return infrastructureDefinition;
  }

  @Override
  public void delete(String appId, String infraDefinitionId) {
    wingsPersistence.delete(InfrastructureDefinition.class, appId, infraDefinitionId);
  }

  @Override
  public Map<DeploymentType, List<SettingVariableTypes>> getDeploymentTypeCloudProviderOptions() {
    Map<DeploymentType, List<SettingVariableTypes>> deploymentCloudProviderOptions = new HashMap<>();

    deploymentCloudProviderOptions.put(DeploymentType.SSH,
        asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
    deploymentCloudProviderOptions.put(KUBERNETES,
        asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
    deploymentCloudProviderOptions.put(
        HELM, asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
    deploymentCloudProviderOptions.put(ECS, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(AWS_CODEDEPLOY, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(AWS_LAMBDA, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(AMI, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(
        WINRM, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
    deploymentCloudProviderOptions.put(PCF, asList(SettingVariableTypes.PCF));

    return deploymentCloudProviderOptions;
  }

  @Override
  public InfrastructureMapping getInfraMapping(String appId, String serviceId, String infraDefinitionId) {
    validateInputs(appId, serviceId, infraDefinitionId);
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      throw new InvalidRequestException(format(
          "No infra definition exists with given appId: [%s] infra definition id : [%s]", appId, infraDefinitionId));
    }
    validateInfraDefinition(infrastructureDefinition);

    if (infrastructureDefinition.getInfrastructure() instanceof GoogleKubernetesEngine) {
      return getGcpKubernetesInfraMapping(infrastructureDefinition, serviceId);
    } else {
      throw new InvalidRequestException(format(
          "Only GCP K8S Definition supported. Found [%s]", infrastructureDefinition.getInfrastructure().getClass()));
    }
  }

  @Override
  public boolean isDynamicInfrastructure(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      return false;
    }
    return isNotEmpty(infrastructureDefinition.getProvisionerId());
  }

  @Override
  public List<String> fetchCloudProviderIds(String appId, List<String> infraDefinitionIds) {
    if (isNotEmpty(infraDefinitionIds)) {
      List<InfrastructureDefinition> infrastructureDefinitions =
          wingsPersistence.createQuery(InfrastructureDefinition.class)
              .project(InfrastructureDefinitionKeys.appId, true)
              .project(InfrastructureDefinitionKeys.infrastructure, true)
              .filter(InfrastructureDefinitionKeys.appId, appId)
              .field(InfrastructureDefinitionKeys.uuid)
              .in(infraDefinitionIds)
              .asList();
      return infrastructureDefinitions.stream()
          .map(InfrastructureDefinition::getInfrastructure)
          .map(CloudProviderInfrastructure::getCloudProviderId)
          .distinct()
          .collect(toList());
    }
    return new ArrayList<>();
  }

  private GcpKubernetesInfrastructureMapping getGcpKubernetesInfraMapping(
      InfrastructureDefinition infraDefinition, String serviceId) {
    GoogleKubernetesEngine infrastructure = (GoogleKubernetesEngine) infraDefinition.getInfrastructure();
    List<GcpKubernetesInfrastructureMapping> infrastructureMappings =
        wingsPersistence.createQuery(GcpKubernetesInfrastructureMapping.class)
            .filter(InfrastructureMapping.APP_ID_KEY, infraDefinition.getAppId())
            .filter(InfrastructureMapping.ENV_ID_KEY, infraDefinition.getEnvId())
            .filter(InfrastructureMappingKeys.serviceId, serviceId)
            .filter(InfrastructureMappingKeys.computeProviderSettingId, infrastructure.getCloudProviderId())
            .filter(InfrastructureMappingKeys.infrastructureDefinitionId, infraDefinition.getUuid())
            .filter(InfrastructureMappingKeys.infraMappingType, InfrastructureMappingType.GCP_KUBERNETES.name())
            .filter(ContainerInfrastructureMappingKeys.clusterName, infrastructure.getClusterName())
            .filter(GcpKubernetesInfrastructureMappingKeys.namespace, infrastructure.getNamespace())
            .filter(GcpKubernetesInfrastructureMappingKeys.releaseName, infrastructure.getReleaseName())
            .asList();

    if (isEmpty(infrastructureMappings)) {
      GcpKubernetesInfrastructureMapping infraMapping =
          (GcpKubernetesInfrastructureMapping) getInfraMappingObject(infraDefinition);
      infraMapping.setServiceId(serviceId);
      infraMapping.setInfrastructureDefinitionId(infraDefinition.getUuid());
      ServiceTemplate serviceTemplate =
          serviceTemplateService.get(infraDefinition.getAppId(), serviceId, infraDefinition.getEnvId());
      infraMapping.setServiceTemplateId(serviceTemplate.getUuid());
      return (GcpKubernetesInfrastructureMapping) infrastructureMappingService.save(infraMapping);
    } else {
      if (infrastructureMappings.size() > 1) {
        throw new WingsException(format("More than 1 mappings found for infra definition : [%s]. Mappings : [%s",
            infraDefinition.toString(), infrastructureMappings.toString()));
      }
      return infrastructureMappings.get(0);
    }
  }

  private void validateInputs(String appId, String serviceId, String infraDefinitionId) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("App Id can't be empty");
    }
    if (isEmpty(serviceId)) {
      throw new InvalidRequestException("Service Id can't be empty");
    }
    if (isEmpty(infraDefinitionId)) {
      throw new InvalidRequestException("Infra Definition Id can't be empty");
    }
  }
}
