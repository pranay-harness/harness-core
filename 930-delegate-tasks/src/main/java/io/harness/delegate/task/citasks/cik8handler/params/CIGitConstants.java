/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler.params;

/**
 * Class to store constants for creating a container to clone git repositories.
 */

public final class CIGitConstants {
  private CIGitConstants() {
    // do nothing
  }

  public static final String GIT_USERNAME_ENV_VAR = "GIT_USERNAME"; // Environment variable key to store GIT username
  public static final String GIT_PASS_ENV_VAR = "GIT_PASSWORD"; // Environment variable key to store GIT password
  public static final String GIT_CLONE_CONTAINER_NAME = "git-clone";
  // TODO: (vistaar) Change this to harness/ci-logger when access is approved
  public static final String GIT_CLONE_IMAGE_NAME = "alpine/git"; // Container image used for cloning GIT repositories
  public static final String GIT_CLONE_IMAGE_TAG = "1.0.10"; // Image version used for cloning GIT repositories

  public static final String LOG_SERVICE_ENDPOINT_VARIABLE = "LOG_SERVICE_ENDPOINT";
  public static final String LOG_SERVICE_ENDPOINT_VARIABLE_VALUE = "http://34.122.43.109:80";

  public static final String GIT_SSH_VOL_NAME = "git-secret"; // Name of volume that stores GIT SSH secret keys
  public static final String PATH_DELIMITER = "/";
  public static final String GIT_SSH_VOL_MOUNT_PATH =
      PATH_DELIMITER.concat("etc/git-secret"); // Mount path for GIT SSH secret key volume
  public static final Integer GIT_SSH_VOL_DEFAULT_MODE = 256; // 0400 access to SSH key
  public static final String STEP_EXEC_VOLUME_MOUNT_PATH =
      PATH_DELIMITER.concat("harness-step-exec"); // Mount path for step executor volume
}
