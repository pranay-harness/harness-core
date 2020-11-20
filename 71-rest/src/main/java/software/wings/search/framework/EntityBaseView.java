package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.EntityType;

/**
 * The base entity view which can be
 * used by search entities.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "EntityBaseViewKeys")
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
