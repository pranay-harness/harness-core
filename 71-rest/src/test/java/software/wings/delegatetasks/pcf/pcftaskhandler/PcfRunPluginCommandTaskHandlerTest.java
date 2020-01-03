package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;

import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfRunPluginScriptRequestData;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.charset.StandardCharsets;

public class PcfRunPluginCommandTaskHandlerTest extends WingsBaseTest {
  private static final String USERNMAE = "USERNAME";
  public static final String URL = "URL";
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";
  public static final String RUNNING = "RUNNING";

  @Mock private DelegateFileManager delegateFileManager;
  @Mock private PcfDeploymentManager pcfDeploymentManager;
  @Mock private EncryptionService encryptionService;
  @Mock private DelegateLogService delegateLogService;
  @Mock private PcfCommandTaskHelper pcfCommandTaskHelper;
  @Mock ExecutionLogCallback executionLogCallback;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock PcfClient pcfClient;

  @Spy @InjectMocks PcfRunPluginCommandTaskHandler pcfRunPluginCommandTaskHandler;

  @Before
  public void setup() {
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
  }

  private PcfConfig getPcfConfig() {
    return PcfConfig.builder().username(USERNMAE).endpointUrl(URL).password(new char[0]).build();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_executeTaskInternal() throws PivotalClientApiException {
    doNothing().when(pcfClient).runPcfPluginScript(
        any(PcfRunPluginScriptRequestData.class), Mockito.eq(executionLogCallback));
    PcfRunPluginCommandRequest pcfCommandRequest = getPcfRunPluginCommandRequest();
    pcfRunPluginCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);

    // verify
    ArgumentCaptor<PcfRunPluginScriptRequestData> argumentCaptor =
        ArgumentCaptor.forClass(PcfRunPluginScriptRequestData.class);
    verify(pcfClient).runPcfPluginScript(argumentCaptor.capture(), eq(executionLogCallback));

    final PcfRunPluginScriptRequestData pcfRunPluginScriptRequestData = argumentCaptor.getValue();
    Assertions.assertThat(pcfRunPluginScriptRequestData.getWorkingDirectory()).isNotNull();
    Assertions.assertThat(pcfRunPluginScriptRequestData.getFinalScriptString())
        .isEqualTo("cf create-service " + pcfRunPluginScriptRequestData.getWorkingDirectory() + "/manifest.yml");

    verify(pcfRunPluginCommandTaskHandler, times(1))
        .saveFilesInWorkingDirectory(
            anyListOf(FileData.class), eq(pcfRunPluginScriptRequestData.getWorkingDirectory()));
  }

  private PcfRunPluginCommandRequest getPcfRunPluginCommandRequest() {
    return PcfRunPluginCommandRequest.builder()
        .pcfCommandType(PcfCommandType.SETUP)
        .pcfConfig(getPcfConfig())
        .organization(ORG)
        .space(SPACE)
        .accountId(ACCOUNT_ID)
        .timeoutIntervalInMin(5)
        .renderedScriptString("cf create-service ${service.manifest.repoRoot}/manifest.yml")
        .encryptedDataDetails(null)
        .fileDataList(ImmutableList.of(FileData.builder()
                                           .filePath("manifest.yml")
                                           .fileBytes("file data ".getBytes(StandardCharsets.UTF_8))
                                           .build()))
        .filePathsInScript(ImmutableList.of("/manifest.yml"))
        .build();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_handleError() {
    final PcfCommandExecutionResponse commandExecutionResponse = pcfRunPluginCommandTaskHandler.handleError(
        executionLogCallback, getPcfRunPluginCommandRequest(), new PivotalClientApiException(""));
    Assertions.assertThat(commandExecutionResponse.getCommandExecutionStatus())
        .isEqualTo(CommandExecutionStatus.FAILURE);
  }
}