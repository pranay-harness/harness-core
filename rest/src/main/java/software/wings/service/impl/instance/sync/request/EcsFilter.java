package software.wings.service.impl.instance.sync.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcsFilter extends ContainerFilter {
  private List<String> serviceNameList;
  private String awsComputeProviderId;
  private String region;

  public static final class Builder {
    protected String clusterName;
    private List<String> serviceNameList;
    private String awsComputeProviderId;
    private String region;

    private Builder() {}

    public static Builder anEcsFilter() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withServiceNameList(List<String> serviceNameList) {
      this.serviceNameList = serviceNameList;
      return this;
    }

    public Builder withAwsComputeProviderId(String awsComputeProviderId) {
      this.awsComputeProviderId = awsComputeProviderId;
      return this;
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder but() {
      return anEcsFilter()
          .withClusterName(clusterName)
          .withServiceNameList(serviceNameList)
          .withAwsComputeProviderId(awsComputeProviderId)
          .withRegion(region);
    }

    public EcsFilter build() {
      EcsFilter ecsFilter = new EcsFilter();
      ecsFilter.setClusterName(clusterName);
      ecsFilter.setServiceNameList(serviceNameList);
      ecsFilter.setAwsComputeProviderId(awsComputeProviderId);
      ecsFilter.setRegion(region);
      return ecsFilter;
    }
  }
}
