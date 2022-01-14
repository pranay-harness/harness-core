package io.harness.ccm.commons.entities.batch;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceMetadataRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CEMetadataRecordKeys")
@StoreIn(DbAliases.CENG)
public final class CEMetadataRecord implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder().name("accountId").field(CEMetadataRecordKeys.accountId).unique(true).build())
        .build();
  }
  @Id private String uuid;
  private String accountId;
  private Boolean clusterConnectorConfigured;
  private Boolean clusterDataConfigured;
  private Boolean awsConnectorConfigured;
  private Boolean azureConnectorConfigured;
  private Boolean awsDataPresent;
  private Boolean gcpConnectorConfigured;
  private Boolean gcpDataPresent;
  private Boolean azureDataPresent;
  private Boolean applicationDataPresent;
  private long lastUpdatedAt;
}
