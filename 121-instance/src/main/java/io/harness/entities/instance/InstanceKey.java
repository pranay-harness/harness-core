package io.harness.entities.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@OwnedBy(HarnessTeam.DX)
public abstract class InstanceKey {}
