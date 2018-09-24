package software.wings.api;

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
