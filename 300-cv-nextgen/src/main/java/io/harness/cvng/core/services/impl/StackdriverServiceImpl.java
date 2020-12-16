package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardDetailsRequest;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardRequest;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDetail;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.StackdriverService;
import io.harness.ng.beans.PageResponse;
import io.harness.serializer.JsonUtils;
import io.harness.utils.PageUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StackdriverServiceImpl implements StackdriverService {
  @Inject private OnboardingService onboardingService;
  @Override
  public PageResponse<StackdriverDashboardDTO> listDashboards(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int pageSize, int offset, String filter) {
    DataCollectionRequest request =
        StackdriverDashboardRequest.builder().type(DataCollectionRequestType.STACKDRIVER_DASHBOARD_LIST).build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);

    final Gson gson = new Gson();
    Type type = new TypeToken<List<StackdriverDashboardDTO>>() {}.getType();
    List<StackdriverDashboardDTO> dashboardDTOS = gson.fromJson(JsonUtils.asJson(response.getResult()), type);
    List<StackdriverDashboardDTO> returnList = new ArrayList<>();
    if (isNotEmpty(filter)) {
      returnList = dashboardDTOS.stream()
                       .filter(dashboardDto -> dashboardDto.getName().toLowerCase().contains(filter.toLowerCase()))
                       .collect(Collectors.toList());
    } else {
      returnList = dashboardDTOS;
    }
    return PageUtils.offsetAndLimit(returnList, offset, pageSize);
  }

  @Override
  public List<StackdriverDashboardDetail> getDashboardDetails(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String path) {
    DataCollectionRequest request = StackdriverDashboardDetailsRequest.builder()
                                        .type(DataCollectionRequestType.STACKDRIVER_DASHBOARD_GET)
                                        .path(path)
                                        .build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);

    final Gson gson = new Gson();
    Type type = new TypeToken<List<StackdriverDashboardDetail>>() {}.getType();
    List<StackdriverDashboardDetail> dashboardDetails = gson.fromJson(JsonUtils.asJson(response.getResult()), type);
    dashboardDetails.forEach(StackdriverDashboardDetail::transformDataSets);
    return dashboardDetails;
  }
}
