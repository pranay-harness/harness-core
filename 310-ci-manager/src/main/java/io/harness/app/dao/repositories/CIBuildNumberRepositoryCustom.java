package io.harness.app.dao.repositories;

import io.harness.ci.beans.entities.BuildNumber;

public interface CIBuildNumberRepositoryCustom {
  BuildNumber increaseBuildNumber(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
