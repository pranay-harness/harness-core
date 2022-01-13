/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GraphNodeMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAccept() {
    SimpleVisitor simpleVisitor = new SimpleVisitor();
    GraphNodeMetadata.builder().id("id1").build().accept(simpleVisitor);
    assertThat(simpleVisitor.getVisited()).containsExactly("id1");

    simpleVisitor = new SimpleVisitor();
    GraphNodeMetadata.builder().id("id1").subGraph(GraphGroupMetadata.builder().build()).build().accept(simpleVisitor);
    assertThat(simpleVisitor.getVisited()).containsExactly("id1");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromOriginGraphNode() {
    assertThat(ExecutionDetailsInternalMetadata.fromGraphNode(null)).isNull();
    GraphGroup graphGroup = new GraphGroup();
    graphGroup.setExecutionStrategy(ExecutionStrategy.PARALLEL);
    graphGroup.setElements(Collections.singletonList(GraphNode.builder().id("id3").build()));
    List<GraphNodeMetadata> graphNodeMetadataList =
        GraphNodeMetadata.fromOriginGraphNode(GraphNode.builder()
                                                  .id("id1")
                                                  .name("n")
                                                  .type("t")
                                                  .executionDetails(prepareExecutionDetailsMap())
                                                  .next(GraphNode.builder().id("id2").build())
                                                  .group(graphGroup)
                                                  .build());
    assertThat(graphNodeMetadataList).isNotNull();
    assertThat(graphNodeMetadataList.size()).isEqualTo(2);

    GraphNodeMetadata graphNodeMetadata = graphNodeMetadataList.get(0);
    assertThat(graphNodeMetadata).isNotNull();
    assertThat(graphNodeMetadata.getInstanceCount()).isNull();
    assertThat(graphNodeMetadata.getId()).isEqualTo("id1");
    assertThat(graphNodeMetadata.getName()).isEqualTo("n");
    assertThat(graphNodeMetadata.getType()).isEqualTo("t");
    assertThat(graphNodeMetadata.getExecutionDetails()).isNotNull();
    assertThat(graphNodeMetadata.getExecutionDetails().size()).isEqualTo(1);
    assertThat(graphNodeMetadata.getExecutionDetails().containsKey("k1")).isTrue();
    assertThat(graphNodeMetadata.getExecutionDetails().get("k1")).isEqualTo("v1");
    assertThat(graphNodeMetadata.getActivityId()).isEqualTo("aid");
    assertThat(graphNodeMetadata.getTiming()).isNull();
    assertThat(graphNodeMetadata.getSubGraph()).isNotNull();
    assertThat(graphNodeMetadata.getSubGraph().getExecutionStrategy()).isEqualTo(ExecutionStrategy.PARALLEL);
    assertThat(graphNodeMetadata.getSubGraph().getElements()).isNotNull();
    assertThat(graphNodeMetadata.getSubGraph().getElements().size()).isEqualTo(1);
    assertThat(graphNodeMetadata.getSubGraph().getElements().get(0)).isNotNull();
    assertThat(graphNodeMetadata.getSubGraph().getElements().get(0).size()).isEqualTo(1);
    assertThat(graphNodeMetadata.getSubGraph().getElements().get(0).get(0).getId()).isEqualTo("id3");
    assertThat(graphNodeMetadataList.get(1).getId()).isEqualTo("id2");
  }

  private Map<String, ExecutionDataValue> prepareExecutionDetailsMap() {
    return ImmutableMap.of("k1", ExecutionDataValue.builder().value("v1").build(), "activityId",
        ExecutionDataValue.builder().value("aid").build(), "Unit", ExecutionDataValue.builder().value("unit").build());
  }
}
