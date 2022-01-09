/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.environment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
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
public class EnvironmentSearchResult extends SearchResult {
  private String appId;
  private String appName;
  private EnvironmentType environmentType;
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

  private void setDeployments(EnvironmentView environmentView) {
    if (EmptyPredicate.isNotEmpty(environmentView.getDeployments())) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(environmentView.getDeploymentTimestamps(), startTimestampToRetainFrom).size();
      removeStaleDeploymentsEntries(environmentView);
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

  public void setAudits(List<RelatedAuditView> audits) {
    if (EmptyPredicate.isNotEmpty(audits)) {
      this.audits = audits;
      this.auditsCount = audits.size();
    } else {
      this.audits = new ArrayList<>();
      this.auditsCount = 0;
    }
  }

  private void setAudits(EnvironmentView environmentView) {
    if (EmptyPredicate.isNotEmpty(environmentView.getAudits())) {
      this.auditsCount =
          SearchEntityUtils.truncateList(environmentView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(environmentView);
    }
  }

  private void removeStaleDeploymentsEntries(EnvironmentView environmentView) {
    if (this.deploymentsCount >= MAX_ENTRIES) {
      this.deployments = environmentView.getDeployments();
    } else {
      int length = environmentView.getDeployments().size();
      this.deployments = environmentView.getDeployments().subList(length - this.deploymentsCount, length);
    }
    Collections.reverse(this.deployments);
  }

  private void removeStaleAuditEntries(EnvironmentView environmentView) {
    if (this.auditsCount >= MAX_ENTRIES) {
      this.audits = environmentView.getAudits();
    } else {
      int length = environmentView.getAudits().size();
      this.audits = environmentView.getAudits().subList(length - this.auditsCount, length);
    }
    Collections.reverse(this.audits);
  }

  public EnvironmentSearchResult(EnvironmentView environmentView, boolean includeAudits, float searchScore) {
    super(environmentView.getId(), environmentView.getName(), environmentView.getDescription(),
        environmentView.getAccountId(), environmentView.getCreatedAt(), environmentView.getLastUpdatedAt(),
        environmentView.getType(), environmentView.getCreatedBy(), environmentView.getLastUpdatedBy(), searchScore);
    this.appId = environmentView.getAppId();
    this.appName = environmentView.getAppName();
    this.environmentType = environmentView.getEnvironmentType();
    this.workflows = environmentView.getWorkflows();
    this.pipelines = environmentView.getPipelines();
    setDeployments(environmentView);
    if (includeAudits) {
      setAudits(environmentView);
    }
  }
}
