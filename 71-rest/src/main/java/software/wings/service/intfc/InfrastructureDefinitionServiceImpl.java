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

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import software.wings.api.DeploymentType;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfrastructureDefinitionServiceImpl implements InfrastructureDefinitionService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<InfrastructureDefinition> list(PageRequest<InfrastructureDefinition> pageRequest) {
    return null;
  }

  @Override
  public InfrastructureDefinition save(InfrastructureDefinition infrastructureDefinition) {
    validate(infrastructureDefinition);
    String uuid = wingsPersistence.save(infrastructureDefinition);
    infrastructureDefinition.setUuid(uuid);
    return infrastructureDefinition;
  }

  private void validate(InfrastructureDefinition infrastructureDefinition) {}

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

    wingsPersistence.save(infrastructureDefinition);
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
