package software.wings.beans;

import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "entityYamlRecord", noClassnameStored = true)
public class EntityYamlRecord implements PersistentEntity, UuidAccess, CreatedAtAccess {
  @Id private String uuid;
  private long createdAt;
  private String entityId;
  private String entityType;
  private String yamlSha;
  private String yamlContent;
}