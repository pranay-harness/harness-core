package io.harness.pms.dashboards;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.Dashboard.DashboardPipelineExecutionInfo;
import io.harness.pms.Dashboard.DashboardPipelineHealthInfo;
import io.harness.pms.Dashboard.PipelineDashboardOverviewResource;
import io.harness.pms.pipeline.service.PipelineDashboardService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineDashboardOverviewResourceTest {
  @Mock PipelineDashboardService pipelineDashboardService;

  @InjectMocks PipelineDashboardOverviewResource pipelineDashboardOverviewResource;
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "pipId";
  private final long HOUR_IN_MS = 60 * 60 * 1000;
  private final long DAY_IN_MS = 24 * HOUR_IN_MS;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetPipelineHealth() {
    long startInterval = 2 * DAY_IN_MS;
    long endInterval = 5 * DAY_IN_MS;
    long previousStartInterval = startInterval - (endInterval - startInterval + DAY_IN_MS);
    DashboardPipelineHealthInfo dashboardPipelineHealthInfo = DashboardPipelineHealthInfo.builder().build();
    doReturn(dashboardPipelineHealthInfo)
        .when(pipelineDashboardService)
        .getDashboardPipelineHealthInfo(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, startInterval,
            endInterval, previousStartInterval, "cd");
    ResponseDTO<DashboardPipelineHealthInfo> responseDTO = pipelineDashboardOverviewResource.getPipelinedHealth(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, "cd", startInterval, endInterval);
    assertThat(responseDTO).isNotNull();
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetPipelineExecution() {
    DashboardPipelineExecutionInfo dashboardPipelineExecutionInfo = DashboardPipelineExecutionInfo.builder().build();
    doReturn(dashboardPipelineExecutionInfo)
        .when(pipelineDashboardService)
        .getDashboardPipelineExecutionInfo(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, 100L, 200L, "cd");
    ResponseDTO<DashboardPipelineExecutionInfo> responseDTO = pipelineDashboardOverviewResource.getPipelineExecution(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, "cd", 100L, 200L);
    assertThat(responseDTO).isNotNull();
  }
}
