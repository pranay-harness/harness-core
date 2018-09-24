package software.wings.beans.instance.dashboard;

import java.util.List;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStatsByService {
  private long totalCount;
  private ServiceSummary serviceSummary;
  private List<InstanceStatsByEnvironment> instanceStatsByEnvList;

  public long getTotalCount() {
    return totalCount;
  }

  private void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public ServiceSummary getServiceSummary() {
    return serviceSummary;
  }

  private void setServiceSummary(ServiceSummary serviceSummary) {
    this.serviceSummary = serviceSummary;
  }

  public List<InstanceStatsByEnvironment> getInstanceStatsByEnvList() {
    return instanceStatsByEnvList;
  }

  private void setInstanceStatsByEnvList(List<InstanceStatsByEnvironment> instanceStatsByEnvList) {
    this.instanceStatsByEnvList = instanceStatsByEnvList;
  }

  public InstanceStatsByService clone(long newCount) {
    return Builder.anInstanceStatsByService()
        .withTotalCount(newCount)
        .withServiceSummary(serviceSummary)
        .withInstanceStatsByEnvList(instanceStatsByEnvList)
        .build();
  }

  public static final class Builder {
    private long totalCount;
    private ServiceSummary serviceSummary;
    private List<InstanceStatsByEnvironment> instanceStatsByEnvList;

    private Builder() {}

    public static Builder anInstanceStatsByService() {
      return new Builder();
    }

    public Builder withTotalCount(long totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    public Builder withServiceSummary(ServiceSummary serviceSummary) {
      this.serviceSummary = serviceSummary;
      return this;
    }

    public Builder withInstanceStatsByEnvList(List<InstanceStatsByEnvironment> instanceStatsByEnvList) {
      this.instanceStatsByEnvList = instanceStatsByEnvList;
      return this;
    }

    public Builder but() {
      return anInstanceStatsByService()
          .withTotalCount(totalCount)
          .withInstanceStatsByEnvList(instanceStatsByEnvList)
          .withServiceSummary(serviceSummary);
    }

    public InstanceStatsByService build() {
      InstanceStatsByService instanceStatsByArtifact = new InstanceStatsByService();
      instanceStatsByArtifact.setTotalCount(totalCount);
      instanceStatsByArtifact.setServiceSummary(serviceSummary);
      instanceStatsByArtifact.setInstanceStatsByEnvList(instanceStatsByEnvList);
      return instanceStatsByArtifact;
    }
  }
}