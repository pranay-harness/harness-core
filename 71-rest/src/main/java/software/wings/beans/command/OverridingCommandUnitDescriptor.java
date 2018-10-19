package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/28/16.
 */
public class OverridingCommandUnitDescriptor implements OverridingStencil<CommandUnit>, CommandUnitDescriptor {
  private CommandUnitDescriptor commandUnitDescriptor;
  private Optional<String> overridingName = Optional.empty();
  private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  /**
   * Instantiates a new Overriding command unit descriptor.
   *
   * @param commandUnitDescriptor the command unit descriptor
   */
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
    return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy()
                                            : commandUnitDescriptor.getJsonSchema();
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
  public JsonNode getOverridingJsonSchema() {
    return overridingJsonSchema.orElse(null);
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

  @Override
  public StencilCategory getStencilCategory() {
    return commandUnitDescriptor == null ? null : commandUnitDescriptor.getStencilCategory();
  }

  @Override
  public Integer getDisplayOrder() {
    return commandUnitDescriptor == null ? Integer.valueOf(DEFAULT_DISPLAY_ORDER)
                                         : commandUnitDescriptor.getDisplayOrder();
  }

  @Override
  public boolean matches(Object context) {
    return commandUnitDescriptor.matches(context);
  }
}
