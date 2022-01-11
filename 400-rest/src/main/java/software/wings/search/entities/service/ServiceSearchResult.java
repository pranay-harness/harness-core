/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import software.wings.api.DeploymentType;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;
import software.wings.utils.ArtifactType;

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
public class ServiceSearchResult extends SearchResult {
  private String appId;
  private String appName;
  private ArtifactType artifactType;
  private DeploymentType deploymentType;
  private List<RelatedAuditView> audits;
  private List<RelatedDeploymentView> deployments;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> pipelines;
  private Integer auditsCount = 0;
  private Integer deploymentsCount = 0;
  private static final int MAX_ENTRIES = 3;
  private static final int DAYS_TO_RETAIN = 7;
  private static final long startTimestampToRetainFrom =
      SearchEntityUtils.getTimestampNdaysBackInMillis(DAYS_TO_RETAIN);

  private void setDeployments(ServiceView serviceView) {
    if (EmptyPredicate.isNotEmpty(serviceView.getDeployments())) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(serviceView.getDeploymentTimestamps(), startTimestampToRetainFrom).size();
      removeStaleDeploymentsEntries(serviceView);
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

  private void setAudits(ServiceView serviceView) {
    if (EmptyPredicate.isNotEmpty(serviceView.getAudits())) {
      this.auditsCount =
          SearchEntityUtils.truncateList(serviceView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(serviceView);
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

  private void removeStaleDeploymentsEntries(ServiceView serviceView) {
    if (this.deploymentsCount >= MAX_ENTRIES) {
      this.deployments = serviceView.getDeployments();
    } else {
      int length = serviceView.getDeployments().size();
      this.deployments = serviceView.getDeployments().subList(length - this.deploymentsCount, length);
    }
    Collections.reverse(this.deployments);
  }

  private void removeStaleAuditEntries(ServiceView serviceView) {
    if (this.auditsCount >= MAX_ENTRIES) {
      this.audits = serviceView.getAudits();
    } else {
      int length = serviceView.getAudits().size();
      this.audits = serviceView.getAudits().subList(length - this.auditsCount, length);
    }
    Collections.reverse(this.audits);
  }

  public ServiceSearchResult(ServiceView serviceView, boolean includeAudits, float searchScore) {
    super(serviceView.getId(), serviceView.getName(), serviceView.getDescription(), serviceView.getAccountId(),
        serviceView.getCreatedAt(), serviceView.getLastUpdatedAt(), serviceView.getType(), serviceView.getCreatedBy(),
        serviceView.getLastUpdatedBy(), searchScore);
    this.appId = serviceView.getAppId();
    this.appName = serviceView.getAppName();
    this.artifactType = serviceView.getArtifactType();
    this.deploymentType = serviceView.getDeploymentType();
    this.workflows = serviceView.getWorkflows();
    this.pipelines = serviceView.getPipelines();
    setDeployments(serviceView);
    if (includeAudits) {
      setAudits(serviceView);
    }
  }
}
