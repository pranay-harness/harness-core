package software.wings.beans;

import software.wings.beans.Activity.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActivityAttributes {
  private Type type;
  private String commandName;
  private String commandType;
  private List<CommandUnit> commandUnits;
  private Artifact artifact;
}
