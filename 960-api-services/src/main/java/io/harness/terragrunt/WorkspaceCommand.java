package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public enum WorkspaceCommand {
  SELECT("select"),
  NEW("new");
  public String command;

  WorkspaceCommand(String command) {
    this.command = command;
  }
}
