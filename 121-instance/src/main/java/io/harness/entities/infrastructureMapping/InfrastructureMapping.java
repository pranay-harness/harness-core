package io.harness.entities.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants(innerTypeName = "InfrastructureMappingKeys")
@Entity(value = "InfrastructureMapping")
// TODO check use-case of @Document
@Document("infrastructureMapping")
@OwnedBy(HarnessTeam.DX)
public abstract class InfrastructureMapping implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_account_org_project_id")
                 .unique(true)
                 .field(InfrastructureMappingKeys.accountIdentifier)
                 .field(InfrastructureMappingKeys.orgIdentifier)
                 .field(InfrastructureMappingKeys.projectIdentifier)
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
}
