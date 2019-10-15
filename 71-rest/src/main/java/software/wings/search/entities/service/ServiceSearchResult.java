package software.wings.search.entities.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.DeploymentType;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;
import software.wings.utils.ArtifactType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
      SearchEntityUtils.getTimestampNdaysBackInSeconds(DAYS_TO_RETAIN);

  private void setDeployments(ServiceView serviceView) {
    if (!serviceView.getDeployments().isEmpty()) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(serviceView.getDeploymentTimestamps(), startTimestampToRetainFrom).size();
      removeStaleDeploymentsEntries(serviceView);
    }
  }

  private void setAudits(ServiceView serviceView) {
    if (!serviceView.getAudits().isEmpty()) {
      this.auditsCount =
          SearchEntityUtils.truncateList(serviceView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(serviceView);
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

  public ServiceSearchResult(ServiceView serviceView) {
    super(serviceView.getId(), serviceView.getName(), serviceView.getDescription(), serviceView.getAccountId(),
        serviceView.getCreatedAt(), serviceView.getLastUpdatedAt(), serviceView.getType(), serviceView.getCreatedBy(),
        serviceView.getLastUpdatedBy());
    this.appId = serviceView.getAppId();
    this.appName = serviceView.getAppName();
    this.artifactType = serviceView.getArtifactType();
    this.deploymentType = serviceView.getDeploymentType();
    this.workflows = serviceView.getWorkflows();
    this.pipelines = serviceView.getPipelines();
    setDeployments(serviceView);
    setAudits(serviceView);
  }
}