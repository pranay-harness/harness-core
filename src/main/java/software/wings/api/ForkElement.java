/**
 *
 */

package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * The Class ForkElement.
 *
 * @author Rishi
 */
public class ForkElement implements ContextElement {
  private String parentId;

  private String stateName;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.FORK;
  }

  /**
   * Gets uuid.
   *
   * @return the uuid
   */
  @Override
  public String getUuid() {
    return parentId + "-fork-" + stateName;
  }

  @Override
  public String getName() {
    return "Fork-" + stateName;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public static final class Builder {
    private String parentId;
    private String stateName;

    private Builder() {}

    public static Builder aForkElement() {
      return new Builder();
    }

    public Builder withParentId(String parentId) {
      this.parentId = parentId;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public ForkElement build() {
      ForkElement forkElement = new ForkElement();
      forkElement.setParentId(parentId);
      forkElement.setStateName(stateName);
      return forkElement;
    }
  }
}
