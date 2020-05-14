package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterAuthType;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.K8ExecuteCommandTaskParams;
import software.wings.beans.ci.ShellScriptType;
import software.wings.delegatetasks.citasks.ExecuteCommandTaskHandler;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.KubernetesHelperService;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class K8ExecuteCommandTaskHandlerTest extends WingsBaseTest {
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private K8CommandExecutor k8CommandExecutor;
  @InjectMocks private K8ExecuteCommandTaskHandler k8ExecuteCommandTaskHandler;

  private static final String MASTER_URL = "http://masterUrl/";
  private static final String mountPath = "/step";
  private static final String namespace = "default";
  private static final String podName = "pod";
  private static final String containerName = "container";
  private static final String stdoutFilePath = "/dir/stdout";
  private static final String stderrFilePath = "/dir/stderr";
  private static final List<String> commands = Arrays.asList("set -xe", "ls", "cd /dir", "ls");
  private static final Integer cmdTimeoutSecs = 3600;

  public K8ExecuteCommandTaskParams getExecCmdTaskParams() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                            .masterUrl(MASTER_URL)
                                            .authType(KubernetesClusterAuthType.OIDC)
                                            .namespace(namespace)
                                            .build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    K8ExecCommandParams k8ExecCommandParams = K8ExecCommandParams.builder()
                                                  .podName(podName)
                                                  .commands(commands)
                                                  .containerName(containerName)
                                                  .mountPath(mountPath)
                                                  .relStdoutFilePath(stdoutFilePath)
                                                  .relStderrFilePath(stderrFilePath)
                                                  .namespace(namespace)
                                                  .commandTimeoutSecs(cmdTimeoutSecs)
                                                  .scriptType(ShellScriptType.DASH)
                                                  .build();
    return K8ExecuteCommandTaskParams.builder()
        .encryptionDetails(encryptionDetails)
        .kubernetesConfig(kubernetesConfig)
        .k8ExecCommandParams(k8ExecCommandParams)
        .build();
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithK8Error()
      throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    Config config = new ConfigBuilder().withMasterUrl(MASTER_URL).withNamespace(namespace).build();
    OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(config);

    when(kubernetesHelperService.getConfig(
             params.getKubernetesConfig(), params.getEncryptionDetails(), StringUtils.EMPTY))
        .thenReturn(config);
    when(kubernetesHelperService.createHttpClientWithProxySetting(config)).thenReturn(okHttpClient);
    doThrow(KubernetesClientException.class)
        .when(k8CommandExecutor)
        .executeCommand(any(), eq(params.getK8ExecCommandParams()));

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithTimeoutError()
      throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    Config config = new ConfigBuilder().withMasterUrl(MASTER_URL).withNamespace(namespace).build();
    OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(config);

    when(kubernetesHelperService.getConfig(
             params.getKubernetesConfig(), params.getEncryptionDetails(), StringUtils.EMPTY))
        .thenReturn(config);
    when(kubernetesHelperService.createHttpClientWithProxySetting(config)).thenReturn(okHttpClient);
    doThrow(TimeoutException.class).when(k8CommandExecutor).executeCommand(any(), eq(params.getK8ExecCommandParams()));

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithInterruptedError()
      throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    Config config = new ConfigBuilder().withMasterUrl(MASTER_URL).withNamespace(namespace).build();
    OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(config);

    when(kubernetesHelperService.getConfig(
             params.getKubernetesConfig(), params.getEncryptionDetails(), StringUtils.EMPTY))
        .thenReturn(config);
    when(kubernetesHelperService.createHttpClientWithProxySetting(config)).thenReturn(okHttpClient);
    doThrow(InterruptedException.class)
        .when(k8CommandExecutor)
        .executeCommand(any(), eq(params.getK8ExecCommandParams()));

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalSuccess() throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    Config config = new ConfigBuilder().withMasterUrl(MASTER_URL).withNamespace(namespace).build();
    OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(config);

    when(kubernetesHelperService.getConfig(
             params.getKubernetesConfig(), params.getEncryptionDetails(), StringUtils.EMPTY))
        .thenReturn(config);
    when(kubernetesHelperService.createHttpClientWithProxySetting(config)).thenReturn(okHttpClient);
    when(k8CommandExecutor.executeCommand(any(), eq(params.getK8ExecCommandParams())))
        .thenReturn(ExecCommandStatus.SUCCESS);

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    Config config = new ConfigBuilder().withMasterUrl(MASTER_URL).withNamespace(namespace).build();
    OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(config);

    when(kubernetesHelperService.getConfig(
             params.getKubernetesConfig(), params.getEncryptionDetails(), StringUtils.EMPTY))
        .thenReturn(config);
    when(kubernetesHelperService.createHttpClientWithProxySetting(config)).thenReturn(okHttpClient);
    when(k8CommandExecutor.executeCommand(any(), eq(params.getK8ExecCommandParams())))
        .thenReturn(ExecCommandStatus.FAILURE);

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(ExecuteCommandTaskHandler.Type.K8, k8ExecuteCommandTaskHandler.getType());
  }
}