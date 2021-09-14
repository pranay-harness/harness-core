/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.pagerduty.PagerDutyServiceDetail;
import io.harness.cvng.beans.pagerduty.PagerDutyServicesRequest;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.PagerDutyService;
import io.harness.serializer.JsonUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CV)
@Slf4j
public class PagerDutyServiceImpl implements PagerDutyService {
  @Inject private OnboardingService onboardingService;

  @Override
  public List<PagerDutyServiceDetail> getPagerDutyServices(
      ProjectParams projectParams, String connectorIdentifier, String requestGuid) {
    DataCollectionRequest request = PagerDutyServicesRequest.builder().build();
    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(projectParams.getAccountIdentifier())
                                                    .tracingId(requestGuid)
                                                    .orgIdentifier(projectParams.getOrgIdentifier())
                                                    .projectIdentifier(projectParams.getProjectIdentifier())
                                                    .build();

    OnboardingResponseDTO response =
        onboardingService.getOnboardingResponse(projectParams.getAccountIdentifier(), onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<List<PagerDutyServiceDetail>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }
}
