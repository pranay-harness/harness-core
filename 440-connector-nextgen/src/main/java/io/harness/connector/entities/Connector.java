package io.harness.connector.entities;

import io.harness.beans.EmbeddedUser;
import io.harness.connector.entities.ConnectorConnectivityDetails.ConnectorConnectivityDetailsKeys;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants(innerTypeName = "ConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@NgUniqueIndex(name = "unique_fullyQualifiedIdentifier", fields = { @Field("fullyQualifiedIdentifier") })
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("connectors")
public abstract class Connector implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;
  // todo deepak: Where we should keep the scope, it will be used by everyone
  @NotEmpty io.harness.encryption.Scope scope;
  String description;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  @NotEmpty String fullyQualifiedIdentifier;
  @NotEmpty ConnectorType type;
  @NotEmpty List<ConnectorCategory> categories;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  @CreatedBy private EmbeddedUser createdBy;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  ConnectorConnectivityDetails connectivityDetails;
  Boolean deleted = Boolean.FALSE;
  String heartbeatPerpetualTaskId;

  @Override
  public String getAccountIdentifier() {
    return accountIdentifier;
  }

  public static final String CONNECTOR_COLLECTION_NAME = "connectors";

  @UtilityClass
  public static final class ConnectorKeys {
    public static final String connectionStatus =
        ConnectorKeys.connectivityDetails + "." + ConnectorConnectivityDetailsKeys.status;
  }
}
