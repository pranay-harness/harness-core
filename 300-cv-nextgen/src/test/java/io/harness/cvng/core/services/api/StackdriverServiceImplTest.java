package io.harness.cvng.core.services.api;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.services.impl.StackdriverServiceImpl;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StackdriverServiceImplTest {
  @Mock private OnboardingService onboardingService;
  @InjectMocks private StackdriverService stackdriverService = new StackdriverServiceImpl();
  private String accountId;
  private String connectorIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  @Captor private ArgumentCaptor<OnboardingRequestDTO> requestCaptor;

  @Before
  public void setup() {
    accountId = generateUuid();
    connectorIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetDashboardList() {
    when(onboardingService.getOnboardingResponse(eq(accountId), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(getDashboardList(8)).build());
    PageResponse<StackdriverDashboardDTO> dashboardList = stackdriverService.listDashboards(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, 10, 0, null);

    verify(onboardingService).getOnboardingResponse(eq(accountId), requestCaptor.capture());

    OnboardingRequestDTO onboardingRequestDTO = requestCaptor.getValue();
    assertThat(onboardingRequestDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(onboardingRequestDTO.getConnectorIdentifier()).isEqualTo(connectorIdentifier);
    assertThat(onboardingRequestDTO.getAccountId()).isEqualTo(accountId);
    assertThat(onboardingRequestDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(onboardingRequestDTO.getDataCollectionRequest()).isNotNull();

    DataCollectionRequest request = onboardingRequestDTO.getDataCollectionRequest();
    assertThat(request.getType().name()).isEqualTo(DataCollectionRequestType.STACKDRIVER_DASHBOARD_LIST.name());
    assertThat(dashboardList).isNotNull();
    assertThat(dashboardList.getContent().size()).isEqualTo(8);
  }

  private List<StackdriverDashboardDTO> getDashboardList(int count) {
    List<StackdriverDashboardDTO> dashboardDTOList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      dashboardDTOList.add(
          StackdriverDashboardDTO.builder().name("dashboard-" + i).path("dashboardPath - " + i).build());
    }
    return dashboardDTOList;
  }
}
