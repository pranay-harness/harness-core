package software.wings.service.impl.instance.sync.request;

import lombok.Data;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
public class InstanceSyncRequest {
  private InstanceFilter filter;

  public static final class Builder {
    private InstanceFilter filter;

    private Builder() {}

    public static Builder anInstanceSyncRequest() {
      return new Builder();
    }

    public Builder withFilter(InstanceFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder but() {
      return anInstanceSyncRequest().withFilter(filter);
    }

    public InstanceSyncRequest build() {
      InstanceSyncRequest instanceSyncRequest = new InstanceSyncRequest();
      instanceSyncRequest.setFilter(filter);
      return instanceSyncRequest;
    }
  }
}
