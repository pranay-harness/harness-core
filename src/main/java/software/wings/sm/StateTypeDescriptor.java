package software.wings.sm;

import ro.fortsoft.pf4j.ExtensionPoint;

/**
 * Plugin interface for adding state.
 * @author Rishi
 */
public interface StateTypeDescriptor extends ExtensionPoint {
  String getType();

  Object getJsonSchema();

  Object getUiSchema();

  State newInstance(String id);
}
