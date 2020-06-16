package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.EntityYamlRecord.EntityYamlRecordKeys;

@Value
@Builder
@Entity(value = "entityYamlRecord", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "EntityYamlRecordKeys")
@Indexes({
  @Index(options = @IndexOptions(name = "index_1"), fields = {
    @Field(EntityYamlRecordKeys.accountId)
    , @Field(EntityYamlRecordKeys.entityId), @Field(EntityYamlRecordKeys.entityType),
        @Field(value = EntityYamlRecordKeys.createdAt, type = IndexType.DESC)
  })
})
public class EntityYamlRecord implements PersistentEntity, UuidAccess, CreatedAtAccess, AccountAccess {
  @Id private String uuid;
  private String accountId;
  private long createdAt;
  private String entityId;
  private String entityType;
  private String yamlPath;
  private String yamlSha;
  private String yamlContent;
}