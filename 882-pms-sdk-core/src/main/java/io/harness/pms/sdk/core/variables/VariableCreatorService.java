/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.variables;

import static io.harness.pms.plan.creation.PlanCreatorUtils.supportsField;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.SetupMetadata;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.pipeline.creators.BaseCreatorService;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class VariableCreatorService extends BaseCreatorService<VariableCreationResponse, SetupMetadata> {
  private final PipelineServiceInfoProvider pipelineServiceInfoProvider;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  @Inject
  public VariableCreatorService(
      PipelineServiceInfoProvider pipelineServiceInfoProvider, PmsGitSyncHelper pmsGitSyncHelper) {
    this.pipelineServiceInfoProvider = pipelineServiceInfoProvider;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  public VariablesCreationBlobResponse createVariablesResponse(VariablesCreationBlobRequest request) {
    Dependencies initialDependencies = request.getDeps();

    try (PmsGitSyncBranchContextGuard ignore = pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(
             request.getMetadata().getGitSyncBranchContext(), true)) {
      VariableCreationResponse response = processNodesRecursively(
          initialDependencies, SetupMetadata.newBuilder().build(), VariableCreationResponse.builder().build());
      return response.toBlobResponse();
    }
  }

  public VariableCreationResponse processNodeInternal(SetupMetadata setupMetadata, YamlField yamlField) {
    Optional<VariableCreator> variableCreatorOptional =
        findVariableCreator(pipelineServiceInfoProvider.getVariableCreators(), yamlField);

    if (!variableCreatorOptional.isPresent()) {
      return null;
    }

    VariableCreationResponse response;
    VariableCreator variableCreator = variableCreatorOptional.get();

    response = variableCreator.createVariablesForField(
        VariableCreationContext.builder().currentField(yamlField).build(), yamlField);
    return response;
  }

  @Override
  public void mergeResponses(VariableCreationResponse finalResponse, VariableCreationResponse response) {
    finalResponse.addYamlProperties(response.getYamlProperties());
    finalResponse.addYamlOutputProperties(response.getYamlOutputProperties());
  }

  private Optional<VariableCreator> findVariableCreator(List<VariableCreator> variableCreators, YamlField yamlField) {
    if (EmptyPredicate.isEmpty(variableCreators)) {
      return Optional.empty();
    }
    return variableCreators.stream()
        .filter(variableCreator -> {
          Map<String, Set<String>> supportedTypes = variableCreator.getSupportedTypes();
          return supportsField(supportedTypes, yamlField);
        })
        .findFirst();
  }
}
