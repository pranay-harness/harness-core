/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.utils;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sClusterHelperTest extends CategoryTest {
  private static final String OLD_CLUSTER = "cluster_1";
  private static final String KUBEUID = "kube_id";
  private static final String NEW_CLUSTER = "cluster_2";

  @Before
  public void setUp() throws Exception {
    K8sClusterHelper.setAsSeen(OLD_CLUSTER, KUBEUID);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testNewCluster() throws Exception {
    assertThat(K8sClusterHelper.isSeen(NEW_CLUSTER, KUBEUID)).isFalse();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testOldCluster() throws Exception {
    assertThat(K8sClusterHelper.isSeen(OLD_CLUSTER, KUBEUID)).isTrue();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    K8sClusterHelper.clean();
  }
}
