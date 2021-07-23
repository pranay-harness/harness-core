package io.harness.entities;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InfrastructureMappingNGKeys")
@Entity(value = "infrastructureMappingNG", noClassnameStored = true)
@Document("infrastructureMappingNG")
@StoreIn(DbAliases.NG_MANAGER)
@Persistent
@OwnedBy(HarnessTeam.DX)
public class InfrastructureMapping {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_idx")
                 .field(InfrastructureMappingNGKeys.accountIdentifier)
                 .field(InfrastructureMappingNGKeys.orgIdentifier)
                 .field(InfrastructureMappingNGKeys.projectIdentifier)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id private String id;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String infrastructureMappingType;
  private String connectorRef;
  private String envId;
  private String deploymentType;
  private String serviceId;
  @FdUniqueIndex private String infrastructureKey;
}
