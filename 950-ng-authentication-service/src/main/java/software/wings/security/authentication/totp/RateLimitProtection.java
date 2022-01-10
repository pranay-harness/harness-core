/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class RateLimitProtection {
  @Builder.Default List<Long> incorrectAttemptTimestamps = new ArrayList<>();
  long lastNotificationSentToUserAt;
  long lastNotificationSentToSecOpsAt;
  int totalIncorrectAttempts;
}
