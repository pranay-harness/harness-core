package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_AMI;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;
import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureType.AZURE_SSH;
import static software.wings.beans.InfrastructureType.CODE_DEPLOY;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import software.wings.beans.InfrastructureMapping;

import java.util.Collections;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsAmiInfrastructure.class, name = AWS_AMI)
  , @JsonSubTypes.Type(value = AwsEcsInfrastructure.class, name = AWS_ECS),
      @JsonSubTypes.Type(value = AwsInstanceInfrastructure.class, name = AWS_INSTANCE),
      @JsonSubTypes.Type(value = AwsLambdaInfrastructure.class, name = AWS_LAMBDA),
      @JsonSubTypes.Type(value = AzureKubernetesService.class, name = AZURE_KUBERNETES),
      @JsonSubTypes.Type(value = AzureInstanceInfrastructure.class, name = AZURE_SSH),
      @JsonSubTypes.Type(value = CodeDeployInfrastructure.class, name = CODE_DEPLOY),
      @JsonSubTypes.Type(value = DirectKubernetesInfrastructure.class, name = DIRECT_KUBERNETES),
      @JsonSubTypes.Type(value = GoogleKubernetesEngine.class, name = GCP_KUBERNETES_ENGINE),
      @JsonSubTypes.Type(value = PcfInfraStructure.class, name = PCF_INFRASTRUCTURE),
      @JsonSubTypes.Type(value = PhysicalInfra.class, name = PHYSICAL_INFRA),
      @JsonSubTypes.Type(value = PhysicalInfraWinrm.class, name = PHYSICAL_INFRA_WINRM)
})
public interface InfraMappingInfrastructureProvider extends CloudProviderInfrastructure {
  @JsonIgnore InfrastructureMapping getInfraMapping();

  @JsonIgnore Class<? extends InfrastructureMapping> getMappingClass();

  @JsonIgnore
  default Set<String> getUserDefinedUniqueInfraFields() {
    return Collections.emptySet();
  }
}
