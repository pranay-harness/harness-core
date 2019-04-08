package software.wings.audit;

import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "entityAuditYamls", noClassnameStored = true)
public class EntityAuditYaml implements PersistentEntity, UuidAccess, CreatedAtAccess {
  @Id private String uuid;
  private long createdAt;
  private String entityId;
  private String yamlSha;
  private String yamlContent;
}
