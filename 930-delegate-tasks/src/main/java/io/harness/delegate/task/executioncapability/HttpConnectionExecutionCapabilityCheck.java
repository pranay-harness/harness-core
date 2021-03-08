package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.network.Http;

public class HttpConnectionExecutionCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) delegateCapability;
    boolean valid = Http.connectableHttpUrl(httpConnectionExecutionCapability.fetchCapabilityBasis());
    return CapabilityResponse.builder().delegateCapability(httpConnectionExecutionCapability).validated(valid).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.HTTP_CONNECTION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    return builder
        .permissionResult(Http.connectableHttpUrl(parameters.getHttpConnectionParameters().getUrl())
                ? PermissionResult.ALLOWED
                : PermissionResult.DENIED)
        .build();
  }
}
