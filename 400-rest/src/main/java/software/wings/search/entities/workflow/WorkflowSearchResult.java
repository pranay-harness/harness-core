/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.workflow;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class WorkflowSearchResult extends SearchResult {
  private String appId;
  private String appName;
  private String workflowType;
  private Set<EntityInfo> services;
  private Set<EntityInfo> pipelines;
  private String environmentId;
  private String environmentName;
  private List<RelatedDeploymentView> deployments;
  private List<RelatedAuditView> audits;
  private Integer auditsCount = 0;
  private Integer deploymentsCount = 0;
  private static final int MAX_ENTRIES = 3;
  private static final int DAYS_TO_RETAIN = 7;
  private static final long startTimestampToRetainFrom =
      SearchEntityUtils.getTimestampNdaysBackInMillis(DAYS_TO_RETAIN);

  private void setDeployments(WorkflowView workflowView) {
    if (EmptyPredicate.isNotEmpty(workflowView.getDeployments())) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(workflowView.getDeploymentTimestamps(), startTimestampToRetainFrom).size();
      removeStaleDeploymentsEntries(workflowView);
    }
  }

  public void setAudits(List<RelatedAuditView> audits) {
    if (EmptyPredicate.isNotEmpty(audits)) {
      this.audits = audits;
      this.auditsCount = audits.size();
    } else {
      this.audits = new ArrayList<>();
      this.auditsCount = 0;
    }
  }

  private void setAudits(WorkflowView workflowView) {
    if (EmptyPredicate.isNotEmpty(workflowView.getAudits())) {
      this.auditsCount =
          SearchEntityUtils.truncateList(workflowView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(workflowView);
    }
  }

  public void setDeployments(List<RelatedDeploymentView> deployments) {
    if (EmptyPredicate.isNotEmpty(deployments)) {
      this.deployments = deployments;
      this.deploymentsCount = deployments.size();
    } else {
      this.deployments = new ArrayList<>();
      this.deploymentsCount = 0;
    }
  }

  private void removeStaleDeploymentsEntries(WorkflowView workflowView) {
    if (this.deploymentsCount >= MAX_ENTRIES) {
      this.deployments = workflowView.getDeployments();
    } else {
      int length = workflowView.getDeployments().size();
      this.deployments = workflowView.getDeployments().subList(length - this.deploymentsCount, length);
    }
    Collections.reverse(this.deployments);
  }

  private void removeStaleAuditEntries(WorkflowView workflowView) {
    if (this.auditsCount >= MAX_ENTRIES) {
      this.audits = workflowView.getAudits();
    } else {
      int length = workflowView.getAudits().size();
      this.audits = workflowView.getAudits().subList(length - this.auditsCount, length);
    }
    Collections.reverse(this.audits);
  }

  public WorkflowSearchResult(WorkflowView workflowView, boolean includeAudits, float searchScore) {
    super(workflowView.getId(), workflowView.getName(), workflowView.getDescription(), workflowView.getAccountId(),
        workflowView.getCreatedAt(), workflowView.getLastUpdatedAt(), workflowView.getType(),
        workflowView.getCreatedBy(), workflowView.getLastUpdatedBy(), searchScore);
    this.appId = workflowView.getAppId();
    this.appName = workflowView.getAppName();
    this.workflowType = workflowView.getWorkflowType();
    this.services = workflowView.getServices();
    this.pipelines = workflowView.getPipelines();
    this.environmentId = workflowView.getEnvironmentId();
    this.environmentName = workflowView.getEnvironmentName();
    setDeployments(workflowView);
    if (includeAudits) {
      setAudits(workflowView);
    }
  }
}
