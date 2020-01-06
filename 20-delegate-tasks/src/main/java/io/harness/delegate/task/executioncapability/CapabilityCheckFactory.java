package io.harness.delegate.task.executioncapability;

import static io.harness.delegate.beans.executioncapability.CapabilityType.ALWAYS_TRUE;
import static io.harness.delegate.beans.executioncapability.CapabilityType.AWS_REGION;
import static io.harness.delegate.beans.executioncapability.CapabilityType.CHART_MUSEUM;
import static io.harness.delegate.beans.executioncapability.CapabilityType.HELM;
import static io.harness.delegate.beans.executioncapability.CapabilityType.HTTP;
import static io.harness.delegate.beans.executioncapability.CapabilityType.PROCESS_EXECUTOR;
import static io.harness.delegate.beans.executioncapability.CapabilityType.SOCKET;
import static io.harness.delegate.beans.executioncapability.CapabilityType.SYSTEM_ENV;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityType;

@Singleton
public class CapabilityCheckFactory {
  @Inject SocketConnectivityCapabilityCheck socketConnectivityCapabilityCheck;
  @Inject IgnoreValidationCapabilityCheck ignoreValidationCapabilityCheck;
  @Inject ProcessExecutorCapabilityCheck processExecutorCapabilityCheck;
  @Inject AwsRegionCapabilityCheck awsRegionCapabilityCheck;
  @Inject SystemEnvCapabilityCheck systemEnvCapabilityCheck;
  @Inject HttpConnectionExecutionCapabilityCheck httpConnectionExecutionCapabilityCheck;
  @Inject HelmCapabilityCheck helmCapabilityCheck;
  @Inject ChartMuseumCapabilityCheck chartMuseumCapabilityCheck;

  public CapabilityCheck obtainCapabilityCheck(CapabilityType capabilityCheckType) {
    if (SOCKET == capabilityCheckType) {
      return socketConnectivityCapabilityCheck;
    }

    if (ALWAYS_TRUE == capabilityCheckType) {
      return ignoreValidationCapabilityCheck;
    }

    if (PROCESS_EXECUTOR == capabilityCheckType) {
      return processExecutorCapabilityCheck;
    }

    if (AWS_REGION == capabilityCheckType) {
      return awsRegionCapabilityCheck;
    }

    if (SYSTEM_ENV == capabilityCheckType) {
      return systemEnvCapabilityCheck;
    }

    if (HTTP == capabilityCheckType) {
      return httpConnectionExecutionCapabilityCheck;
    }

    if (HELM == capabilityCheckType) {
      return helmCapabilityCheck;
    }

    if (CHART_MUSEUM == capabilityCheckType) {
      return chartMuseumCapabilityCheck;
    }

    return null;
  }
}
