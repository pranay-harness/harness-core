package io.harness.perpetualtask.k8s.utils;

import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sClusterHelperTest extends CategoryTest {
  private static final String OLD_CLUSTER = "cluster_1";
  private static final String NEW_CLUSTER = RandomStringUtils.randomAlphabetic(10);

  @Before
  public void setUp() throws Exception {
    K8sClusterHelper.setAsSeen(OLD_CLUSTER);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testNewCluster() throws Exception {
    assertThat(K8sClusterHelper.isSeen(NEW_CLUSTER)).isFalse();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testOldCluster() throws Exception {
    assertThat(K8sClusterHelper.isSeen(OLD_CLUSTER)).isTrue();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    K8sClusterHelper.clean();
  }
}
