package software.wings.yaml.command;

import software.wings.beans.command.TailFilePatternEntry;

import java.util.List;

public class ExecCommandUnitYaml extends SshCommandUnitYaml {
  public String commandPath;
  public String commandString;
  public List<TailFilePatternEntry> tailPatterns;

  public ExecCommandUnitYaml() {
    super();
  }

  public String getCommandPath() {
    return commandPath;
  }

  public void setCommandPath(String commandPath) {
    this.commandPath = commandPath;
  }

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }

  public List<TailFilePatternEntry> getTailPatterns() {
    return tailPatterns;
  }

  public void setTailPatterns(List<TailFilePatternEntry> tailPatterns) {
    this.tailPatterns = tailPatterns;
  }
}
