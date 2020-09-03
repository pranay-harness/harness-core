package io.harness.app.dao.repositories;

import io.harness.ci.beans.entities.BuildNumber;

public interface CustomBuildNumberDao { BuildNumber increaseBuildNumber(String pipelineIdentifier); }
