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
  private int newServiceInstanceCount;
  private int oldServiceInstanceCount;

  public ContainerServiceElement getContainerServiceElement() {
    return containerServiceElement;
  }

  public void setContainerServiceElement(ContainerServiceElement containerServiceElement) {
    this.containerServiceElement = containerServiceElement;
  }

  public int getNewServiceInstanceCount() {
    return newServiceInstanceCount;
  }

  public void setNewServiceInstanceCount(int newServiceInstanceCount) {
    this.newServiceInstanceCount = newServiceInstanceCount;
  }

  public int getOldServiceInstanceCount() {
    return oldServiceInstanceCount;
  }

  public void setOldServiceInstanceCount(int oldServiceInstanceCount) {
    this.oldServiceInstanceCount = oldServiceInstanceCount;
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
    private int newServiceInstanceCount;
    private int oldServiceInstanceCount;

    private ContainerUpgradeRequestElementBuilder() {}

    public static ContainerUpgradeRequestElementBuilder aContainerUpgradeRequestElement() {
      return new ContainerUpgradeRequestElementBuilder();
    }

    public ContainerUpgradeRequestElementBuilder withContainerServiceElement(
        ContainerServiceElement containerServiceElement) {
      this.containerServiceElement = containerServiceElement;
      return this;
    }

    public ContainerUpgradeRequestElementBuilder withNewServiceInstanceCount(int newServiceInstanceCount) {
      this.newServiceInstanceCount = newServiceInstanceCount;
      return this;
    }

    public ContainerUpgradeRequestElementBuilder withOldServiceInstanceCount(int oldServiceInstanceCount) {
      this.oldServiceInstanceCount = oldServiceInstanceCount;
      return this;
    }

    public ContainerUpgradeRequestElement build() {
      ContainerUpgradeRequestElement containerUpgradeRequestElement = new ContainerUpgradeRequestElement();
      containerUpgradeRequestElement.setContainerServiceElement(containerServiceElement);
      containerUpgradeRequestElement.setNewServiceInstanceCount(newServiceInstanceCount);
      containerUpgradeRequestElement.setOldServiceInstanceCount(oldServiceInstanceCount);
      return containerUpgradeRequestElement;
    }
  }
}
