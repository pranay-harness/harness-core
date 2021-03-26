package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.ARM;

import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.ARMResourceType;
import io.harness.azure.model.ARMScopeType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class ARMInfrastructureProvisionerYaml extends InfraProvisionerYaml {
  private ARMResourceType resourceType;
  private ARMSourceType sourceType;
  private String templateBody;
  private GitFileConfig gitFileConfig;
  private ARMScopeType scopeType;

  @Builder
  public ARMInfrastructureProvisionerYaml(String type, String harnessApiVersion, String description,
      List<NameValuePairYaml> variables, List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints,
      ARMSourceType sourceType, String templateBody, GitFileConfig gitFileConfig, ARMScopeType scopeType,
      ARMResourceType resourceType) {
    super(type, harnessApiVersion, description, ARM.name(), variables, mappingBlueprints);
    this.sourceType = sourceType;
    this.templateBody = trim(templateBody);
    this.gitFileConfig = gitFileConfig;
    this.scopeType = scopeType;
    this.resourceType = resourceType;
  }
}
