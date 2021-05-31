package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.billing.InstanceCategory;
import io.harness.ccm.commons.constants.CloudProvider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class K8sServiceProvider {
  String region;
  String instanceFamily;
  CloudProvider cloudProvider;
  InstanceCategory instanceCategory;
}
