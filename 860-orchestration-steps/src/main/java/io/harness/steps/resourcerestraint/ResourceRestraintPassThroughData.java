package io.harness.steps.resourcerestraint;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@RecasterAlias("io.harness.steps.resourcerestraint.ResourceRestraintPassThroughData")
public class ResourceRestraintPassThroughData implements PassThroughData {
  String consumerId;
}
