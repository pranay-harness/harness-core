package software.wings.yaml.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandUnitType;

/**
 * This yaml is used to represent a command reference. A command could be referred from another command, in that case,
 * we need a ref.
 *
 * @author rktummala on 11/16/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("COMMAND")
public class CommandRefYaml extends AbstractCommandUnit.Yaml {
  public CommandRefYaml() {
    super(CommandUnitType.COMMAND.name());
  }

  @Builder
  public CommandRefYaml(String name, String deploymentType) {
    super(name, CommandUnitType.COMMAND.name(), deploymentType);
  }
}
