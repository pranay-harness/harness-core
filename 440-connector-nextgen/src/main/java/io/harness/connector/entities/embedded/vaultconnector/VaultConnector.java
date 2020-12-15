package io.harness.connector.entities.embedded.vaultconnector;

import io.harness.connector.entities.Connector;
import io.harness.security.encryption.AccessType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "VaultConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.vaultconnector.VaultConnector")
public class VaultConnector extends Connector {
  String vaultUrl;
  String basePath;
  String secretEngineName;
  String appRoleId;
  boolean isDefault;
  boolean isReadOnly;
  int renewalIntervalHours;
  AccessType accessType;
  int secretEngineVersion;
}
