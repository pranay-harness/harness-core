package software.wings.beans.command;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnitExecutionData extends CommandExecutionData {
  /**
   * The type Builder.
   */
  public static final class Builder {
    private Builder() {}

    /**
     * A resize command unit execution data builder.
     *
     * @return the builder
     */
    public static Builder aKubernetesResizeCommandUnitExecutionData() {
      return new Builder();
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aKubernetesResizeCommandUnitExecutionData();
    }

    /**
     * Build resize command unit execution data.
     *
     * @return the resize command unit execution data
     */
    public KubernetesResizeCommandUnitExecutionData build() {
      KubernetesResizeCommandUnitExecutionData kubernetesResizeCommandUnitExecutionData =
          new KubernetesResizeCommandUnitExecutionData();
      return kubernetesResizeCommandUnitExecutionData;
    }
  }
}
