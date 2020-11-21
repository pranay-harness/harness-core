package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import software.wings.beans.EntityYamlRecord.EntityYamlRecordKeys;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "entityYamlRecord", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "EntityYamlRecordKeys")
@CdIndex(name = "index_1",
    fields =
    {
      @Field(EntityYamlRecordKeys.accountId)
      , @Field(EntityYamlRecordKeys.entityId), @Field(EntityYamlRecordKeys.entityType),
          @Field(value = EntityYamlRecordKeys.createdAt, type = IndexType.DESC)
    })
@CdIndex(name = "index_2",
    fields =
    {
      @Field(EntityYamlRecordKeys.accountId)
      , @Field(EntityYamlRecordKeys.entityType), @Field(value = EntityYamlRecordKeys.createdAt, type = IndexType.DESC),
          @Field(EntityYamlRecordKeys.yamlPath)
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
