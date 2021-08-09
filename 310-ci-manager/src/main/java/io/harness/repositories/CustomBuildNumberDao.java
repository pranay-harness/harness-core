package io.harness.repositories;

import io.harness.ci.beans.entities.BuildNumberDetails;

public interface CustomBuildNumberDao {
  BuildNumberDetails increaseBuildNumber(String pipelineIdentifier);
}
