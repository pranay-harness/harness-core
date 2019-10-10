package software.wings.delegatetasks.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

public class K8sTaskHelperTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private KubernetesContainerService mockKubernetesContainerService;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private GitService mockGitService;
  @Mock private EncryptionService mockEncryptionService;
  @Mock private HelmTaskHelper mockHelmTaskHelper;
  @Mock private KubernetesHelperService mockKubernetesHelperService;

  @Spy @Inject @InjectMocks private K8sTaskHelper helper;

  @Test
  @Category(UnitTests.class)
  public void testGetTargetInstancesForCanary() {
    ExecutionLogCallback mockLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockLogCallback).saveExecutionLog(anyString());
    assertThat(helper.getTargetInstancesForCanary(50, 4, mockLogCallback)).isEqualTo(2);
    assertThat(helper.getTargetInstancesForCanary(5, 2, mockLogCallback)).isEqualTo(1);
  }
}