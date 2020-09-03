package io.harness.core.ci.services;

import io.harness.ci.beans.entities.BuildNumber;

public interface BuildNumberService {
  BuildNumber increaseBuildNumber(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
