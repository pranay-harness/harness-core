package io.harness.redesign.states.shell;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.shell.ScriptType;

import software.wings.service.impl.SSHKeyDataProvider;
import software.wings.service.impl.WinRmConnectionAttributesDataProvider;
import software.wings.sm.states.ShellScriptState;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import com.github.reinert.jjschema.Attributes;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Property;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShellScriptStepParameters implements StepParameters {
  @Attributes(title = "Execute on Delegate") boolean executeOnDelegate;
  @NotEmpty @Attributes(title = "Target Host") String host;
  @NotEmpty @Attributes(title = "Tags") List<String> tags;
  @NotEmpty @DefaultValue("SSH") @Attributes(title = "Connection Type") ShellScriptState.ConnectionType connectionType;

  @NotEmpty
  @Attributes(title = "SSH Key")
  @EnumData(enumDataProvider = SSHKeyDataProvider.class)
  @Property("sshKeyRef")
  String sshKeyRef;

  @NotEmpty
  @Attributes(title = "Connection Attributes")
  @EnumData(enumDataProvider = WinRmConnectionAttributesDataProvider.class)
  String connectionAttributes;

  @Attributes(title = "Working Directory") String commandPath;
  @NotEmpty @DefaultValue("BASH") @Attributes(title = "Script Type") ScriptType scriptType;
  @NotEmpty @Attributes(title = "Script") String scriptString;
  @NotEmpty @DefaultValue("3600") @Attributes(title = "Timeout in secs") String timeoutSecs;

  @Attributes(title = "Script Output Variables") String outputVars;
  @Attributes(title = "Publish Variable Name") String sweepingOutputName;
  @Attributes(title = "Publish Variable Scope") String sweepingOutputScope;
}
