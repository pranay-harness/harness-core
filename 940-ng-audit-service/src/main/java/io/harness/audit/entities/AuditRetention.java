package io.harness.audit.entities;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AuditRetentionKeys")
@Entity(value = "auditRetentions", noClassnameStored = true)
@Document("auditRetentions")
@TypeAlias("auditRetentions")
@StoreIn(DbAliases.AUDITS)
public class AuditRetention {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotBlank String accountId;
  @NotNull Long retentionPeriodInMonths;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditRetentionUniqueIdx")
                 .field(AuditRetentionKeys.accountId)
                 .unique(true)
                 .build())
        .build();
  }
}
