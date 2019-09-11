package software.wings.search.framework;

import io.harness.beans.EmbeddedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.EntityType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityBaseView {
  private String id;
  private String name;
  private String description;
  private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private EntityType type;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}