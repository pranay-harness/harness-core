package software.wings.search.entities.environment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.Environment.EnvironmentType;
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
      SearchEntityUtils.getTimestampNdaysBackInSeconds(DAYS_TO_RETAIN);

  private void setDeployments(EnvironmentView environmentView) {
    if (!environmentView.getDeployments().isEmpty()) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(environmentView.getDeploymentTimestamps(), startTimestampToRetainFrom).size();
      removeStaleDeploymentsEntries(environmentView);
    }
  }

  private void setAudits(EnvironmentView environmentView) {
    if (!environmentView.getAudits().isEmpty()) {
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

  public EnvironmentSearchResult(EnvironmentView environmentView) {
    super(environmentView.getId(), environmentView.getName(), environmentView.getDescription(),
        environmentView.getAccountId(), environmentView.getCreatedAt(), environmentView.getLastUpdatedAt(),
        environmentView.getType(), environmentView.getCreatedBy(), environmentView.getLastUpdatedBy());
    this.appId = environmentView.getAppId();
    this.appName = environmentView.getAppName();
    this.environmentType = environmentView.getEnvironmentType();
    this.workflows = environmentView.getWorkflows();
    this.pipelines = environmentView.getPipelines();
    setDeployments(environmentView);
    setAudits(environmentView);
  }
}
