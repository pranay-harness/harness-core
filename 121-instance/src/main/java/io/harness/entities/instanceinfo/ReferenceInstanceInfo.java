package io.harness.entities.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@io.harness.annotations.dev.OwnedBy(HarnessTeam.DX)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
// Created for reference, either change its name before modifying or create a new instance info
public class ReferenceInstanceInfo extends InstanceInfo {
  String podName;
}
