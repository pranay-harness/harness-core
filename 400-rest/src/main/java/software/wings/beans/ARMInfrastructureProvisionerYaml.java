package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.ARM;

import io.harness.azure.model.ARMScopeType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class ARMInfrastructureProvisionerYaml extends InfraProvisionerYaml {
  private ARMSourceType sourceType;
  private String templateBody;
  private GitFileConfig gitFileConfig;
  private ARMScopeType scopeType;
  private boolean isBlueprint;

  @Builder
  public ARMInfrastructureProvisionerYaml(String type, String harnessApiVersion, String description,
      List<NameValuePairYaml> variables, List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints,
      ARMSourceType sourceType, String templateBody, GitFileConfig gitFileConfig, ARMScopeType scopeType,
      boolean isBlueprint) {
    super(type, harnessApiVersion, description, ARM.name(), variables, mappingBlueprints);
    this.sourceType = sourceType;
    this.templateBody = templateBody;
    this.gitFileConfig = gitFileConfig;
    this.scopeType = scopeType;
    this.isBlueprint = isBlueprint;
  }
}
