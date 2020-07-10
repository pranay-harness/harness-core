package io.harness.connector.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.connector.entities.embedded.ConnectivityStatus;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.PersistentEntity;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@FieldNameConstants(innerTypeName = "ConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
// todo deepak: Add index after adding the queries
@CdUniqueIndex(name = "unique_fullyQualifiedIdentifier", fields = { @Field("fullyQualifiedIdentifier") })
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("connectors")
public abstract class Connector implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;

  // todo deepak: Where we should keep the scope, it will be used by everyone
  @NotEmpty Scope scope;
  String description;
  @Trimmed @NotEmpty String accountId;
  @Trimmed String orgId;
  @Trimmed String projectId;
  @NotEmpty String fullyQualifiedIdentifier;
  @NotEmpty ConnectorType type;
  @NotEmpty List<ConnectorCategory> categories;
  @NotEmpty ConnectivityStatus status;

  @NotNull @Singular @Size(max = 128) List<String> tags;
  // todo deepak: Add createdBy once user entity is decided
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  public enum Scope { ACCOUNT, PROJECT, ORGANIZATION }
}
