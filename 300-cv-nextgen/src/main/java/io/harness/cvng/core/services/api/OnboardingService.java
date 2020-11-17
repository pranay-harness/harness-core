package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;

public interface OnboardingService {
  OnboardingResponseDTO getOnboardingResponse(String accountId, OnboardingRequestDTO onboardingRequestDTO);
}
