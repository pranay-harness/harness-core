package software.wings.search.entities.workflow;

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

  private void setAudits(WorkflowView workflowView) {
    if (EmptyPredicate.isNotEmpty(workflowView.getAudits())) {
      this.auditsCount =
          SearchEntityUtils.truncateList(workflowView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(workflowView);
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