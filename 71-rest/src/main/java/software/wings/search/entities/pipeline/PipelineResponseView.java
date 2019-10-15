package software.wings.search.entities.pipeline;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PipelineResponseView extends SearchResult {
  private String appId;
  private String appName;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> services;
  private List<RelatedDeploymentView> deployments;
  private List<RelatedAuditView> audits;
  private Integer auditsCount = 0;
  private Integer deploymentsCount = 0;

  public PipelineResponseView(PipelineView pipelineView) {
    super(pipelineView.getId(), pipelineView.getName(), pipelineView.getDescription(), pipelineView.getAccountId(),
        pipelineView.getCreatedAt(), pipelineView.getLastUpdatedAt(), pipelineView.getType(),
        pipelineView.getCreatedBy(), pipelineView.getLastUpdatedBy());
    long timestamp7DaysBack = SearchEntityUtils.getTimestampNdaysBackInSeconds(7);
    this.appId = pipelineView.getAppId();
    this.appName = pipelineView.getAppName();
    this.workflows = pipelineView.getWorkflows();
    this.services = pipelineView.getServices();
    if (!pipelineView.getDeployments().isEmpty()) {
      this.deploymentsCount =
          SearchEntityUtils.truncateList(pipelineView.getDeploymentTimestamps(), timestamp7DaysBack).size();
      if (this.deploymentsCount > 2) {
        this.deployments = pipelineView.getDeployments();
      } else {
        int length = pipelineView.getDeployments().size();
        this.deployments = pipelineView.getDeployments().subList(length - this.deploymentsCount, length);
      }
    }
    if (!pipelineView.getAudits().isEmpty()) {
      this.auditsCount = SearchEntityUtils.truncateList(pipelineView.getAuditTimestamps(), timestamp7DaysBack).size();
      if (this.auditsCount > 2) {
        this.audits = pipelineView.getAudits();
      } else {
        int length = pipelineView.getAudits().size();
        this.audits = pipelineView.getAudits().subList(length - this.auditsCount, length);
      }
    }
  }
}