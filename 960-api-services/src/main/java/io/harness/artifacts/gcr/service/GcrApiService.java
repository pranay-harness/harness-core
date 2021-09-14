/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.artifacts.gcr.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.gcr.beans.GcrInternalConfig;

import java.util.List;

/**
 * Created by brett on 8/2/17
 */
@OwnedBy(CDC)
public interface GcrApiService {
  int MAX_NO_OF_TAGS_PER_IMAGE = 10000;
  /**
   * Gets builds.
   *
   * @param gcpConfig         the gcp cloud provider config
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetailsInternal> getBuilds(GcrInternalConfig gcpConfig, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param gcpConfig the gcr config
   * @return the last successful build`
   */
  BuildDetailsInternal getLastSuccessfulBuild(GcrInternalConfig gcpConfig, String imageName);

  /**
   * Validates the Image
   *
   * @param gcpConfig
   */
  boolean verifyImageName(GcrInternalConfig gcpConfig, String imageName);

  /**
   * Validate the credentials
   *
   * @param gcpConfig
   * @return
   */
  boolean validateCredentials(GcrInternalConfig gcpConfig, String imageName);

  /**
   * Gets the last successful build with input as tag regex.
   */
  BuildDetailsInternal getLastSuccessfulBuildFromRegex(
      GcrInternalConfig gcrInternalConfig, String imageName, String tagRegex);

  /**
   * Validates the Image Tag
   */
  BuildDetailsInternal verifyBuildNumber(GcrInternalConfig gcrInternalConfig, String imageName, String tag);
}
