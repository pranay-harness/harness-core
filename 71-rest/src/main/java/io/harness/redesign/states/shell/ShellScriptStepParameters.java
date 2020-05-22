package io.harness.redesign.states.shell;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.github.reinert.jjschema.Attributes;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Property;
import software.wings.service.impl.SSHKeyDataProvider;
import software.wings.service.impl.WinRmConnectionAttributesDataProvider;
import software.wings.sm.states.ShellScriptState;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
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

  @Attributes(title = "Script Output Variables") String outputVars;
  @Attributes(title = "Publish Variable Name") String sweepingOutputName;
}
