package software.wings.beans.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 2/28/17.
 */
public class ResizeCommandUnitExecutionData extends CommandExecutionData {
  private List<String> containerIds = new ArrayList<>();

  /**
   * Gets container ids.
   *
   * @return the container ids
   */
  public List<String> getContainerIds() {
    return containerIds;
  }

  /**
   * Sets container ids.
   *
   * @param containerIds the container ids
   */
  public void setContainerIds(List<String> containerIds) {
    this.containerIds = containerIds;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private List<String> containerIds;

    private Builder() {}

    /**
     * A resize command unit execution data builder.
     *
     * @return the builder
     */
    public static Builder aResizeCommandUnitExecutionData() {
      return new Builder();
    }

    /**
     * With container ids builder.
     *
     * @param containerIds the container ids
     * @return the builder
     */
    public Builder withContainerIds(List<String> containerIds) {
      this.containerIds = containerIds;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aResizeCommandUnitExecutionData().withContainerIds(containerIds);
    }

    /**
     * Build resize command unit execution data.
     *
     * @return the resize command unit execution data
     */
    public ResizeCommandUnitExecutionData build() {
      ResizeCommandUnitExecutionData resizeCommandUnitExecutionData = new ResizeCommandUnitExecutionData();
      resizeCommandUnitExecutionData.setContainerIds(containerIds);
      return resizeCommandUnitExecutionData;
    }
  }
}
