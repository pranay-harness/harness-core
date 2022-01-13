/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.genericgitconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomCommitAttributes {
  public static final String COMMIT_MSG = "Harness IO Git Sync.";
  public static final String HARNESS_IO_KEY_ = "Harness.io";
  public static final String HARNESS_SUPPORT_EMAIL_KEY = "support@harness.io";
  String authorName;
  String authorEmail;
  String commitMessage;
}
