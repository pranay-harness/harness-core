package software.wings.search.entities.environment;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.search.framework.EntityBaseView;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "EnvironmentViewKeys")
public class EnvironmentView extends EntityBaseView {
  private String appId;
  private String appName;
  private EnvironmentType environmentType;

  public EnvironmentView(String uuid, String name, String description, String accountId, long createdAt,
      long lastUpdatedAt, EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy, String appId,
      EnvironmentType environmentType) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
    this.appId = appId;
    this.environmentType = environmentType;
  }

  public static EnvironmentView fromEnvironment(Environment environment) {
    return new EnvironmentView(environment.getUuid(), environment.getName(), environment.getDescription(),
        environment.getAccountId(), environment.getCreatedAt(), environment.getLastUpdatedAt(), EntityType.ENVIRONMENT,
        environment.getCreatedBy(), environment.getLastUpdatedBy(), environment.getAppId(),
        environment.getEnvironmentType());
  }
}