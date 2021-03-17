package io.harness.delegate.task.gcp.request;

import io.harness.delegate.task.TaskParameters;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GcpValidationRequest extends GcpRequest implements TaskParameters {}