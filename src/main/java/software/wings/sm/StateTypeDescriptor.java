package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ro.fortsoft.pf4j.ExtensionPoint;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Plugin interface for adding state.
 *
 * @author Rishi
 */
public interface StateTypeDescriptor extends ExtensionPoint {
  /**
   * Gets type.
   *
   * @return the type
   */
  String getType();

  /**
   * Gets scopes.
   *
   * @return the scopes
   */
  List<StateTypeScope> getScopes();

  /**
   * Gets state class.
   *
   * @return the state class
   */
  @JsonIgnore Class<? extends State> getStateClass();

  /**
   * Gets json schema.
   *
   * @return the json schema
   */
  Object getJsonSchema();

  /**
   * Gets ui schema.
   *
   * @return the ui schema
   */
  Object getUiSchema();

  /**
   * New instance.
   *
   * @param id the id
   * @return the state
   */
  State newInstance(String id);
}
