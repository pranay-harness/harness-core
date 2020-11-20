package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.task.citasks.CICleanupTaskHandler;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CIK8CleanupTaskHandlerTest extends CategoryTest {
  @Mock private K8sConnectorHelper k8sConnectorHelper;
  @Mock private CIK8CtlHandler kubeCtlHandler;
  @InjectMocks private CIK8CleanupTaskHandler cik8DeleteSetupTaskHandler;

  private static final String namespace = "default";
  private static final String podName = "pod";
  private static final String serviceName = "svc";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private CIK8CleanupTaskParams getTaskParams() {
    ConnectorDetails connectorDetails = ConnectorDetails.builder().build();
    List<String> podList = new ArrayList<>();
    podList.add(podName);
    return CIK8CleanupTaskParams.builder()
        .k8sConnector(connectorDetails)
        .podNameList(podList)
        .namespace(namespace)
        .build();
  }

  private CIK8CleanupTaskParams getTaskParamsWithService() {
    ConnectorDetails connectorDetails = ConnectorDetails.builder().build();
    List<String> podList = new ArrayList<>();
    podList.add(podName);
    return CIK8CleanupTaskParams.builder()
        .k8sConnector(connectorDetails)
        .podNameList(podList)
        .serviceNameList(Arrays.asList(serviceName))
        .namespace(namespace)
        .build();
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithSuccess() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParams();

    when(k8sConnectorHelper.createKubernetesClient(any(ConnectorDetails.class))).thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenReturn(Boolean.TRUE);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithFailure() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParams();

    when(k8sConnectorHelper.createKubernetesClient(any(ConnectorDetails.class))).thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenReturn(Boolean.FALSE);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithDeleteException() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParams();

    when(k8sConnectorHelper.createKubernetesClient(any(ConnectorDetails.class))).thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenThrow(KubernetesClientException.class);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithServiceSuccess() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParamsWithService();

    when(k8sConnectorHelper.createKubernetesClient(any(ConnectorDetails.class))).thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenReturn(Boolean.TRUE);
    when(kubeCtlHandler.deleteService(kubernetesClient, namespace, serviceName)).thenReturn(Boolean.TRUE);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithServiceFailure() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParamsWithService();

    when(k8sConnectorHelper.createKubernetesClient(any(ConnectorDetails.class))).thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenReturn(Boolean.TRUE);
    when(kubeCtlHandler.deleteService(kubernetesClient, namespace, serviceName)).thenReturn(Boolean.FALSE);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(CICleanupTaskHandler.Type.GCP_K8, cik8DeleteSetupTaskHandler.getType());
  }
}
