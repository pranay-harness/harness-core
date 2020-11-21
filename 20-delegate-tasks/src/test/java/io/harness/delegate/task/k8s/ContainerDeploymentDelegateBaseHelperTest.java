package io.harness.delegate.task.k8s;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ContainerDeploymentDelegateBaseHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock LogCallback logCallback;

  @Spy @InjectMocks ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

  @Before
  public void setup() {
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doNothing().when(logCallback).saveExecutionLog(anyString(), any(LogLevel.class));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetContainerInfosWhenReadyByLabels() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    List<Pod> existingPods = asList(new Pod());
    List<? extends HasMetadata> controllers = getMockedControllers();

    when(kubernetesContainerService.getControllers(any(KubernetesConfig.class), anyMap())).thenReturn(controllers);

    containerDeploymentDelegateBaseHelper.getContainerInfosWhenReadyByLabels(
        kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);

    verify(kubernetesContainerService, times(1))
        .getContainerInfosWhenReady(
            kubernetesConfig, "deployment-name", 0, -1, 30, existingPods, false, logCallback, true, 0, "default");
    verify(kubernetesContainerService, times(1))
        .getContainerInfosWhenReady(
            kubernetesConfig, "daemonSet-name", 0, -1, 30, existingPods, true, logCallback, true, 0, "default");
  }

  private List<? extends HasMetadata> getMockedControllers() {
    HasMetadata controller_1 = mock(Deployment.class);
    HasMetadata controller_2 = mock(DaemonSet.class);
    ObjectMeta metaData_1 = mock(ObjectMeta.class);
    ObjectMeta metaData_2 = mock(ObjectMeta.class);
    when(controller_1.getKind()).thenReturn("Deployment");
    when(controller_2.getKind()).thenReturn("DaemonSet");
    when(controller_1.getMetadata()).thenReturn(metaData_1);
    when(controller_2.getMetadata()).thenReturn(metaData_2);
    when(metaData_1.getName()).thenReturn("deployment-name");
    when(metaData_2.getName()).thenReturn("daemonSet-name");
    return asList(controller_1, controller_2);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetControllerCountByLabels() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    Map<String, String> labels = new HashMap<>();

    List<? extends HasMetadata> controllers = getMockedControllers();
    when(kubernetesContainerService.getControllers(any(KubernetesConfig.class), anyMap())).thenReturn(controllers);
    assertThat(containerDeploymentDelegateBaseHelper.getControllerCountByLabels(kubernetesConfig, labels)).isEqualTo(2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getExistingPodsByLabels() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    Map<String, String> labels = new HashMap<>();

    when(kubernetesContainerService.getPods(kubernetesConfig, labels)).thenReturn(asList(new Pod()));

    final List<Pod> pods = containerDeploymentDelegateBaseHelper.getExistingPodsByLabels(kubernetesConfig, labels);
    assertThat(pods).hasSize(1);
    verify(kubernetesContainerService, times(1)).getPods(kubernetesConfig, labels);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetContainerInfosWhenReadyByLabel() {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    List<Pod> existingPods = asList(new Pod());

    when(kubernetesContainerService.getPods(eq(kubernetesConfig), anyMap())).thenReturn(existingPods);
    doReturn(null)
        .when(containerDeploymentDelegateBaseHelper)
        .getContainerInfosWhenReadyByLabels(any(KubernetesConfig.class), any(LogCallback.class), anyMap(), anyList());

    containerDeploymentDelegateBaseHelper.getContainerInfosWhenReadyByLabels(
        kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);

    verify(containerDeploymentDelegateBaseHelper, times(1))
        .getContainerInfosWhenReadyByLabels(
            kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);
  }
}
