package software.wings.beans.shellscript.provisioner;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerType;

@JsonTypeName("SHELL_SCRIPT")
@Data
public class ShellScriptInfrastructureProvisioner extends InfrastructureProvisioner {
  @NotEmpty private String scriptBody;

  public ShellScriptInfrastructureProvisioner() {
    setInfrastructureProvisionerType(InfrastructureProvisionerType.SHELL_SCRIPT.toString());
  }

  @Override
  public String variableKey() {
    return "shellScriptProvisioner";
  }
}
