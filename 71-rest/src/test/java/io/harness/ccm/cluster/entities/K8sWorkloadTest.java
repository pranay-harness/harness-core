package io.harness.ccm.cluster.entities;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sWorkloadTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldReplaceDotsInPrePersist() throws Exception {
    K8sWorkload k8sWorkload = K8sWorkload.builder()
                                  .namespace("harness")
                                  .name("batch-processing")
                                  .kind("Deployment")
                                  .labels(ImmutableMap.of("app", "batch-processing", "harness.io/release-name",
                                      "3f8a52b6-6053-36b1-8994-f9a424476ffe"))
                                  .build();
    k8sWorkload.prePersist();
    assertThat(k8sWorkload.getLabels())
        .isEqualTo(ImmutableMap.of(
            "app", "batch-processing", "harness~io/release-name", "3f8a52b6-6053-36b1-8994-f9a424476ffe"));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRestoreDotsInPostLoad() throws Exception {
    K8sWorkload k8sWorkload = K8sWorkload.builder()
                                  .namespace("harness")
                                  .name("batch-processing")
                                  .kind("Deployment")
                                  .labels(ImmutableMap.of("app", "batch-processing", "harness~io/release-name",
                                      "3f8a52b6-6053-36b1-8994-f9a424476ffe"))
                                  .build();
    k8sWorkload.postLoad();
    assertThat(k8sWorkload.getLabels())
        .isEqualTo(ImmutableMap.of(
            "app", "batch-processing", "harness.io/release-name", "3f8a52b6-6053-36b1-8994-f9a424476ffe"));
  }
}
