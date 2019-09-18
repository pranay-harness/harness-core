package software.wings.search.entities.service;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.search.framework.EntityBaseView;
import software.wings.utils.ArtifactType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ServiceViewKeys")
public class ServiceView extends EntityBaseView {
  private String appId;
  private String appName;
  private ArtifactType artifactType;
  private DeploymentType deploymentType;

  public ServiceView(String uuid, String name, String description, String accountId, long createdAt, long lastUpdatedAt,
      EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy, String appId,
      ArtifactType artifactType, DeploymentType deploymentType) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
    this.appId = appId;
    this.artifactType = artifactType;
    this.deploymentType = deploymentType;
  }

  public static ServiceView fromService(Service service) {
    return new ServiceView(service.getUuid(), service.getName(), service.getDescription(), service.getAccountId(),
        service.getCreatedAt(), service.getLastUpdatedAt(), EntityType.SERVICE, service.getCreatedBy(),
        service.getLastUpdatedBy(), service.getAppId(), service.getArtifactType(), service.getDeploymentType());
  }
}
