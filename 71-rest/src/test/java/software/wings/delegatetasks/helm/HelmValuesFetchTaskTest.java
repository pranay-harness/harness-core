package software.wings.delegatetasks.helm;

import static io.harness.rule.OwnerRule.ABOSII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;

public class HelmValuesFetchTaskTest extends WingsBaseTest {
  @Mock private HelmTaskHelper helmTaskHelper;
  @Mock private DelegateLogService delegateLogService;

  @InjectMocks
  HelmValuesFetchTask task = (HelmValuesFetchTask) TaskType.HELM_VALUES_FETCH.getDelegateRunnableTask(
      DelegateTaskPackage.builder().delegateId("delegateId").data(TaskData.builder().async(false).build()).build(),
      null, notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunSuccessful() throws Exception {
    HelmValuesFetchTaskParameters parameters =
        HelmValuesFetchTaskParameters.builder()
            .accountId("accountId")
            .helmChartConfigTaskParams(HelmChartConfigParams.builder().chartName("chart").build())
            .build();

    doReturn("helmValue: value")
        .when(helmTaskHelper)
        .getValuesYamlFromChart(any(HelmChartConfigParams.class), anyLong());

    HelmValuesFetchTaskResponse response = task.run(parameters);
    verify(helmTaskHelper, times(1)).getValuesYamlFromChart(any(HelmChartConfigParams.class), anyLong());
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(response.getValuesFileContent()).isEqualTo("helmValue: value");

    doReturn(null).when(helmTaskHelper).getValuesYamlFromChart(any(HelmChartConfigParams.class), anyLong());

    HelmValuesFetchTaskResponse emptyResponse = task.run(parameters);
    verify(helmTaskHelper, times(2)).getValuesYamlFromChart(any(HelmChartConfigParams.class), anyLong());
    assertThat(emptyResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(emptyResponse.getValuesFileContent()).isEqualTo(null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunWithException() throws Exception {
    HelmValuesFetchTaskParameters parameters =
        HelmValuesFetchTaskParameters.builder()
            .accountId("accountId")
            .helmChartConfigTaskParams(HelmChartConfigParams.builder().chartName("chart").build())
            .build();

    doThrow(new RuntimeException("Unable to fetch Values.yaml"))
        .when(helmTaskHelper)
        .getValuesYamlFromChart(any(HelmChartConfigParams.class), anyLong());

    HelmValuesFetchTaskResponse response = task.run(parameters);
    verify(helmTaskHelper, times(1)).getValuesYamlFromChart(any(HelmChartConfigParams.class), anyLong());
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(response.getErrorMessage()).isEqualTo("Execution failed with Exception: Unable to fetch Values.yaml");
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunWithObjectParams() {
    task.run(new Object[] {});
  }
}