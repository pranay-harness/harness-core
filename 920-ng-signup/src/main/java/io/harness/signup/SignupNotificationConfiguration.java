/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.signup;

import io.harness.signup.notification.EmailInfo;
import io.harness.signup.notification.EmailType;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupNotificationConfiguration {
  private String projectId;
  private String bucketName;
  private Map<EmailType, EmailInfo> templates;
  private int expireDurationInMinutes;
}
