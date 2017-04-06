package software.wings.beans.command;

import software.wings.cloudprovider.ContainerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 2/28/17.
 */
public class ResizeCommandUnitExecutionData extends CommandExecutionData {
  private List<ContainerInfo> containerInfos = new ArrayList<>();

  public List<ContainerInfo> getContainerInfos() {
    return containerInfos;
  }

  public void setContainerInfos(List<ContainerInfo> containerInfos) {
    this.containerInfos = containerInfos;
  }

  public static final class ResizeCommandUnitExecutionDataBuilder {
    private List<ContainerInfo> containerInfos = new ArrayList<>();

    private ResizeCommandUnitExecutionDataBuilder() {}

    public static ResizeCommandUnitExecutionDataBuilder aResizeCommandUnitExecutionData() {
      return new ResizeCommandUnitExecutionDataBuilder();
    }

    public ResizeCommandUnitExecutionDataBuilder withContainerInfos(List<ContainerInfo> containerInfos) {
      this.containerInfos = containerInfos;
      return this;
    }

    public ResizeCommandUnitExecutionDataBuilder but() {
      return aResizeCommandUnitExecutionData().withContainerInfos(containerInfos);
    }

    public ResizeCommandUnitExecutionData build() {
      ResizeCommandUnitExecutionData resizeCommandUnitExecutionData = new ResizeCommandUnitExecutionData();
      resizeCommandUnitExecutionData.setContainerInfos(containerInfos);
      return resizeCommandUnitExecutionData;
    }
  }
}
