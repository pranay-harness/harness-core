package io.harness.core.ci.services;

import io.harness.ci.beans.entities.BuildNumberDetails;

public interface BuildNumberService {
  BuildNumberDetails increaseBuildNumber(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
