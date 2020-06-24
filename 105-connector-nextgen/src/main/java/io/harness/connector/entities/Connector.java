package io.harness.connector.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.connector.common.ConnectorCategory;
import io.harness.connector.common.ConnectorType;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.entities.connectivityStatus.ConnectivityStatus;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.UniqueIndex;
import io.harness.persistence.PersistentEntity;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@FieldNameConstants(innerTypeName = "ConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@UniqueIndex(name = "unique_fullyQualifiedIdentifier", fields = { @Field(ConnectorKeys.fullyQualifiedIdentifier) })
@Document("connectors")
@TypeAlias("connectors")
// todo deepak: Add index after adding the queries
@JsonIgnoreProperties(ignoreUnknown = true)
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
