package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.ANIL;

import static software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType.CREATE_ROUTE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfCreatePcfResourceCommandTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateFileManager delegateFileManager;
  @Mock private EncryptionService encryptionService;
  @Mock private PcfDeploymentManager pcfDeploymentManager;
  @Mock private DelegateLogService delegateLogService;
  @Mock private ExecutionLogCallback logCallback;
  @InjectMocks @Inject PcfCreatePcfResourceCommandTaskHandler pcfSetupCommandTaskHandler;

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testExecuteInternalSuccess() {
    PcfInfraMappingDataRequest mappingDataRequest =
        PcfInfraMappingDataRequest.builder()
            .pcfCommandType(CREATE_ROUTE)
            .pcfConfig(PcfConfig.builder().username("test".toCharArray()).password("test".toCharArray()).build())
            .timeoutIntervalInMin(10)
            .build();
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    PcfCommandExecutionResponse response =
        pcfSetupCommandTaskHandler.executeTaskInternal(mappingDataRequest, encryptedDataDetails, logCallback, false);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testExecuteInternalFailure() throws PivotalClientApiException, InterruptedException {
    reset(pcfDeploymentManager);
    PcfInfraMappingDataRequest mappingDataRequest =
        PcfInfraMappingDataRequest.builder()
            .pcfCommandType(CREATE_ROUTE)
            .pcfConfig(PcfConfig.builder().username("test".toCharArray()).password("test".toCharArray()).build())
            .timeoutIntervalInMin(10)
            .build();
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    doThrow(Exception.class)
        .when(pcfDeploymentManager)
        .createRouteMap(
            any(PcfRequestConfig.class), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
    PcfCommandExecutionResponse response =
        pcfSetupCommandTaskHandler.executeTaskInternal(mappingDataRequest, encryptedDataDetails, logCallback, false);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);

    PcfCommandDeployRequest deployRequest = PcfCommandDeployRequest.builder().build();
    assertThatThrownBy(
        () -> pcfSetupCommandTaskHandler.executeTaskInternal(deployRequest, encryptedDataDetails, logCallback, false))
        .isInstanceOf(InvalidArgumentsException.class);
  }
}
