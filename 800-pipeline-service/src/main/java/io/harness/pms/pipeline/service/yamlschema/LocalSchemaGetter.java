/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service.yamlschema;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.pipeline.service.yamlschema.approval.ApprovalYamlSchemaService;
import io.harness.pms.pipeline.service.yamlschema.featureflag.FeatureFlagYamlService;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

public class LocalSchemaGetter implements SchemaGetter {
  private final String accountIdentifier;
  private final ModuleType moduleType;
  private YamlSchemaProvider yamlSchemaProvider;
  private final ApprovalYamlSchemaService approvalYamlSchemaService;
  private final FeatureFlagYamlService featureFlagYamlService;
  private final PmsYamlSchemaHelper pmsYamlSchemaHelper;

  public LocalSchemaGetter(String accountIdentifier, ModuleType moduleType, YamlSchemaProvider yamlSchemaProvider,
      ApprovalYamlSchemaService approvalYamlSchemaService, FeatureFlagYamlService featureFlagYamlService,
      PmsYamlSchemaHelper pmsYamlSchemaHelper) {
    this.accountIdentifier = accountIdentifier;
    this.moduleType = moduleType;
    this.yamlSchemaProvider = yamlSchemaProvider;
    this.approvalYamlSchemaService = approvalYamlSchemaService;
    this.featureFlagYamlService = featureFlagYamlService;
    this.pmsYamlSchemaHelper = pmsYamlSchemaHelper;
  }

  @Override
  public List<PartialSchemaDTO> getSchema(List<YamlSchemaWithDetails> yamlSchemaWithDetailsList) {
    List<PartialSchemaDTO> partialSchemaDTOList = new ArrayList<>();
    partialSchemaDTOList.add(approvalYamlSchemaService.getApprovalYamlSchema(null, null, null));
    partialSchemaDTOList.add(featureFlagYamlService.getFeatureFlagYamlSchema(null, null, null));
    return partialSchemaDTOList;
  }

  @Override
  public YamlSchemaDetailsWrapper getSchemaDetails() {
    return YamlSchemaDetailsWrapper.builder()
        .yamlSchemaWithDetailsList(yamlSchemaProvider.getCrossFunctionalStepsSchemaDetails(null, null, null,
            pmsYamlSchemaHelper.getNodeEntityTypesByYamlGroup(StepCategory.STEP.name()), ModuleType.PMS))
        .build();
  }

  @Override
  public JsonNode fetchStepYamlSchema(EntityType entityType) {
    return yamlSchemaProvider.getYamlSchema(entityType, null, null, null);
  }
}
