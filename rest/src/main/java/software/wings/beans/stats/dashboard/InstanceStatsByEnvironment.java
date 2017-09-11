package software.wings.beans.stats.dashboard;

import java.util.List;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStatsByEnvironment {
  private EnvironmentSummary environmentSummary;
  private List<InstanceStatsByArtifact> instanceStatsByArtifactList;

  public EnvironmentSummary getEnvironmentSummary() {
    return environmentSummary;
  }

  private void setEnvironmentSummary(EnvironmentSummary environmentSummary) {
    this.environmentSummary = environmentSummary;
  }

  public List<InstanceStatsByArtifact> getInstanceStatsByArtifactList() {
    return instanceStatsByArtifactList;
  }

  private void setInstanceStatsByArtifactList(List<InstanceStatsByArtifact> instanceStatsByArtifactList) {
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