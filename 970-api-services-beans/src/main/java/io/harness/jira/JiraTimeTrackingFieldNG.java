/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraTimeTrackingFieldNG {
  String originalEstimate;
  String remainingEstimate;

  public JiraTimeTrackingFieldNG(String originalEstimate, String remainingEstimate) {
    setOriginalEstimate(originalEstimate);
    setRemainingEstimate(remainingEstimate);
  }

  public void setOriginalEstimate(String originalEstimate) {
    if (EmptyPredicate.isNotEmpty(originalEstimate)) {
      this.originalEstimate = originalEstimate;
    }
  }

  public void setRemainingEstimate(String remainingEstimate) {
    if (EmptyPredicate.isNotEmpty(remainingEstimate)) {
      this.remainingEstimate = remainingEstimate;
    }
  }

  public void addToFields(Map<String, Object> fields) {
    // Returns 2 fields - "Original Estimate", "Remaining Estimate"
    if (originalEstimate != null) {
      fields.put(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, originalEstimate);
    }
    if (remainingEstimate != null) {
      fields.put(JiraConstantsNG.REMAINING_ESTIMATE_NAME, remainingEstimate);
    }
  }
}
