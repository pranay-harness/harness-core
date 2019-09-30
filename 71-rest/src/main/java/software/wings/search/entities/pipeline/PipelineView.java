package software.wings.search.entities.pipeline;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.EntityType;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityBaseView;
import software.wings.search.framework.EntityInfo;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "PipelineViewKeys")
public class PipelineView extends EntityBaseView {
  private String appId;
  private String appName;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> services;
  private List<RelatedDeploymentView> deployments;
  private List<RelatedAuditView> audits;
  private List<Long> deploymentTimestamps;
  private List<Long> auditTimestamps;

  PipelineView(String uuid, String name, String description, String accountId, long createdAt, long lastUpdatedAt,
      EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy, String appId) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
    this.appId = appId;
  }
}
