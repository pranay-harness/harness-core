package io.harness.delegate.task.k8s;

import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.k8s.K8sRollingBaseHandler;
import io.harness.k8s.model.K8sPod;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.stream.Collectors;

public class K8sRollingBaseHandlerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks private K8sRollingBaseHandler k8sRollingBaseHandler;

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTagNewPods() {
    assertThat(k8sRollingBaseHandler.tagNewPods(emptyList(), emptyList())).isEmpty();

    List<K8sPod> pods = k8sRollingBaseHandler.tagNewPods(
        asList(podWithName("pod-1"), podWithName("pod-2")), asList(podWithName("old-pod-1"), podWithName("old-pod-2")));
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");

    pods = k8sRollingBaseHandler.tagNewPods(
        asList(podWithName("pod-1"), podWithName("pod-2")), asList(podWithName("pod-1")));
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(1);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");

    pods = k8sRollingBaseHandler.tagNewPods(asList(podWithName("pod-1"), podWithName("pod-2")), emptyList());
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
  }

  private K8sPod podWithName(String name) {
    return K8sPod.builder().name(name).build();
  }
}
