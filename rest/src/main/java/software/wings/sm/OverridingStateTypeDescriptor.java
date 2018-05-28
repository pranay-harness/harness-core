package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import java.util.List;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/6/16.
 */
public class OverridingStateTypeDescriptor implements StateTypeDescriptor, OverridingStencil<State> {
  private final StateTypeDescriptor stateTypeDescriptor;
  @JsonIgnore private Optional<String> overridingName = Optional.empty();
  @JsonIgnore private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  public boolean isActive(String accountId) {
    return true;
  }

  /**
   * Instantiates a new Overriding state type descriptor.
   *
   * @param stateTypeDescriptor the state type descriptor
   */
  public OverridingStateTypeDescriptor(StateTypeDescriptor stateTypeDescriptor) {
    this.stateTypeDescriptor = stateTypeDescriptor;
  }

  /**
   * Getter for property 'overridingJsonSchema'.
   *
   * @return Value for property 'overridingJsonSchema'.
   */
  @Override
  public JsonNode getOverridingJsonSchema() {
    return overridingJsonSchema.orElse(null);
  }

  /**
   * Setter for property 'overridingJsonSchema'.
   *
   * @param overridingJsonSchema Value to set for property 'overridingJsonSchema'.
   */
  @Override
  public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
    this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
  }

  /**
   * Gets overriding state type.
   *
   * @return the overriding state type
   */
  @Override
  public String getOverridingName() {
    return overridingName.orElse(null);
  }

  /**
   * Sets overriding state type.
   *
   * @param overridingName the overriding state type
   */
  @Override
  public void setOverridingName(String overridingName) {
    this.overridingName = Optional.ofNullable(overridingName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return stateTypeDescriptor.getType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<StateTypeScope> getScopes() {
    return stateTypeDescriptor.getScopes();
  }

  @Override
  public List<String> getPhaseStepTypes() {
    return stateTypeDescriptor.getPhaseStepTypes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<?> getTypeClass() {
    return stateTypeDescriptor.getTypeClass();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JsonNode getJsonSchema() {
    return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy()
                                            : stateTypeDescriptor.getJsonSchema();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getUiSchema() {
    return stateTypeDescriptor.getUiSchema();
  }

  @Override
  public String getName() {
    return overridingName.orElse(stateTypeDescriptor.getName());
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return stateTypeDescriptor.getOverridingStencil();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public State newInstance(String id) {
    return stateTypeDescriptor.newInstance(id);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return stateTypeDescriptor == null ? null : stateTypeDescriptor.getStencilCategory();
  }

  @SuppressFBWarnings("BX_UNBOXING_IMMEDIATELY_REBOXED")
  @Override
  public Integer getDisplayOrder() {
    return stateTypeDescriptor == null ? DEFAULT_DISPLAY_ORDER : stateTypeDescriptor.getDisplayOrder();
  }

  @Override
  public boolean matches(Object context) {
    return stateTypeDescriptor.matches(context);
  }
}
