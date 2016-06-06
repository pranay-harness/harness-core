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
  String getType();

  List<StateTypeScope> getScopes();

  @JsonIgnore Class<? extends State> getStateClass();

  Object getJsonSchema();

  Object getUiSchema();

  /**
   * New instance.
   *
   * @param id the id
   * @return the state
   */
  State newInstance(String id);
}
