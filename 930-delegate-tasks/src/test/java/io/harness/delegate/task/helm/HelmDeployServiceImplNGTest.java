package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABHINAV2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.helm.HelmClient;
import io.harness.helm.HelmClientImpl;
import io.harness.helm.HelmCommandData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class HelmDeployServiceImplNGTest extends CategoryTest {
  @InjectMocks HelmDeployServiceImplNG helmDeployService;

  @Mock private HelmClient helmClient;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testSuccessfulHelmRollback() throws InterruptedException, IOException, TimeoutException {
    // TODO:
    //    HelmRollbackCommandRequestNG commandRequest = HelmRollbackCommandRequestNG.builder().build();
    //    HelmClientImpl.HelmCliResponse simulatedResponse =
    //        HelmClientImpl.HelmCliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    //    doReturn(simulatedResponse).when(helmClient).rollback(any(HelmCommandData.class));
    //
    //    HelmCommandResponseNG response = helmDeployService.rollback(commandRequest);
    //
    //    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testFailedHelmRollback() throws InterruptedException, IOException, TimeoutException {
    // TODO:
    //    HelmRollbackCommandRequestNG commandRequest = HelmRollbackCommandRequestNG.builder().build();
    //    doThrow(IOException.class).when(helmClient).rollback(any(HelmCommandData.class));
    //
    //    HelmCommandResponseNG response = helmDeployService.rollback(commandRequest);
    //
    //    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testReleaseHistoryWithEmptyReleaseHistory() throws InterruptedException, IOException, TimeoutException {
    // TODO:
    //    HelmReleaseHistoryCommandRequestNG commandRequest = HelmReleaseHistoryCommandRequestNG.builder().build();
    //    HelmClientImpl.HelmCliResponse simulatedResponse =
    //    HelmClientImpl.HelmCliResponse.builder().output("").build();
    //    doReturn(simulatedResponse).when(helmClient).releaseHistory(any(HelmCommandData.class));
    //
    //    HelmReleaseHistoryCmdResponseNG response = helmDeployService.releaseHistory(commandRequest);
    //
    //   assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    //   assertThat(response.getReleaseInfoList()).isEmpty();
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testReleaseHistoryGivenException() throws InterruptedException, IOException, TimeoutException {
    HelmReleaseHistoryCommandRequestNG commandRequest = HelmReleaseHistoryCommandRequestNG.builder().build();
    doThrow(IOException.class).when(helmClient).releaseHistory(any(HelmCommandData.class));

    HelmReleaseHistoryCmdResponseNG response = helmDeployService.releaseHistory(commandRequest);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testReleaseHistoryGivenNonEmptyOutput() throws InterruptedException, IOException, TimeoutException {
    String nonEmptyCliResponse = "";
    HelmClientImpl.HelmCliResponse simulatedResponse =
        HelmClientImpl.HelmCliResponse.builder().output(nonEmptyCliResponse).build();

    doReturn(simulatedResponse).when(helmClient).releaseHistory(any(HelmCommandData.class));

    // TODO: Insert actual output in nonEmptyCliResponse for covering CSV output parsing function and add valid
    // assertions
  }
}
