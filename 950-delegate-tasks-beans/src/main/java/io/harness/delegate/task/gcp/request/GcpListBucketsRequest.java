package io.harness.delegate.task.gcp.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@OwnedBy(CDP)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GcpListBucketsRequest extends GcpRequest {}
