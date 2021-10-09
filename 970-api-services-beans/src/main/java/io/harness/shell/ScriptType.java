package io.harness.shell;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum ScriptType {
  BASH("Bash Script"),
  POWERSHELL("PowerShell Script");

  private String displayName;

  ScriptType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
