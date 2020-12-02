package io.harness.connector.entities;

import io.harness.beans.EmbeddedUser;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
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
@Builder
@FieldNameConstants(innerTypeName = "ConnectorFilterKeys")
@Entity(value = "connectorFilters", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("connectorFilters")
public class ConnectorFilter implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty String accountIdentifier;
  @NotEmpty String name;
  @NotEmpty String identifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotEmpty String fullyQualifiedIdentifier;
  String searchTerm;
  List<String> connectorNames;
  List<String> connectorIdentifier;
  List<String> descriptions;
  List<ConnectorType> types;
  List<Scope> scopes;
  List<ConnectorCategory> categories;
  List<ConnectivityStatus> connectivityStatuses;
  Boolean inheritingCredentialsFromDelegate;
  @CreatedBy private EmbeddedUser createdBy;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_filter_idx")
                 .unique(true)
                 .field(ConnectorFilterKeys.accountIdentifier)
                 .field(ConnectorFilterKeys.identifier)
                 .build())
        .build();
  }
}
