/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.k8s;

import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.ExecutionDataValue;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sStateExecutionDataTest extends CategoryTest {
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    K8sStateExecutionData k8sStateExecutionData = new K8sStateExecutionData();
    k8sStateExecutionData.setClusterName("clusterName");

    Map<String, ExecutionDataValue> executionSummary = k8sStateExecutionData.getExecutionSummary();
    assertThat(executionSummary).isNotNull();
    assertThat(executionSummary.get("cluster").getValue()).isEqualTo("clusterName");
    assertThat(executionSummary.get("cloudProvider")).isNull();

    k8sStateExecutionData.setCloudProvider("cloudProvider");
    executionSummary = k8sStateExecutionData.getExecutionSummary();
    assertThat(executionSummary).isNotNull();
    assertThat(executionSummary.get("cluster").getValue()).isEqualTo("clusterName");
    assertThat(executionSummary.get("cloudProvider").getValue()).isEqualTo("cloudProvider");

    k8sStateExecutionData.setClusterName(null);
    assertThat(executionSummary).isNotNull();
    executionSummary = k8sStateExecutionData.getExecutionSummary();
    assertThat(executionSummary.get("cluster")).isNull();
    assertThat(executionSummary.get("cloudProvider").getValue()).isEqualTo("cloudProvider");
  }
}
