package io.harness.instrumentation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public class ServiceInstrumentationConstants {
  public static String ACTIVE_SERVICES_COUNT_EVENT = "cd_active_services_count";
  public static String ACTIVE_SERVICES_COUNT = "count";
  public static String ACTIVE_SERVICES_ACCOUNT_ID = "account_id";
  public static String ACTIVE_SERVICES_ACCOUNT_NAME = "account_name";
  public static String ACTIVE_SERVICES_PROJECT_ID = "project_id";
  public static String ACTIVE_SERVICES_ORG_ID = "organization_id";
}
