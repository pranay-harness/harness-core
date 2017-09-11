package software.wings.service.impl.instance.sync.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.GcpConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GcpCloudInstanceFilter extends CloudInstanceFilter {
  private GcpConfig gcpConfig;

  public static final class Builder {
    protected List<String> hostNames;
    private GcpConfig gcpConfig;

    private Builder() {}

    public static Builder aGcpCloudInstanceFilter() {
      return new Builder();
    }

    public Builder withGcpConfig(GcpConfig gcpConfig) {
      this.gcpConfig = gcpConfig;
      return this;
    }

    public Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public Builder but() {
      return aGcpCloudInstanceFilter().withGcpConfig(gcpConfig).withHostNames(hostNames);
    }

    public GcpCloudInstanceFilter build() {
      GcpCloudInstanceFilter gcpCloudInstanceFilter = new GcpCloudInstanceFilter();
      gcpCloudInstanceFilter.setGcpConfig(gcpConfig);
      gcpCloudInstanceFilter.hostNames = this.hostNames;
      return gcpCloudInstanceFilter;
    }
  }
}
