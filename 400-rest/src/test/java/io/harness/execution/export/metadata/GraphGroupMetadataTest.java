/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.ExecutionStrategy;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;

import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GraphGroupMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAccept() {
    SimpleVisitor simpleVisitor = new SimpleVisitor();
    GraphGroupMetadata.builder()
        .executionStrategy(ExecutionStrategy.PARALLEL)
        .elements(asList(asList(prepareGraphNodeMetadata(1), prepareGraphNodeMetadata(2)),
            Collections.singletonList(prepareGraphNodeMetadata(3))))
        .build()
        .accept(simpleVisitor);
    assertThat(simpleVisitor.getVisited()).containsExactly("id1", "id2", "id3");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromStateExecutionData() {
    assertThat(GraphGroupMetadata.fromGraphGroup(null)).isNull();

    GraphGroup graphGroup = new GraphGroup();
    graphGroup.setElements(asList(null, GraphNode.builder().id("id1").build(), GraphNode.builder().id("id2").build()));
    graphGroup.setExecutionStrategy(ExecutionStrategy.PARALLEL);
    GraphGroupMetadata graphGroupMetadata = GraphGroupMetadata.fromGraphGroup(graphGroup);
    assertThat(graphGroupMetadata).isNotNull();
    assertThat(graphGroupMetadata.getElements()).isNotNull();
    assertThat(graphGroupMetadata.getElements().size()).isEqualTo(2);
    assertThat(graphGroupMetadata.getElements().get(0).get(0).getId()).isEqualTo("id1");
    assertThat(graphGroupMetadata.getElements().get(1).get(0).getId()).isEqualTo("id2");
    assertThat(graphGroupMetadata.getExecutionStrategy()).isEqualTo(ExecutionStrategy.PARALLEL);
  }

  private GraphNodeMetadata prepareGraphNodeMetadata(int idx) {
    return GraphNodeMetadata.builder().id("id" + idx).build();
  }
}
