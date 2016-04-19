/**
 *
 */
package software.wings.sm;

import ro.fortsoft.pf4j.ExtensionPoint;

/**
 * @author Rishi
 *
 */
public interface StateTypeDescriptor extends ExtensionPoint {
  public String getType();

  public Object getJsonSchema();

  public Object getUiSchema();

  public State newInstance(String id);
}
