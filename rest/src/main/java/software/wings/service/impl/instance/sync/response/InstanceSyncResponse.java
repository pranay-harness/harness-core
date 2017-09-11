package software.wings.service.impl.instance.sync.response;

import lombok.Data;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;

import java.util.List;

/**
 * @author rktummala on 09/02/17
 */
@Data
public class InstanceSyncResponse {
  private List<ContainerInfo> containerInfoList;

  public static final class Builder {
    private List<ContainerInfo> containerInfoList;

    private Builder() {}

    public static Builder anInstanceSyncResponse() {
      return new Builder();
    }

    public Builder withContainerInfoList(List<ContainerInfo> containerInfoList) {
      this.containerInfoList = containerInfoList;
      return this;
    }

    public Builder but() {
      return anInstanceSyncResponse().withContainerInfoList(containerInfoList);
    }

    public InstanceSyncResponse build() {
      InstanceSyncResponse instanceSyncResponse = new InstanceSyncResponse();
      instanceSyncResponse.setContainerInfoList(containerInfoList);
      return instanceSyncResponse;
    }
  }
}
