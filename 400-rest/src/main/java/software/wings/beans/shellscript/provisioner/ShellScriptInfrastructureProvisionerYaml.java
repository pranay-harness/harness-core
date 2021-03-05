package software.wings.beans.shellscript.provisioner;

import software.wings.beans.InfraProvisionerYaml;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.NameValuePairYaml;

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
public final class ShellScriptInfrastructureProvisionerYaml extends InfraProvisionerYaml {
  private String scriptBody;

  @Builder
  public ShellScriptInfrastructureProvisionerYaml(String type, String harnessApiVersion, String description,
      String infrastructureProvisionerType, List<NameValuePairYaml> variables,
      List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, String scriptBody) {
    super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
    this.scriptBody = scriptBody;
  }
}
