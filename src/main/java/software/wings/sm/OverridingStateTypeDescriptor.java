package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Created by peeyushaggarwal on 6/6/16.
 */
public class OverridingStateTypeDescriptor implements StateTypeDescriptor {
  private final StateTypeDescriptor stateTypeDescriptor;
  @JsonIgnore private Object overridingJsonSchema;

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
  public Object getOverridingJsonSchema() {
    return overridingJsonSchema;
  }

  /**
   * Setter for property 'overridingJsonSchema'.
   *
   * @param overridingJsonSchema Value to set for property 'overridingJsonSchema'.
   */
  public void setOverridingJsonSchema(Object overridingJsonSchema) {
    this.overridingJsonSchema = overridingJsonSchema;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends State> getStateClass() {
    return stateTypeDescriptor.getStateClass();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getJsonSchema() {
    if (getOverridingJsonSchema() != null) {
      return overridingJsonSchema;
    } else {
      return stateTypeDescriptor.getJsonSchema();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getUiSchema() {
    return stateTypeDescriptor.getUiSchema();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public State newInstance(String id) {
    return stateTypeDescriptor.newInstance(id);
  }
}
