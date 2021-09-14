/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.feedback.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.feedback.beans.FeedbackFormDTO;

@OwnedBy(GTM)
public interface FeedbackService {
  Boolean saveFeedback(FeedbackFormDTO dto);
}
