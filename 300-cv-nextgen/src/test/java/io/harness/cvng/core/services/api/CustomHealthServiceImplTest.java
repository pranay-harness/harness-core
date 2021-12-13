package io.harness.cvng.core.services.api;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.customhealth.CustomHealthFetchSampleDataRequest;
import io.harness.cvng.core.beans.CustomHealthSampleDataRequest;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class CustomHealthServiceImplTest extends CvNextGenTestBase {
  @Mock OnboardingService onboardingServiceMock;
  @Inject CustomHealthService customHealthService;

  String accountId = "1234_accountId";
  String connectorIdentifier = "1234_connectorIdentifer";
  String orgIdentifier = "1234_orgIdentifier";
  String projectIdentifier = "1234_projectIdentifier";
  String tracingId = "1234_tracingId";

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(customHealthService, "onboardingService", onboardingServiceMock, true);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testFetchSampleData() {
    String requestBody = "postBody";
    CustomHealthMethod requestMethod = CustomHealthMethod.POST;
    String urlPath = "asdasd?asdad";
    Map<String, String> timestampPlaceholderValues = new HashMap<>();

    CustomHealthFetchSampleDataRequest customHealthSampleDataRequest =
        CustomHealthFetchSampleDataRequest.builder()
            .type(DataCollectionRequestType.CUSTOM_HEALTH_SAMPLE_DATA)
            .body(requestBody)
            .method(requestMethod)
            .urlPath(urlPath)
            .requestTimestampPlaceholderAndValues(timestampPlaceholderValues)
            .build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(customHealthSampleDataRequest)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .tracingId(tracingId)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    CustomHealthSampleDataRequest request = CustomHealthSampleDataRequest.builder()
                                                .body(requestBody)
                                                .method(CustomHealthMethod.POST)
                                                .urlPath(urlPath)
                                                .requestTimestampPlaceholderAndValues(timestampPlaceholderValues)
                                                .build();

    OnboardingResponseDTO responseDTO = OnboardingResponseDTO.builder().result(new HashMap<>()).build();
    when(onboardingServiceMock.getOnboardingResponse(accountId, onboardingRequestDTO)).thenReturn(responseDTO);
    assertThat(customHealthService.fetchSampleData(
                   accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId, request))
        .isEqualTo(new HashMap<>());
  }
}
