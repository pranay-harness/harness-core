package software.wings.api;

import lombok.Data;

/**
 * Created by bzane on 9/11/17.
 */
@Data
public class ContainerServiceData {
  private String name;
  private int previousCount;
  private int desiredCount;

  public static final class ContainerServiceDataBuilder {
    private String name;
    private int previousCount;
    private int desiredCount;

    private ContainerServiceDataBuilder() {}

    public static ContainerServiceDataBuilder aContainerServiceData() {
      return new ContainerServiceDataBuilder();
    }

    public ContainerServiceDataBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ContainerServiceDataBuilder withPreviousCount(int previousCount) {
      this.previousCount = previousCount;
      return this;
    }

    public ContainerServiceDataBuilder withDesiredCount(int desiredCount) {
      this.desiredCount = desiredCount;
      return this;
    }

    public ContainerServiceDataBuilder but() {
      return aContainerServiceData().withName(name).withPreviousCount(previousCount).withDesiredCount(desiredCount);
    }

    public ContainerServiceData build() {
      ContainerServiceData containerServiceData = new ContainerServiceData();
      containerServiceData.setName(name);
      containerServiceData.setPreviousCount(previousCount);
      containerServiceData.setDesiredCount(desiredCount);
      return containerServiceData;
    }
  }
}
