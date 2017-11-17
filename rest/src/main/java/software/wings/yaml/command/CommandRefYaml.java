package software.wings.yaml.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.AbstractCommandUnit;

/**
 * This yaml is used to represent a command reference. A command could be referred from another command, in that case,
 * we need a ref.
 *
 * @author rktummala on 11/16/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandRefYaml extends AbstractCommandUnit.Yaml {
  public static final class Builder extends AbstractCommandUnit.Yaml.Builder {
    private Builder() {}

    public static Builder aYaml() {
      return new Builder();
    }

    @Override
    protected CommandRefYaml getCommandUnitYaml() {
      return new CommandRefYaml();
    }
  }
}
