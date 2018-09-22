/**
 *
 */

package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

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
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  /**
   * Gets state name.
   *
   * @return the state name
   */
  public String getStateName() {
    return stateName;
  }

  /**
   * Sets state name.
   *
   * @param stateName the state name
   */
  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  /**
   * Gets parent id.
   *
   * @return the parent id
   */
  public String getParentId() {
    return parentId;
  }

  /**
   * Sets parent id.
   *
   * @param parentId the parent id
   */
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String parentId;
    private String stateName;

    private Builder() {}

    /**
     * A fork element builder.
     *
     * @return the builder
     */
    public static Builder aForkElement() {
      return new Builder();
    }

    /**
     * With parent id builder.
     *
     * @param parentId the parent id
     * @return the builder
     */
    public Builder withParentId(String parentId) {
      this.parentId = parentId;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * Build fork element.
     *
     * @return the fork element
     */
    public ForkElement build() {
      ForkElement forkElement = new ForkElement();
      forkElement.setParentId(parentId);
      forkElement.setStateName(stateName);
      return forkElement;
    }
  }
}
