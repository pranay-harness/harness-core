/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Replace secret values with mask for safe display
 */
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ActivityBasedLogSanitizer extends LogSanitizer {
  private final String activityId;
  private final Set<String> secretLines;

  public ActivityBasedLogSanitizer(String activityId, Set<String> secrets) {
    this.activityId = activityId;
    secretLines = calculateSecretLines(secrets);
  }

  /**
   * Replace secret values in {@code log} with mask for safe display
   * @param activityId The id to match to the set of secrets for this sanitizer
   * @param message The text that may contain secret values
   * @return text with secrets replaced by a mask
   */
  @Override
  public String sanitizeLog(String activityId, String message) {
    if (StringUtils.equals(activityId, this.activityId)) {
      if (isEmpty(secretLines)) {
        return message;
      }
      return sanitizeLogInternal(message, secretLines);
    }
    return message;
  }
}
