/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.beans.FeatureName;
import io.harness.exception.DuplicateEntityException;
import io.harness.spec.server.ssca.v1.model.Artifact;
import io.harness.spec.server.ssca.v1.model.EnforceSbomRequestBody;
import io.harness.spec.server.ssca.v1.model.EnforceSbomResponseBody;
import io.harness.spec.server.ssca.v1.model.EnforcementSummaryResponse;
import io.harness.spec.server.ssca.v1.model.PolicyViolation;
import io.harness.ssca.beans.PolicyEvaluationResult;
import io.harness.ssca.beans.PolicyType;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.ArtifactEntity.ArtifactEntityKeys;
import io.harness.ssca.entities.EnforcementSummaryEntity;

import com.google.inject.Inject;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
public class EnforcementStepServiceImpl implements EnforcementStepService {
  @Inject ArtifactService artifactService;
  @Inject EnforcementSummaryService enforcementSummaryService;
  @Inject EnforcementResultService enforcementResultService;
  @Inject FeatureFlagService featureFlagService;
  @Inject Map<PolicyType, PolicyEvaluationService> policyEvaluationServiceMapBinder;

  @Override
  public EnforceSbomResponseBody enforceSbom(
      String accountId, String orgIdentifier, String projectIdentifier, EnforceSbomRequestBody body) {
    if (enforcementSummaryService
            .getEnforcementSummary(accountId, orgIdentifier, projectIdentifier, body.getEnforcementId())
            .isPresent()) {
      throw new DuplicateEntityException(
          String.format("Enforcement Summary already present with enforcement id [%s]", body.getEnforcementId()));
    }
    String artifactId =
        artifactService.generateArtifactId(body.getArtifact().getRegistryUrl(), body.getArtifact().getName());
    ArtifactEntity artifactEntity =
        artifactService
            .getArtifact(accountId, orgIdentifier, projectIdentifier, artifactId,
                Sort.by(ArtifactEntityKeys.createdOn).descending())
            .orElseThrow(()
                             -> new NotFoundException(
                                 String.format("Artifact with image name [%s] and registry Url [%s] is not found",
                                     body.getArtifact().getName(), body.getArtifact().getRegistryUrl())));
    PolicyEvaluationResult policyEvaluationResult;
    if (featureFlagService.isFeatureFlagEnabled(accountId, FeatureName.SSCA_ENFORCEMENT_OPA.name())
        && CollectionUtils.isNotEmpty(body.getPolicySetRef())) {
      policyEvaluationResult = policyEvaluationServiceMapBinder.get(PolicyType.OPA)
                                   .evaluatePolicy(accountId, orgIdentifier, projectIdentifier, body, artifactEntity);
    } else {
      policyEvaluationResult = policyEvaluationServiceMapBinder.get(PolicyType.SSCA)
                                   .evaluatePolicy(accountId, orgIdentifier, projectIdentifier, body, artifactEntity);
    }
    String status = enforcementSummaryService.persistEnforcementSummary(body.getEnforcementId(),
        policyEvaluationResult.getDenyListViolations(), policyEvaluationResult.getAllowListViolations(), artifactEntity,
        body.getPipelineExecutionId());

    EnforceSbomResponseBody responseBody = new EnforceSbomResponseBody();
    responseBody.setEnforcementId(body.getEnforcementId());
    responseBody.setStatus(status);

    return responseBody;
  }

  @Override
  public EnforcementSummaryResponse getEnforcementSummary(
      String accountId, String orgIdentifier, String projectIdentifier, String enforcementId) {
    EnforcementSummaryEntity enforcementSummary =
        enforcementSummaryService.getEnforcementSummary(accountId, orgIdentifier, projectIdentifier, enforcementId)
            .orElseThrow(()
                             -> new NotFoundException(String.format(
                                 "Enforcement with enforcementIdentifier [%s] is not found", enforcementId)));

    return new EnforcementSummaryResponse()
        .enforcementId(enforcementSummary.getEnforcementId())
        .artifact(new Artifact()
                      .id(enforcementSummary.getArtifact().getArtifactId())
                      .name(enforcementSummary.getArtifact().getName())
                      .type(enforcementSummary.getArtifact().getType())
                      .registryUrl(enforcementSummary.getArtifact().getUrl())
                      .tag(enforcementSummary.getArtifact().getTag())

                )
        .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
        .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
        .status(enforcementSummary.getStatus());
  }

  @Override
  public Page<PolicyViolation> getPolicyViolations(String accountId, String orgIdentifier, String projectIdentifier,
      String enforcementId, String searchText, Pageable pageable) {
    return enforcementResultService
        .getPolicyViolations(accountId, orgIdentifier, projectIdentifier, enforcementId, searchText, pageable)
        .map(enforcementResultEntity
            -> new PolicyViolation()
                   .enforcementId(enforcementResultEntity.getEnforcementID())
                   .account(enforcementResultEntity.getAccountId())
                   .org(enforcementResultEntity.getOrgIdentifier())
                   .project(enforcementResultEntity.getProjectIdentifier())
                   .artifactId(enforcementResultEntity.getArtifactId())
                   .imageName(enforcementResultEntity.getImageName())
                   .purl(enforcementResultEntity.getPurl())
                   .orchestrationId(enforcementResultEntity.getOrchestrationID())
                   .license(enforcementResultEntity.getLicense())
                   .tag(enforcementResultEntity.getTag())
                   .supplier(enforcementResultEntity.getSupplier())
                   .supplierType(enforcementResultEntity.getSupplierType())
                   .name(enforcementResultEntity.getName())
                   .version(enforcementResultEntity.getVersion())
                   .packageManager(enforcementResultEntity.getPackageManager())
                   .violationType(enforcementResultEntity.getViolationType())
                   .violationDetails(enforcementResultEntity.getViolationDetails()));
  }
}
