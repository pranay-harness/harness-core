package software.wings.beans.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnitExecutionData extends CommandExecutionData {
  private List<String> podNames = new ArrayList<>();

  public List<String> getPodNames() {
    return podNames;
  }

  public void setPodNames(List<String> podNames) {
    this.podNames = podNames;
  }

  public static final class KubernetesResizeCommandUnitExecutionDataBuilder {
    private List<String> podNames = new ArrayList<>();

    private KubernetesResizeCommandUnitExecutionDataBuilder() {}

    public static KubernetesResizeCommandUnitExecutionDataBuilder aKubernetesResizeCommandUnitExecutionData() {
      return new KubernetesResizeCommandUnitExecutionDataBuilder();
    }

    public KubernetesResizeCommandUnitExecutionDataBuilder withPodNames(List<String> podNames) {
      this.podNames = podNames;
      return this;
    }

    public KubernetesResizeCommandUnitExecutionDataBuilder but() {
      return aKubernetesResizeCommandUnitExecutionData().withPodNames(podNames);
    }

    public KubernetesResizeCommandUnitExecutionData build() {
      KubernetesResizeCommandUnitExecutionData kubernetesResizeCommandUnitExecutionData =
          new KubernetesResizeCommandUnitExecutionData();
      kubernetesResizeCommandUnitExecutionData.setPodNames(podNames);
      return kubernetesResizeCommandUnitExecutionData;
    }
  }
}
