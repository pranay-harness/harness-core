/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(DX)
public class GitSyncConstants {
  public static final String DEFAULT = "__default__";
  public static final String COMMIT_MSG = "Harness.io Git Sync";
  public static final String DEFAULT_USER_NAME = "Harness_user";
  public static final String DEFAULT_USER_EMAIL_ID = "user@harness.io";
  public static final String FOLDER_PATH = ".harness/";
}
