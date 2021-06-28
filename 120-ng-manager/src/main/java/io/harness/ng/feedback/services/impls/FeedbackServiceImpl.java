package io.harness.ng.feedback.services.impls;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.feedback.beans.FeedbackFormDTO;
import io.harness.ng.feedback.entities.FeedbackForm;
import io.harness.ng.feedback.services.FeedbackService;
import io.harness.repositories.feedback.spring.FeedbackFormRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(GTM)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class FeedbackServiceImpl implements FeedbackService {
  private final FeedbackFormRepository feedbackFormRepository;

  @Override
  public Boolean saveFeedback(FeedbackFormDTO feedbackFormDTO) {
    validateFeedback(feedbackFormDTO);
    feedbackFormRepository.save(convertToEntity(feedbackFormDTO));
    return true;
  }

  private void validateFeedback(FeedbackFormDTO feedbackFormDTO) {
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
}
