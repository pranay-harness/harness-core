package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
public class ContainerUpgradeRequestElement implements ContextElement {
  private ContainerServiceElement containerServiceElement;
  private int newInstanceCount;
  private int oldInstanceCount;

  public ContainerServiceElement getContainerServiceElement() {
    return containerServiceElement;
  }

  public void setContainerServiceElement(ContainerServiceElement containerServiceElement) {
    this.containerServiceElement = containerServiceElement;
  }

  public int getNewInstanceCount() {
    return newInstanceCount;
  }

  public void setNewInstanceCount(int newInstanceCount) {
    this.newInstanceCount = newInstanceCount;
  }

  public int getOldInstanceCount() {
    return oldInstanceCount;
  }

  public void setOldInstanceCount(int oldInstanceCount) {
    this.oldInstanceCount = oldInstanceCount;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return Constants.CONTAINER_UPGRADE_REQUEST_PARAM;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  public static final class ContainerUpgradeRequestElementBuilder {
    private ContainerServiceElement containerServiceElement;
    private int newInstanceCount;
    private int oldInstanceCount;

    private ContainerUpgradeRequestElementBuilder() {}

    public static ContainerUpgradeRequestElementBuilder aContainerUpgradeRequestElement() {
      return new ContainerUpgradeRequestElementBuilder();
    }

    public ContainerUpgradeRequestElementBuilder withContainerServiceElement(
        ContainerServiceElement containerServiceElement) {
      this.containerServiceElement = containerServiceElement;
      return this;
    }

    public ContainerUpgradeRequestElementBuilder withNewInstanceCount(int newInstanceCount) {
      this.newInstanceCount = newInstanceCount;
      return this;
    }

    public ContainerUpgradeRequestElementBuilder withOldInstanceCount(int oldInstanceCount) {
      this.oldInstanceCount = oldInstanceCount;
      return this;
    }

    public ContainerUpgradeRequestElement build() {
      ContainerUpgradeRequestElement containerUpgradeRequestElement = new ContainerUpgradeRequestElement();
      containerUpgradeRequestElement.setContainerServiceElement(containerServiceElement);
      containerUpgradeRequestElement.setNewInstanceCount(newInstanceCount);
      containerUpgradeRequestElement.setOldInstanceCount(oldInstanceCount);
      return containerUpgradeRequestElement;
    }
  }
}
