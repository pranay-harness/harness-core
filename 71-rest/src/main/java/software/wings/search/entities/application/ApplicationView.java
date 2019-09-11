package software.wings.search.entities.application;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.search.framework.EntityBaseView;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ApplicationView extends EntityBaseView {
  public ApplicationView(String uuid, String name, String description, String accountId, long createdAt,
      long lastUpdatedAt, EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
  }

  public static ApplicationView fromApplication(Application application) {
    return new ApplicationView(application.getUuid(), application.getName(), application.getDescription(),
        application.getAccountId(), application.getCreatedAt(), application.getLastUpdatedAt(), EntityType.APPLICATION,
        application.getCreatedBy(), application.getLastUpdatedBy());
  }
}
