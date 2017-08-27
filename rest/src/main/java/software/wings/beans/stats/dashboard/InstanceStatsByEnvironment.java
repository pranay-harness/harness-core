package software.wings.beans.stats.dashboard;

import software.wings.exception.WingsException;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStatsByEnvironment {
  private EnvironmentSummary environmentSummary;
  private List<InstanceStatsByArtifact> instanceStatsByArtifactList;

  public EnvironmentSummary getEnvironmentSummary() {
    return environmentSummary;
  }

  public void setEnvironmentSummary(EnvironmentSummary environmentSummary) {
    this.environmentSummary = environmentSummary;
  }

  public List<InstanceStatsByArtifact> getInstanceStatsByArtifactList() {
    return instanceStatsByArtifactList;
  }

  public void setInstanceStatsByArtifactList(List<InstanceStatsByArtifact> instanceStatsByArtifactList) {
    this.instanceStatsByArtifactList = instanceStatsByArtifactList;
  }

  public static final class Builder {
    private EnvironmentSummary environmentSummary;
    private List<InstanceStatsByArtifact> instanceStatsByArtifactList;

    private Builder() {}

    public static Builder anInstanceStatsByEnvironment() {
      return new Builder();
    }

    public Builder withEnvironmentSummary(EnvironmentSummary environmentSummary) {
      this.environmentSummary = environmentSummary;
      return this;
    }

    public Builder withInstanceStatsByArtifactList(List<InstanceStatsByArtifact> instanceStatsByArtifactList) {
      this.instanceStatsByArtifactList = instanceStatsByArtifactList;
      return this;
    }

    public InstanceStatsByEnvironment build() {
      InstanceStatsByEnvironment instanceStatsByArtifact = new InstanceStatsByEnvironment();
      instanceStatsByArtifact.setEnvironmentSummary(environmentSummary);
      instanceStatsByArtifact.setInstanceStatsByArtifactList(instanceStatsByArtifactList);
      return instanceStatsByArtifact;
    }
  }
}