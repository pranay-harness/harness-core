/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static lombok.AccessLevel.PUBLIC;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.FieldDefaults;

@OwnedBy(CDP)
@FieldDefaults(level = PUBLIC)
public class HelmTestConstants {
  static String ACCOUNT_ID = "ACCOUNT_ID";
  static String APP_ID = "APP_ID";
  static String MANIFEST_ID = "MANIFEST_ID";
  static String SERVICE_ID = "SERVICE_ID";

  static long LONG_TIMEOUT_INTERVAL = 60 * 1000L;
}
