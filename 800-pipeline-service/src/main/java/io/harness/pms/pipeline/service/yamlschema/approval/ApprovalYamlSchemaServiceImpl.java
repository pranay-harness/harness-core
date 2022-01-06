/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service.yamlschema.approval;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.pipeline.service.yamlschema.PmsYamlSchemaHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.approval.stage.ApprovalStageConfig;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public class ApprovalYamlSchemaServiceImpl implements ApprovalYamlSchemaService {
  private static final String APPROVAL_NAMESPACE = "approval";
  private static final String APPROVAL_STAGE_CONFIG = YamlSchemaUtils.getSwaggerName(ApprovalStageConfig.class);

  @Inject private YamlSchemaProvider yamlSchemaProvider;
  @Inject private PmsYamlSchemaHelper pmsYamlSchemaHelper;
  @Inject private YamlSchemaGenerator yamlSchemaGenerator;
  @Inject private List<YamlSchemaRootClass> yamlSchemaRootClasses;

  @Override
  public PartialSchemaDTO getApprovalYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode approvalStageSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.APPROVAL_STAGE, orgIdentifier, projectIdentifier, scope);

    JsonNode definitions = approvalStageSchema.get(DEFINITIONS_NODE);

    JsonNode jsonNode = definitions.get(StepElementConfig.class.getSimpleName());
    pmsYamlSchemaHelper.modifyStepElementSchema((ObjectNode) jsonNode);

    jsonNode = definitions.get(ParallelStepElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      PmsYamlSchemaHelper.flatten((ObjectNode) jsonNode);
    }

    pmsYamlSchemaHelper.removeUnwantedNodes(definitions, ImmutableSet.of(YAMLFieldNameConstants.ROLLBACK_STEPS));
    yamlSchemaProvider.mergeAllV2StepsDefinitions(projectIdentifier, orgIdentifier, scope, (ObjectNode) definitions,
        YamlSchemaUtils.getNodeEntityTypesByYamlGroup(yamlSchemaRootClasses, StepCategory.STEP.name()));

    YamlSchemaUtils.addOneOfInExecutionWrapperConfig(approvalStageSchema.get(DEFINITIONS_NODE),
        YamlSchemaUtils.getNodeClassesByYamlGroup(yamlSchemaRootClasses, StepCategory.STEP.name()), "");

    yamlSchemaGenerator.modifyRefsNamespace(approvalStageSchema, APPROVAL_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(APPROVAL_NAMESPACE, definitions);

    JsonNode partialApprovalSchema = ((ObjectNode) approvalStageSchema).set(DEFINITIONS_NODE, node);

    return PartialSchemaDTO.builder()
        .namespace(APPROVAL_NAMESPACE)
        .nodeName(APPROVAL_STAGE_CONFIG)
        .schema(partialApprovalSchema)
        .nodeType(getApprovalStageTypeName())
        .moduleType(ModuleType.PMS)
        .build();
  }

  private String getApprovalStageTypeName() {
    JsonTypeName annotation = ApprovalStageConfig.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }
}
