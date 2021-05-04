package io.harness.pms.dashboards;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.Dashboard.DashboardPipelineExecutionInfo;
import io.harness.pms.Dashboard.DashboardPipelineHealthInfo;
import io.harness.pms.Dashboard.PipelineCountInfo;
import io.harness.pms.Dashboard.PipelineExecutionInfo;
import io.harness.pms.Dashboard.StatusAndTime;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.service.PipelineDashboardServiceImpl;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.PIPELINE)
public class DashboardPipelineHealthAndExecutionInfoTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @InjectMocks @Spy private PipelineDashboardServiceImpl pipelineDashboardService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDashboardPipelineHealthInfoTest() {
    // this test tests only total and success components of Executions  and duration part of mean and median under
    // DashboardPipelineHealthInfo

    String startInterval = "2021-04-01";
    String endInterval = "2021-04-30";
    String previousStartInterval = "2021-03-01";

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name());
    List<String> time = Arrays.asList("2021-04-05 17:37:45.383", "2021-04-21 17:37:45.383", "2021-04-30 0:0:0",
        "2021-03-31 0.0.0", "2021-04-01 0.0.0", "2021-03-25 0:0:0", "2021-03-25 23:59:999");
    StatusAndTime statusAndTime = StatusAndTime.builder().status(status).time(time).build();

    doReturn(statusAndTime).when(pipelineDashboardService).queryCalculatorForStatusAndTime(anyString());
    doReturn(0L).when(pipelineDashboardService).queryCalculatorMean(anyString());
    doReturn(0L).when(pipelineDashboardService).queryCalculatorMedian(anyString());

    DashboardPipelineHealthInfo dashboardPipelineHealthInfo = pipelineDashboardService.getDashboardPipelineHealthInfo(
        "ac", "or", "pr", "pip", startInterval, endInterval, previousStartInterval, "CI");

    // total count = 4, Previous total count = 3, current success = 3, previous success = 2
    assertThat(dashboardPipelineHealthInfo.getExecutions().getTotal().getCount()).isEqualTo(4);
    assertThat(dashboardPipelineHealthInfo.getExecutions().getTotal().getRate()).isEqualTo((1 / (double) 3) * 100);
    assertThat(dashboardPipelineHealthInfo.getExecutions().getSuccess().getPercent()).isEqualTo((3 / (double) 4) * 100);
    assertThat(dashboardPipelineHealthInfo.getExecutions().getSuccess().getRate())
        .isEqualTo(((3 - 2) / (double) 2) * 100);

    // test for empty status and time
    List<String> emptyStatus = new ArrayList<>();
    List<String> emptyTime = new ArrayList<>();
    doReturn(StatusAndTime.builder().time(emptyTime).status(emptyStatus).build())
        .when(pipelineDashboardService)
        .queryCalculatorForStatusAndTime(anyString());

    DashboardPipelineHealthInfo dashboardPipelineHealthInfoEmptyList =
        pipelineDashboardService.getDashboardPipelineHealthInfo(
            "ac", "or", "pr", "pip", "2021-04-01", "2021-04-30", "2021-03-01", "CI");
    assertThat(dashboardPipelineHealthInfoEmptyList.getExecutions().getTotal().getCount()).isEqualTo(0);
    assertThat(dashboardPipelineHealthInfoEmptyList.getExecutions().getTotal().getRate()).isEqualTo(0.0);
    assertThat(dashboardPipelineHealthInfoEmptyList.getExecutions().getSuccess().getPercent()).isEqualTo(0.0);
    assertThat(dashboardPipelineHealthInfoEmptyList.getExecutions().getSuccess().getRate()).isEqualTo(0.0);

    // Mean And Median duration test
    String table = "pipeline_execution_summary_ci";

    // currentMean
    String queryCurrentMean =
        pipelineDashboardService.queryBuilderMean("ac", "or", "pr", "pip", startInterval, "2021-05-01", table);
    doReturn(100L).when(pipelineDashboardService).queryCalculatorMean(queryCurrentMean);

    // currentMedian
    String queryCurrentMedian =
        pipelineDashboardService.queryBuilderMedian("ac", "or", "pr", "pip", startInterval, "2021-05-01", table);
    doReturn(150L).when(pipelineDashboardService).queryCalculatorMedian(queryCurrentMedian);

    // PreviousMean
    String queryPreviousMean =
        pipelineDashboardService.queryBuilderMean("ac", "or", "pr", "pip", previousStartInterval, startInterval, table);
    doReturn(40L).when(pipelineDashboardService).queryCalculatorMean(queryPreviousMean);

    // previousMedian
    String queryPreviousMedian = pipelineDashboardService.queryBuilderMedian(
        "ac", "or", "pr", "pip", previousStartInterval, startInterval, table);
    doReturn(180L).when(pipelineDashboardService).queryCalculatorMedian(queryPreviousMedian);

    DashboardPipelineHealthInfo dashboardPipelineHealthInfoMeanMedian =
        pipelineDashboardService.getDashboardPipelineHealthInfo(
            "ac", "or", "pr", "pip", "2021-04-01", "2021-04-30", "2021-03-01", "CI");
    assertThat(dashboardPipelineHealthInfoMeanMedian.getExecutions().getMeanInfo().getDuration()).isEqualTo("100");
    assertThat(dashboardPipelineHealthInfoMeanMedian.getExecutions().getMeanInfo().getRate()).isEqualTo("60");
    assertThat(dashboardPipelineHealthInfoMeanMedian.getExecutions().getMedianInfo().getDuration()).isEqualTo("150");
    assertThat(dashboardPipelineHealthInfoMeanMedian.getExecutions().getMedianInfo().getRate()).isEqualTo("-30");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDashboardPipelineExecutionInfo() {
    String startInterval = "2021-04-01";
    String endInterval = "2021-04-05";

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.ABORTED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());
    List<String> time = Arrays.asList("2021-04-02 17:37:45.383", "2021-04-02 17:37:45.383", "2021-04-02 0:0:0",
        "2021-04-05 0.0.0", "2021-04-03 0.0.0", "2021-04-01 0:0:0", "2021-04-01 23:59:999");
    StatusAndTime statusAndTime = StatusAndTime.builder().status(status).time(time).build();

    doReturn(statusAndTime).when(pipelineDashboardService).queryCalculatorForStatusAndTime(anyString());

    DashboardPipelineExecutionInfo dashboardPipelineExecutionInfo =
        pipelineDashboardService.getDashboardPipelineExecutionInfo(
            "ac", "or", "pr", "pip", startInterval, endInterval, "CI");

    List<PipelineExecutionInfo> pipelineExecutionInfoList = new ArrayList<>();
    pipelineExecutionInfoList.add(PipelineExecutionInfo.builder()
                                      .date("2021-04-01")
                                      .count(PipelineCountInfo.builder().total(2).success(0).failure(2).build())
                                      .build());
    pipelineExecutionInfoList.add(PipelineExecutionInfo.builder()
                                      .date("2021-04-02")
                                      .count(PipelineCountInfo.builder().total(3).success(1).failure(2).build())
                                      .build());
    pipelineExecutionInfoList.add(PipelineExecutionInfo.builder()
                                      .date("2021-04-03")
                                      .count(PipelineCountInfo.builder().total(1).success(0).failure(1).build())
                                      .build());
    pipelineExecutionInfoList.add(PipelineExecutionInfo.builder()
                                      .date("2021-04-04")
                                      .count(PipelineCountInfo.builder().total(0).success(0).failure(0).build())
                                      .build());
    pipelineExecutionInfoList.add(PipelineExecutionInfo.builder()
                                      .date("2021-04-05")
                                      .count(PipelineCountInfo.builder().total(1).success(0).failure(0).build())
                                      .build());
    assertThat(dashboardPipelineExecutionInfo.getPipelineExecutionInfoList()).isEqualTo(pipelineExecutionInfoList);
  }
}
