/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.walktree.visitor.entityreference;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.SimpleVisitorFactory;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class EntityReferenceExtractorVisitorTest extends CDNGTestBase {
  @Inject SimpleVisitorFactory factory;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testEntityReferenceExtractorVisitor() throws IOException {
    String ACCOUNT = "ACCOUNT";
    String ORG = "ORG";
    String PROJECT = "PROJECT";
    Set<String> expectedReferences = Sets.newHashSet(
        "pipeline.stages.stage_1.spec.infrastructure.infrastructureDefinition.spec.connectorRef",
        "pipeline.stages.stage_1.spec.serviceConfig.serviceDefinition.spec.manifests.manifest1.spec.store.spec.connectorRef",
        "pipeline.stages.stage_1.spec.serviceConfig.serviceDefinition.spec.manifests.values.spec.store.spec.connectorRef",
        "pipeline.stages.stage_1.spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.connectorRef",
        "pipeline.stages.stage_1.spec.serviceConfig.service.identifier",
        "pipeline.stages.stage_1.spec.infrastructure.environment.identifier");

    ClassLoader classLoader = getClass().getClassLoader();
    String pipelineFilename = "visitor-framework-pipeline.yml";
    String pipelineYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(pipelineFilename)), StandardCharsets.UTF_8);

    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(pipelineYaml));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Stage1 Node
    YamlField stage1Node = stagesNode.getNode().asArray().get(0).getField("stage");
    StageElementConfig stageElementConfig = YamlUtils.read(stage1Node.getNode().toString(), StageElementConfig.class);
    List<String> qualifiedNameList = new LinkedList<>();
    qualifiedNameList.add(YAMLFieldNameConstants.PIPELINE);
    qualifiedNameList.add(YAMLFieldNameConstants.STAGES);
    qualifiedNameList.add(stageElementConfig.getIdentifier());
    qualifiedNameList.add(YAMLFieldNameConstants.SPEC);
    EntityReferenceExtractorVisitor visitor =
        factory.obtainEntityReferenceExtractorVisitor(ACCOUNT, ORG, PROJECT, qualifiedNameList);

    visitor.walkElementTree(stageElementConfig.getStageType());
    Set<EntityDetailProtoDTO> references = visitor.getEntityReferenceSet();
    Set<String> entityReferences = references.stream()
                                       .map(EntityDetailProtoDTO::getIdentifierRef)
                                       .map(IdentifierRefProtoDTO::getMetadata)
                                       .map(metaData -> metaData.get("fqn"))
                                       .collect(Collectors.toSet());

    assertThat(entityReferences).isEqualTo(expectedReferences);
  }
}
