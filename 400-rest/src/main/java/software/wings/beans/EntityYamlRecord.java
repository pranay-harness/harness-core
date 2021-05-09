package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "entityYamlRecord", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "EntityYamlRecordKeys")
@StoreIn(DbAliases.CG_MANAGER)
public class EntityYamlRecord implements PersistentEntity, UuidAccess, CreatedAtAccess, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("index_1")
                 .field(EntityYamlRecordKeys.accountId)
                 .field(EntityYamlRecordKeys.entityId)
                 .field(EntityYamlRecordKeys.entityType)
                 .descSortField(EntityYamlRecordKeys.createdAt)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("index_2")
                 .field(EntityYamlRecordKeys.accountId)
                 .field(EntityYamlRecordKeys.entityType)
                 .descSortField(EntityYamlRecordKeys.createdAt)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String accountId;
  private long createdAt;
  private String entityId;
  private String entityType;
  private String yamlPath;
  private String yamlSha;
  private String yamlContent;
}
