package software.wings.service.impl.instance.sync.request;

import lombok.Data;

@Data
public abstract class ContainerFilter {
  protected String clusterName;
}
