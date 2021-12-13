package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.customhealth.CustomHealthFetchSampleDataRequest;
import io.harness.cvng.core.beans.CustomHealthSampleDataRequest;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.services.api.CustomHealthService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.serializer.JsonUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.Map;

public class CustomHealthServiceImpl implements CustomHealthService {
  @Inject OnboardingService onboardingService;

  @Override
  public Map<String, Object> fetchSampleData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String tracingId, CustomHealthSampleDataRequest request) {
    CustomHealthFetchSampleDataRequest customHealthSampleDataRequest =
        CustomHealthFetchSampleDataRequest.builder()
            .type(DataCollectionRequestType.CUSTOM_HEALTH_SAMPLE_DATA)
            .body(request.getBody())
            .method(request.getMethod())
            .urlPath(request.getUrlPath())
            .requestTimestampPlaceholderAndValues(request.getRequestTimestampPlaceholderAndValues())
            .build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(customHealthSampleDataRequest)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .tracingId(tracingId)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<Map<String, Object>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }
}
