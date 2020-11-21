package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.K8ExecCommandParams;
import io.harness.delegate.beans.ci.K8ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.ShellScriptType;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.task.citasks.ExecuteCommandTaskHandler;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8ExecuteCommandTaskHandlerTest extends CategoryTest {
  @Mock private K8sConnectorHelper k8sConnectorHelper;
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

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  public K8ExecuteCommandTaskParams getExecCmdTaskParams() {
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);

    KubernetesClusterConfigDTO configDTO =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .config(KubernetesClusterDetailsDTO.builder()
                                        .masterUrl(MASTER_URL)
                                        .auth(KubernetesAuthDTO.builder()
                                                  .authType(KubernetesAuthType.OPEN_ID_CONNECT)
                                                  .credentials(KubernetesOpenIdConnectDTO.builder().build())
                                                  .build())
                                        .build())
                            .build())

            .build();

    ConnectorDetails k8sConnector =
        ConnectorDetails.builder().connectorConfig(configDTO).encryptedDataDetails(encryptionDetails).build();
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
        .k8sConnector(k8sConnector)
        .k8ExecCommandParams(k8ExecCommandParams)
        .build();
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithK8Error()
      throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    DefaultKubernetesClient kubernetesClient = mock(DefaultKubernetesClient.class);
    when(k8sConnectorHelper.getDefaultKubernetesClient(eq(params.getK8sConnector()))).thenReturn(kubernetesClient);
    doThrow(KubernetesClientException.class)
        .when(k8CommandExecutor)
        .executeCommand(any(), eq(params.getK8ExecCommandParams()));

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithTimeoutError()
      throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    DefaultKubernetesClient kubernetesClient = mock(DefaultKubernetesClient.class);
    when(k8sConnectorHelper.getDefaultKubernetesClient(eq(params.getK8sConnector()))).thenReturn(kubernetesClient);
    doThrow(TimeoutException.class).when(k8CommandExecutor).executeCommand(any(), eq(params.getK8ExecCommandParams()));

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithInterruptedError()
      throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    DefaultKubernetesClient kubernetesClient = mock(DefaultKubernetesClient.class);
    when(k8sConnectorHelper.getDefaultKubernetesClient(eq(params.getK8sConnector()))).thenReturn(kubernetesClient);
    doThrow(InterruptedException.class)
        .when(k8CommandExecutor)
        .executeCommand(any(), eq(params.getK8ExecCommandParams()));

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalSuccess() throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    DefaultKubernetesClient kubernetesClient = mock(DefaultKubernetesClient.class);
    when(k8sConnectorHelper.getDefaultKubernetesClient(eq(params.getK8sConnector()))).thenReturn(kubernetesClient);
    when(k8CommandExecutor.executeCommand(any(), eq(params.getK8ExecCommandParams())))
        .thenReturn(ExecCommandStatus.SUCCESS);

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() throws TimeoutException, InterruptedException, UnsupportedEncodingException {
    K8ExecuteCommandTaskParams params = getExecCmdTaskParams();
    DefaultKubernetesClient kubernetesClient = mock(DefaultKubernetesClient.class);
    when(k8sConnectorHelper.getDefaultKubernetesClient(eq(params.getK8sConnector()))).thenReturn(kubernetesClient);
    when(k8CommandExecutor.executeCommand(any(), eq(params.getK8ExecCommandParams())))
        .thenReturn(ExecCommandStatus.FAILURE);

    K8sTaskExecutionResponse response = k8ExecuteCommandTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(ExecuteCommandTaskHandler.Type.K8, k8ExecuteCommandTaskHandler.getType());
  }
}
