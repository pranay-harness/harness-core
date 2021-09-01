package io.harness.cdng.k8s.beans;

import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitFetchResponsePassThroughData implements PassThroughData {
  String errorMsg;
  UnitProgressData unitProgressData;
}
