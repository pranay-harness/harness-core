package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.TERRAFORM;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("TERRAFORM")
public class TerraformInfrastructureProvisioner extends InfrastructureProvisioner {
  @NotEmpty private String sourceRepoSettingId;
  private String path;

  @Override
  public String variableKey() {
    return "terraform";
  }

  TerraformInfrastructureProvisioner() {
    setInfrastructureProvisionerType(TERRAFORM.name());
  }

  @Builder
  private TerraformInfrastructureProvisioner(String appId, String infrastructureProvisionerType, String name,
      String sourceRepoSettingId, String path, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints) {
    setAppId(appId);
    setInfrastructureProvisionerType(infrastructureProvisionerType);
    setName(name);
    setSourceRepoSettingId(sourceRepoSettingId);
    setPath(path);
    setVariables(variables);
    setMappingBlueprints(mappingBlueprints);
  }
}
