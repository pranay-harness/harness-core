package software.wings.beans.shellscript.provisioner;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.ShellScriptProvisionerOutputElement;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.NameValuePair;

import java.util.List;

@JsonTypeName("SHELL_SCRIPT")
@Data
@EqualsAndHashCode(callSuper = true)
public class ShellScriptInfrastructureProvisioner extends InfrastructureProvisioner {
  @NotEmpty private String scriptBody;

  public ShellScriptInfrastructureProvisioner() {
    setInfrastructureProvisionerType(InfrastructureProvisionerType.SHELL_SCRIPT.toString());
  }

  @Override
  public String variableKey() {
    return ShellScriptProvisionerOutputElement.KEY;
  }

  @Builder
  private ShellScriptInfrastructureProvisioner(String uuid, String appId, String name, String scriptBody,
      List<NameValuePair> variables, List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId,
      String description, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String entityYamlPath) {
    super(name, description, InfrastructureProvisionerType.SHELL_SCRIPT.name(), variables, mappingBlueprints, accountId,
        uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.scriptBody = scriptBody;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class Yaml extends InfraProvisionerYaml {
    private String scriptBody;

    @Builder
    public Yaml(String type, String harnessApiVersion, String description, String infrastructureProvisionerType,
        List<NameValuePair.Yaml> variables, List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints,
        String scriptBody) {
      super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.scriptBody = scriptBody;
    }
  }
}
