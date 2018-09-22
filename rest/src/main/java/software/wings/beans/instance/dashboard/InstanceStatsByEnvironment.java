package software.wings.beans.instance.dashboard;

import software.wings.beans.infrastructure.instance.SyncStatus;

import java.util.List;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStatsByEnvironment {
  private EnvironmentSummary environmentSummary;
  private List<InstanceStatsByArtifact> instanceStatsByArtifactList;
  private List<SyncStatus> infraMappingSyncStatusList;
  private boolean hasSyncIssues;

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

  public List<SyncStatus> getInfraMappingSyncStatusList() {
    return infraMappingSyncStatusList;
  }

  public void setInfraMappingSyncStatusList(List<SyncStatus> infraMappingSyncStatusList) {
    this.infraMappingSyncStatusList = infraMappingSyncStatusList;
  }

  public boolean isHasSyncIssues() {
    return hasSyncIssues;
  }

  public void setHasSyncIssues(boolean hasSyncIssues) {
    this.hasSyncIssues = hasSyncIssues;
  }

  public static final class Builder {
    private EnvironmentSummary environmentSummary;
    private List<InstanceStatsByArtifact> instanceStatsByArtifactList;
    private List<SyncStatus> infraMappingSyncStatusList;
    private boolean hasSyncIssues;

    private Builder() {}

    public static Builder anInstanceStatsByEnvironment() {
      return new Builder();
    }

    public Builder environmentSummary(EnvironmentSummary environmentSummary) {
      this.environmentSummary = environmentSummary;
      return this;
    }

    public Builder instanceStatsByArtifactList(List<InstanceStatsByArtifact> instanceStatsByArtifactList) {
      this.instanceStatsByArtifactList = instanceStatsByArtifactList;
      return this;
    }

    public Builder infraMappingSyncStatusList(List<SyncStatus> infraMappingSyncStatusList) {
      this.infraMappingSyncStatusList = infraMappingSyncStatusList;
      return this;
    }

    public Builder hasSyncIssues(boolean hasSyncIssues) {
      this.hasSyncIssues = hasSyncIssues;
      return this;
    }

    public InstanceStatsByEnvironment build() {
      InstanceStatsByEnvironment instanceStatsByEnvironment = new InstanceStatsByEnvironment();
      instanceStatsByEnvironment.setEnvironmentSummary(environmentSummary);
      instanceStatsByEnvironment.setInstanceStatsByArtifactList(instanceStatsByArtifactList);
      instanceStatsByEnvironment.setInfraMappingSyncStatusList(infraMappingSyncStatusList);
      instanceStatsByEnvironment.setHasSyncIssues(hasSyncIssues);
      return instanceStatsByEnvironment;
    }
  }
}