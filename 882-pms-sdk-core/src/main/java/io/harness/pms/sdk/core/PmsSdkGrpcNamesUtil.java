package io.harness.pms.sdk.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkGrpcNamesUtil {
  public static String getPmsSdkServiceManagerName(String serviceName) {
    return String.format("pmsSDKServiceManager-%s", serviceName);
  }

  public static String getPmsClientStubAnnotation(String serviceName) {
    return String.format("pmsSdkClientStub-%s", serviceName);
  }

  public static String getPmsSdkServiceName(String serviceName) {
    return String.format("pms-sdk-grpc-service-%s", serviceName);
  }
}
