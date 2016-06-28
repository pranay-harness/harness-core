package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.stencils.OverridingStencil;

import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/28/16.
 */
public class OverridingCommandUnitDescriptor implements OverridingStencil<CommandUnit>, CommandUnitDescriptor {
  private CommandUnitDescriptor commandUnitDescriptor;
  private Optional<String> overridingName = Optional.empty();
  private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  public OverridingCommandUnitDescriptor(CommandUnitDescriptor commandUnitDescriptor) {
    this.commandUnitDescriptor = commandUnitDescriptor;
  }

  @Override
  public String getType() {
    return commandUnitDescriptor.getType();
  }

  @Override
  @JsonIgnore
  public Class<? extends CommandUnit> getTypeClass() {
    return commandUnitDescriptor.getTypeClass();
  }

  @Override
  public JsonNode getJsonSchema() {
    return overridingJsonSchema.orElse(commandUnitDescriptor.getJsonSchema());
  }

  @Override
  public Object getUiSchema() {
    return commandUnitDescriptor.getUiSchema();
  }

  @Override
  public String getName() {
    return overridingName.orElse(commandUnitDescriptor.getName());
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return commandUnitDescriptor.getOverridingStencil();
  }

  @Override
  public CommandUnit newInstance(String id) {
    return commandUnitDescriptor.newInstance(id);
  }

  @Override
  public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
    this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
  }

  @Override
  public String getOverridingName() {
    return overridingName.orElse(null);
  }

  @Override
  public void setOverridingName(String overridingName) {
    this.overridingName = Optional.ofNullable(overridingName);
  }
}
