package software.wings.beans;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.Activity.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;

import java.util.List;

@Value
@Builder
public class ActivityAttributes {
  private Type type;
  private String commandName;
  private String commandType;
  private String commandUnitType;
  private List<CommandUnit> commandUnits;
  private Artifact artifact;
}