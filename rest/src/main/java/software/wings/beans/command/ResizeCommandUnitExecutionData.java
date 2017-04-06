package software.wings.beans.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 2/28/17.
 */
public class ResizeCommandUnitExecutionData extends CommandExecutionData {
  private List<String> hostNames = new ArrayList<>();
  private List<String> containerIds = new ArrayList<>();

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  public List<String> getContainerIds() {
    return containerIds;
  }

  public void setContainerIds(List<String> containerIds) {
    this.containerIds = containerIds;
  }

  public static final class ResizeCommandUnitExecutionDataBuilder {
    private List<String> hostNames = new ArrayList<>();
    private List<String> containerIds = new ArrayList<>();

    private ResizeCommandUnitExecutionDataBuilder() {}

    public static ResizeCommandUnitExecutionDataBuilder aResizeCommandUnitExecutionData() {
      return new ResizeCommandUnitExecutionDataBuilder();
    }

    public ResizeCommandUnitExecutionDataBuilder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public ResizeCommandUnitExecutionDataBuilder withContainerIds(List<String> containerIds) {
      this.containerIds = containerIds;
      return this;
    }

    public ResizeCommandUnitExecutionDataBuilder but() {
      return aResizeCommandUnitExecutionData().withHostNames(hostNames).withContainerIds(containerIds);
    }

    public ResizeCommandUnitExecutionData build() {
      ResizeCommandUnitExecutionData resizeCommandUnitExecutionData = new ResizeCommandUnitExecutionData();
      resizeCommandUnitExecutionData.setHostNames(hostNames);
      resizeCommandUnitExecutionData.setContainerIds(containerIds);
      return resizeCommandUnitExecutionData;
    }
  }
}
