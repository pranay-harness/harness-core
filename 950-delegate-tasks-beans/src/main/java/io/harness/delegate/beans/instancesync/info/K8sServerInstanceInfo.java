package io.harness.delegate.beans.instancesync.info;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.k8s.model.K8sContainer;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class K8sServerInstanceInfo extends ServerInstanceInfo {
  private String name;
  private String namespace;
  private String releaseName;
  private String podIP;
  private String blueGreenColor;
  private List<K8sContainer> containerList;
}
