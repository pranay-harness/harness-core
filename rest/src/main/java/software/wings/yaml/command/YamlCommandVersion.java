package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

import java.util.ArrayList;
import java.util.List;

public class YamlCommandVersion {
  @YamlSerialize public long version;
  @YamlSerialize public List<YamlCommandUnit> commandUnits = new ArrayList<>();

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public List<YamlCommandUnit> getCommandUnits() {
    return commandUnits;
  }

  public void setCommandUnits(List<YamlCommandUnit> commandUnits) {
    this.commandUnits = commandUnits;
  }
}
