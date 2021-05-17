package io.harness.models.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public final class InstanceSyncConstants {
  public static final String HARNESS_ACCOUNT_ID = "accountId";
  public static final String HARNESS_APPLICATION_ID = "applicationId";
  public static final String HARNESS_ENV_ID = "environmentId";
  public static final String INFRASTRUCTURE_MAPPING_ID = "infrastructureMappingId";
  public static final String INFRASTRUCTURE_MAPPING_DETAILS = "infrastructureMappingDetails";
  public static final String NAMESPACE = "namespace";
  public static final String RELEASE_NAME = "releaseName";
  public static final String CONTAINER_SERVICE_NAME = "containerSvcName";
  public static final String CONTAINER_TYPE = "containerType";
  public static final int TIMEOUT_SECONDS = 600;
  public static final int INTERVAL_MINUTES = 10;
  public static final int VALIDATION_TIMEOUT_MINUTES = 2;
}
