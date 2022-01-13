/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.feedback.services.impls;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.feedback.beans.FeedbackFormDTO;
import io.harness.ng.feedback.entities.FeedbackForm;
import io.harness.ng.feedback.services.FeedbackService;
import io.harness.repositories.feedback.spring.FeedbackFormRepository;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(GTM)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class FeedbackServiceImpl implements FeedbackService {
  private final FeedbackFormRepository feedbackFormRepository;
  private final TelemetryReporter telemetryReporter;

  private static final String USER_FEEDBACK = "USER_FEEDBACK";

  @Override
  public Boolean saveFeedback(FeedbackFormDTO feedbackFormDTO) {
    validateFeedback(feedbackFormDTO);
    sendFeedBackTelemetry(feedbackFormDTO);

    feedbackFormRepository.save(convertToEntity(feedbackFormDTO));
    return true;
  }

  private void validateFeedback(FeedbackFormDTO feedbackFormDTO) {
    if (feedbackFormDTO.getAccountId() == null || feedbackFormDTO.getScore() == null
        || feedbackFormDTO.getEmail() == null || feedbackFormDTO.getModuleType() == null) {
      throw new InvalidRequestException("Invalid feedback request");
    }
    if (feedbackFormDTO.getSuggestion() == null) {
      feedbackFormDTO.setSuggestion("");
    }

    if (feedbackFormDTO.getSuggestion().length() > 500) {
      throw new InvalidRequestException("Feedback suggestion can't exceed 500 characters");
    }
  }

  private FeedbackForm convertToEntity(FeedbackFormDTO dto) {
    return FeedbackForm.builder()
        .accountIdentifier(dto.getAccountId())
        .email(dto.getEmail())
        .moduleType(dto.getModuleType())
        .score(dto.getScore())
        .suggestion(dto.getSuggestion())
        .build();
  }

  private void sendFeedBackTelemetry(FeedbackFormDTO dto) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("accountId", dto.getAccountId());
    properties.put("email", dto.getEmail());
    properties.put("module", dto.getModuleType());
    properties.put("score", dto.getScore());
    properties.put("suggestion", dto.getSuggestion());
    telemetryReporter.sendTrackEvent(USER_FEEDBACK, properties,
        ImmutableMap.<Destination, Boolean>builder()
            .put(Destination.AMPLITUDE, true)
            .put(Destination.ALL, false)
            .build(),
        Category.SIGN_UP);
  }
}
