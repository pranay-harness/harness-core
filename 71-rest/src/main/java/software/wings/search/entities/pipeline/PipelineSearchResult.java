package software.wings.search.entities.pipeline;

import io.harness.data.structure.EmptyPredicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PipelineSearchResult extends SearchResult {
  private String appId;
  private String appName;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> services;
  private List<RelatedDeploymentView> deployments;
  private List<RelatedAuditView> audits;
  private Integer auditsCount = 0;
  private Integer deploymentsCount = 0;
  private static final int MAX_ENTRIES = 3;
  private static final int DAYS_TO_RETAIN = 7;
  private static final long startTimestampToRetainFrom =
      SearchEntityUtils.getTimestampNdaysBackInMillis(DAYS_TO_RETAIN);

  private void setDeployments(PipelineView pipelineView) {
    if (EmptyPredicate.isNotEmpty(pipelineView.getDeployments())) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(pipelineView.getDeploymentTimestamps(), startTimestampToRetainFrom).size();
      removeStaleDeploymentsEntries(pipelineView);
    }
  }

  private void setAudits(PipelineView pipelineView) {
    if (EmptyPredicate.isNotEmpty(pipelineView.getAudits())) {
      this.auditsCount =
          SearchEntityUtils.truncateList(pipelineView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(pipelineView);
    }
  }

  private void removeStaleDeploymentsEntries(PipelineView pipelineView) {
    if (this.deploymentsCount >= MAX_ENTRIES) {
      this.deployments = pipelineView.getDeployments();
    } else {
      int length = pipelineView.getDeployments().size();
      this.deployments = pipelineView.getDeployments().subList(length - this.deploymentsCount, length);
    }
    Collections.reverse(this.deployments);
  }

  private void removeStaleAuditEntries(PipelineView pipelineView) {
    if (this.auditsCount >= MAX_ENTRIES) {
      this.audits = pipelineView.getAudits();
    } else {
      int length = pipelineView.getAudits().size();
      this.audits = pipelineView.getAudits().subList(length - this.auditsCount, length);
    }
    Collections.reverse(this.audits);
  }

  public PipelineSearchResult(PipelineView pipelineView, boolean includeAudits, float searchScore) {
    super(pipelineView.getId(), pipelineView.getName(), pipelineView.getDescription(), pipelineView.getAccountId(),
        pipelineView.getCreatedAt(), pipelineView.getLastUpdatedAt(), pipelineView.getType(),
        pipelineView.getCreatedBy(), pipelineView.getLastUpdatedBy(), searchScore);
    this.appId = pipelineView.getAppId();
    this.appName = pipelineView.getAppName();
    this.workflows = pipelineView.getWorkflows();
    this.services = pipelineView.getServices();
    setDeployments(pipelineView);
    if (includeAudits) {
      setAudits(pipelineView);
    }
  }
}