package software.wings.service.impl.instance.sync.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerFilter extends InstanceFilter {
  protected String clusterName;
}
