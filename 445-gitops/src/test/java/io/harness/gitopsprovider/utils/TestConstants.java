package io.harness.gitopsprovider.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.GITOPS)
public class TestConstants {
  public static final String PROJECT_ID = "projectId";
  public static final String ORG_ID = "orgId";
  public static final String DESCRIPTION = "description";
  public static final String IDENTIFIER = "identifier";
  public static final String ARGO_NAME = "argo_name";
  public static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  public static final String ADAPTER_URL = "https://url";
}
