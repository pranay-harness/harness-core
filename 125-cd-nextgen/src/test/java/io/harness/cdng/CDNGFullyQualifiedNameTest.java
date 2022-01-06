/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class CDNGFullyQualifiedNameTest extends CDNGTestBase {
  @Inject SimpleVisitorFactory factory;
  private final String ACCOUNT = "ACCOUNT";
  private final String ORG = "ORG";
  private final String PROJECT = "PROJECT";
  private Set<String> expectedReferences = Sets.newHashSet(
      "pipeline.stages.stage_1.spec.infrastructure.infrastructureDefinition.spec.connectorRef",
      "pipeline.stages.stage_1.spec.serviceConfig.serviceDefinition.spec.manifests.manifest1.spec.store.spec.connectorRef",
      "pipeline.stages.stage_1.spec.serviceConfig.serviceDefinition.spec.manifests.values.spec.store.spec.connectorRef",
      "pipeline.stages.stage_1.spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.connectorRef",
      "pipeline.stages.stage_1.spec.serviceConfig.service.identifier",
      "pipeline.stages.stage_1.spec.infrastructure.environment.identifier");

  private Map<String, String> identifierToFieldName = Maps.newHashMap();
  private List<String> arrayFields = new ArrayList<>();

  @Before
  public void setup() {
    identifierToFieldName.put("stage_1", "stage");
    identifierToFieldName.put("manifest1", "manifest");
    arrayFields.add("stages");
    arrayFields.add("manifests");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testFullyQualifiedNameGeneration() throws IOException {
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
    Set<String> entityReferences = getFqnUsingVisitorFramework(stageElementConfig);
    Set<String> yamlUtilsFqn = getFqnUsingYamlNode(yamlField);

    // log.info("Validating fqn generation from yamlUtils and Visitor framework");
    assertThat(entityReferences).isEqualTo(yamlUtilsFqn);

    validateFqnIsGeneratedUsingFQNUtils(pipelineYaml);
    assertThat(entityReferences).isEqualTo(expectedReferences);
  }

  private void validateFqnIsGeneratedUsingFQNUtils(String pipelineYaml) throws IOException {
    Map<FQN, Object> fqnObjectMap =
        FQNMapGenerator.generateFQNMap(YamlUtils.readTree(pipelineYaml).getNode().getCurrJsonNode());
    Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();
    fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));

    for (String fqn : expectedReferences) {
      assertThat(fqnToObjectMapMergedYaml.containsKey(fqn)).isTrue();
    }
  }

  private Set<String> getFqnUsingVisitorFramework(StageElementConfig stageElementConfig) {
    List<String> qualifiedNameList = new LinkedList<>();
    qualifiedNameList.add(YAMLFieldNameConstants.PIPELINE);
    qualifiedNameList.add(YAMLFieldNameConstants.STAGES);
    qualifiedNameList.add(stageElementConfig.getIdentifier());
    qualifiedNameList.add(YAMLFieldNameConstants.SPEC);
    EntityReferenceExtractorVisitor visitor =
        factory.obtainEntityReferenceExtractorVisitor(ACCOUNT, ORG, PROJECT, qualifiedNameList);

    visitor.walkElementTree(stageElementConfig.getStageType());
    Set<EntityDetailProtoDTO> references = visitor.getEntityReferenceSet();
    return references.stream()
        .map(EntityDetailProtoDTO::getIdentifierRef)
        .map(IdentifierRefProtoDTO::getMetadata)
        .map(metaData -> metaData.get("fqn"))
        .collect(Collectors.toSet());
  }

  private Set<String> getFqnUsingYamlNode(YamlField yamlField) {
    Set<String> yamlUtilsFqn = new HashSet<>();
    YamlNode currentYamlField = yamlField.getNode();
    yamlUtilsFqn.add(YamlUtils.getFullyQualifiedName(currentYamlField.getField("pipeline")
                                                         .getNode()
                                                         .getField("stages")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("stage")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("infrastructure")
                                                         .getNode()
                                                         .getField("infrastructureDefinition")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("connectorRef")
                                                         .getNode()));
    yamlUtilsFqn.add(YamlUtils.getFullyQualifiedName(currentYamlField.getField("pipeline")
                                                         .getNode()
                                                         .getField("stages")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("stage")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("serviceConfig")
                                                         .getNode()
                                                         .getField("serviceDefinition")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("manifests")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("manifest")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("store")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("connectorRef")
                                                         .getNode()));
    yamlUtilsFqn.add(YamlUtils.getFullyQualifiedName(currentYamlField.getField("pipeline")
                                                         .getNode()
                                                         .getField("stages")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("stage")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("serviceConfig")
                                                         .getNode()
                                                         .getField("serviceDefinition")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("manifests")
                                                         .getNode()
                                                         .asArray()
                                                         .get(1)
                                                         .getField("manifest")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("store")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("connectorRef")
                                                         .getNode()));

    yamlUtilsFqn.add(YamlUtils.getFullyQualifiedName(currentYamlField.getField("pipeline")
                                                         .getNode()
                                                         .getField("stages")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("stage")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("serviceConfig")
                                                         .getNode()
                                                         .getField("serviceDefinition")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("artifacts")
                                                         .getNode()
                                                         .getField("primary")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("connectorRef")
                                                         .getNode()));
    yamlUtilsFqn.add(YamlUtils.getFullyQualifiedName(currentYamlField.getField("pipeline")
                                                         .getNode()
                                                         .getField("stages")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("stage")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("serviceConfig")
                                                         .getNode()
                                                         .getField("service")
                                                         .getNode()
                                                         .getField("identifier")
                                                         .getNode()));
    yamlUtilsFqn.add(YamlUtils.getFullyQualifiedName(currentYamlField.getField("pipeline")
                                                         .getNode()
                                                         .getField("stages")
                                                         .getNode()
                                                         .asArray()
                                                         .get(0)
                                                         .getField("stage")
                                                         .getNode()
                                                         .getField("spec")
                                                         .getNode()
                                                         .getField("infrastructure")
                                                         .getNode()
                                                         .getField("environment")
                                                         .getNode()
                                                         .getField("identifier")
                                                         .getNode()));
    return yamlUtilsFqn;
  }
}
