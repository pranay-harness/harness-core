package software.wings.service.impl.yaml.handler.InfraDefinition;

import static software.wings.beans.InfrastructureType.AWS_AMI;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;
import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureType.AZURE_SSH;
import static software.wings.beans.InfrastructureType.AZURE_VMSS;
import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;
import static software.wings.beans.InfrastructureType.CODE_DEPLOY;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;

import software.wings.beans.InfrastructureType;
import software.wings.infra.AwsAmiInfrastructureYaml;
import software.wings.infra.AwsEcsInfrastructureYaml;
import software.wings.infra.AwsInstanceInfrastructureYaml;
import software.wings.infra.AwsLambdaInfrastructureYaml;
import software.wings.infra.AzureInstanceInfrastructureYaml;
import software.wings.infra.AzureKubernetesServiceYaml;
import software.wings.infra.AzureVMSSInfraYaml;
import software.wings.infra.AzureWebAppInfraYaml;
import software.wings.infra.CodeDeployInfrastructureYaml;
import software.wings.infra.CustomInfrastructureYaml;
import software.wings.infra.DirectKubernetesInfrastructureYaml;
import software.wings.infra.GoogleKubernetesEngineYaml;
import software.wings.infra.PcfInfraStructureYaml;
import software.wings.infra.PhysicalInfraWinrmYaml;
import software.wings.infra.PhysicalInfraYaml;
import software.wings.yaml.BaseYamlWithType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsAmiInfrastructureYaml.class, name = AWS_AMI)
  , @JsonSubTypes.Type(value = AwsEcsInfrastructureYaml.class, name = AWS_ECS),
      @JsonSubTypes.Type(value = AwsInstanceInfrastructureYaml.class, name = AWS_INSTANCE),
      @JsonSubTypes.Type(value = AwsLambdaInfrastructureYaml.class, name = AWS_LAMBDA),
      @JsonSubTypes.Type(value = AzureKubernetesServiceYaml.class, name = AZURE_KUBERNETES),
      @JsonSubTypes.Type(value = AzureInstanceInfrastructureYaml.class, name = AZURE_SSH),
      @JsonSubTypes.Type(value = CodeDeployInfrastructureYaml.class, name = CODE_DEPLOY),
      @JsonSubTypes.Type(value = DirectKubernetesInfrastructureYaml.class, name = DIRECT_KUBERNETES),
      @JsonSubTypes.Type(value = GoogleKubernetesEngineYaml.class, name = GCP_KUBERNETES_ENGINE),
      @JsonSubTypes.Type(value = PcfInfraStructureYaml.class, name = PCF_INFRASTRUCTURE),
      @JsonSubTypes.Type(value = PhysicalInfraYaml.class, name = PHYSICAL_INFRA),
      @JsonSubTypes.Type(value = PhysicalInfraWinrmYaml.class, name = PHYSICAL_INFRA_WINRM),
      @JsonSubTypes.Type(value = AzureVMSSInfraYaml.class, name = AZURE_VMSS),
      @JsonSubTypes.Type(value = AzureWebAppInfraYaml.class, name = AZURE_WEBAPP),
      @JsonSubTypes.Type(value = CustomInfrastructureYaml.class, name = InfrastructureType.CUSTOM_INFRASTRUCTURE)
})
public abstract class CloudProviderInfrastructureYaml extends BaseYamlWithType {
  public CloudProviderInfrastructureYaml(String type) {
    super(type);
  }
}
