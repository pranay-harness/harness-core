package software.wings.sm;

import ro.fortsoft.pf4j.ExtensionPoint;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Plugin interface for adding state.
 *
 * @author Rishi
 */
public interface StateTypeDescriptor extends ExtensionPoint {
  public String getType();

  public List<StateTypeScope> getScopes();

  public Object getJsonSchema();

  public Object getUiSchema();

  /**
   * New instance.
   *
   * @param id the id
   * @return the state
   */
  public State newInstance(String id);
}
