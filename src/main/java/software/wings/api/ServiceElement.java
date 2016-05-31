/**
 *
 */
package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * @author Rishi
 */
public class ServiceElement implements ContextElement {
  private String name;

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SERVICE;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }
}
