package software.wings.service.intfc;

import static java.lang.String.format;
import static java.util.Arrays.asList;
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
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.api.DeploymentType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.dl.WingsPersistence;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.settings.SettingValue.SettingVariableTypes;

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
  @Inject private AppService appService;

  @Override
  public PageResponse<InfrastructureDefinition> list(PageRequest<InfrastructureDefinition> pageRequest) {
    return wingsPersistence.query(InfrastructureDefinition.class, pageRequest);
  }

  @Override
  public InfrastructureDefinition save(InfrastructureDefinition infrastructureDefinition) {
    validate(infrastructureDefinition);
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

  private void validate(@Valid InfrastructureDefinition infraDefinition) {
    InfrastructureMapping infrastructureMapping = getInfraMapping(infraDefinition);
    infrastructureMappingService.validateInfraMapping(infrastructureMapping, false);
  }

  private InfrastructureMapping getInfraMapping(InfrastructureDefinition infraDefinition) {
    InfrastructureMapping infrastructureMapping;
    if (infraDefinition.getInfrastructure() instanceof GoogleKubernetesEngine) {
      infrastructureMapping =
          GcpKubernetesInfrastructureMapping.builder()
              .appId(infraDefinition.getAppId())
              .envId(infraDefinition.getEnvId())
              .deploymentType(infraDefinition.getDeploymentType().name())
              .computeProviderName(infraDefinition.getCloudProviderType().name())
              .computeProviderSettingId(infraDefinition.getInfrastructure().getCloudProviderId())
              .clusterName(((GoogleKubernetesEngine) infraDefinition.getInfrastructure()).getClusterName())
              .name(((GoogleKubernetesEngine) infraDefinition.getInfrastructure()).getNamespace())
              .releaseName(((GoogleKubernetesEngine) infraDefinition.getInfrastructure()).getReleaseName())
              .provisionerId(infraDefinition.getProvisionerId())
              .accountId(appService.getAccountIdByAppId(infraDefinition.getAppId()))
              .type(InfrastructureMappingType.GCP_KUBERNETES.getName())
              .serviceTemplateId("dummy")
              .build();
    } else {
      throw new InvalidRequestException("Only k8s gcp infra definition is supported");
    }
    return infrastructureMapping;
  }

  @Override
  public InfrastructureDefinition get(String appId, String infraDefinitionId) {
    return wingsPersistence.getWithAppId(InfrastructureDefinition.class, appId, infraDefinitionId);
  }

  @Override
  public InfrastructureDefinition update(InfrastructureDefinition infrastructureDefinition) {
    validate(infrastructureDefinition);
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
}
