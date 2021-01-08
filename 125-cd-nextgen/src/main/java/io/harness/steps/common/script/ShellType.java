package io.harness.steps.common.script;

import io.harness.shell.ScriptType;

public enum ShellType {
  Bash(ScriptType.BASH),
  PowerShell(ScriptType.POWERSHELL);

  private final ScriptType scriptType;

  ShellType(ScriptType scriptType) {
    this.scriptType = scriptType;
  }

  public ScriptType getScriptType() {
    return scriptType;
  }
}
