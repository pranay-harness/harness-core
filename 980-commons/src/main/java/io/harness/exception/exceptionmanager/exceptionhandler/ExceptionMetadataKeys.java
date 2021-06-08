package io.harness.exception.exceptionmanager.exceptionhandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public enum ExceptionMetadataKeys {
  IMAGE_NAME,
  IMAGE_TAG,
  URL,
  CONNECTOR,
  REGION,
  GIT_REPO_URL
}
