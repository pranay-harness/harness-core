package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.BaseIntegrationTest;

public class K8SWatchServiceImplTest extends BaseIntegrationTest {
  @Inject K8sWatchPerpetualTaskServiceClient k8SWatchPerpetualTaskServiceClient;

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  public void testCreate() {
    K8sWatchTaskParams params = K8sWatchTaskParams
                                    .newBuilder()
                                    //.setK8SClusterConfig();// TODO: create a default config
                                    .setK8SResourceKind("Pod")
                                    .build();
    //    k8SWatchPerpetualTaskServiceClient.create(params);
    // TODO: verify that the task has been created
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  public void testDelete() {}
}
