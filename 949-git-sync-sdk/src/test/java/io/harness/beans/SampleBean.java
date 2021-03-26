package io.harness.beans;

import io.harness.EntityType;
import io.harness.encryption.Scope;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.EntityDetail;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Entity(value = "sampleBean", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("sampleBean")
public class SampleBean implements UuidAware, PersistentEntity, GitSyncableEntity {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String test1;
  String branch;
  String accountId;
  String projectIdentifier;
  String orgIdentifier;
  String identifier;
  String name;

  @Override
  public EntityType getEntityType() {
    return EntityType.CONNECTORS;
  }

  @Override
  public EntityDetail getEntityDetail() {
    return EntityDetail.builder()
        .entityRef(IdentifierRef.builder()
                       .identifier(identifier)
                       .accountIdentifier(accountId)
                       .orgIdentifier(orgIdentifier)
                       .projectIdentifier(projectIdentifier)
                       .scope(Scope.PROJECT)
                       .build())
        .type(EntityType.CONNECTORS)
        .name(name)
        .build();
  }
}
