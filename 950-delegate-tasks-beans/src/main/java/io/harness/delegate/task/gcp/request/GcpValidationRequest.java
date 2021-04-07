package io.harness.delegate.task.gcp.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class GcpValidationRequest extends GcpRequest {}