package software.wings.beans.shellscript.provisioner;

import io.harness.beans.EmbeddedUser;

import software.wings.api.ShellScriptProvisionerOutputElement;
import software.wings.beans.InfraProvisionerYaml;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.NameValuePair;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

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
}
