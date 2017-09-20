package software.wings.service.impl.instance.sync.request;

import lombok.Data;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
public class ContainerSyncRequest {
  private ContainerFilter filter;

  public static final class Builder {
    private ContainerFilter filter;

    private Builder() {}

    public static Builder aContainerSyncRequest() {
      return new Builder();
    }

    public Builder withFilter(ContainerFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder but() {
      return aContainerSyncRequest().withFilter(filter);
    }

    public ContainerSyncRequest build() {
      ContainerSyncRequest containerSyncRequest = new ContainerSyncRequest();
      containerSyncRequest.setFilter(filter);
      return containerSyncRequest;
    }
  }
}
