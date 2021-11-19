package io.harness.cvng.core.entities.demo;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "cvngDemoDataIndexKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@Entity(value = "cvngDemoDataIndices")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.CV)
@StoreIn(DbAliases.CVNG)
public class CVNGDemoDataIndex implements AccountAccess, PersistentEntity, UuidAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_verificationTaskId_dataColWorkerId_idx")
                 .unique(true)
                 .field(cvngDemoDataIndexKeys.accountId)
                 .field(cvngDemoDataIndexKeys.verificationTaskId)
                 .field(cvngDemoDataIndexKeys.dataCollectionWorkerId)
                 .build())
        .build();
  }
  @Id private String uuid;
  String accountId;
  String verificationTaskId;
  String dataCollectionWorkerId;
  int lastIndex;
}
