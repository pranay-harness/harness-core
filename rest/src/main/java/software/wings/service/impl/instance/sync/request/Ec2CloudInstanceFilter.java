package software.wings.service.impl.instance.sync.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Ec2CloudInstanceFilter extends CloudInstanceFilter {
  private AwsConfig awsConfig;

  public static final class Builder {
    private AwsConfig awsConfig;
    private List<String> hostNames;

    private Builder() {}

    public static Builder anEc2CloudInstanceFilter() {
      return new Builder();
    }

    public Builder withAwsConfig(AwsConfig awsConfig) {
      this.awsConfig = awsConfig;
      return this;
    }

    public Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public Builder but() {
      return anEc2CloudInstanceFilter().withAwsConfig(awsConfig).withHostNames(hostNames);
    }

    public Ec2CloudInstanceFilter build() {
      Ec2CloudInstanceFilter ec2CloudInstanceFilter = new Ec2CloudInstanceFilter();
      ec2CloudInstanceFilter.setAwsConfig(awsConfig);
      ec2CloudInstanceFilter.hostNames = this.hostNames;
      return ec2CloudInstanceFilter;
    }
  }
}
