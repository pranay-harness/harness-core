package software.wings.beans.infrastructure.instance.key;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerInstanceKey extends InstanceKey {
  private String containerId;

  public static final class Builder {
    private String containerId;

    private Builder() {}

    public static Builder aContainerInstanceKey() {
      return new Builder();
    }

    public Builder withContainerId(String containerId) {
      this.containerId = containerId;
      return this;
    }

    public Builder but() {
      return aContainerInstanceKey().withContainerId(containerId);
    }

    public ContainerInstanceKey build() {
      ContainerInstanceKey containerInstanceKey = new ContainerInstanceKey();
      containerInstanceKey.setContainerId(containerId);
      return containerInstanceKey;
    }
  }
}
