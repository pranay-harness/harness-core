package io.harness.repositories;

import io.harness.ci.beans.entities.BuildNumberDetails;

public interface CIBuildNumberRepositoryCustom {
  BuildNumberDetails increaseBuildNumber(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
