/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;

public interface OnboardingService {
  OnboardingResponseDTO getOnboardingResponse(String accountId, OnboardingRequestDTO onboardingRequestDTO);

  void checkConnectivity(String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier,
      String tracingId, DataSourceType dataSourceType);
}
