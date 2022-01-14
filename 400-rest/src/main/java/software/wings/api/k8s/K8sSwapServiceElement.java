package software.wings.api.k8s;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sSwapServiceElement {
  private boolean swapDone;
}
