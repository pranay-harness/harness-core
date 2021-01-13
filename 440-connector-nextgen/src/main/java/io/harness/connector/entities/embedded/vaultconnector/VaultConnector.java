package io.harness.connector.entities.embedded.vaultconnector;

import io.harness.connector.entities.Connector;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "VaultConnectorKeys")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.vaultconnector.VaultConnector")
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultConnector extends Connector {
  String vaultUrl;
  String secretEngineName;
  String appRoleId;
  boolean isDefault;
  boolean isReadOnly;
  AccessType accessType;
  int secretEngineVersion;

  @Getter(AccessLevel.NONE) Long renewalIntervalMinutes;

  @Getter(AccessLevel.NONE) String basePath;

  @Getter(AccessLevel.NONE) Boolean secretEngineManuallyConfigured;

  public long getRenewalIntervalMinutes() {
    if (renewalIntervalMinutes == null) {
      return 0;
    }
    return renewalIntervalMinutes;
  }

  public boolean isSecretEngineManuallyConfigured() {
    return secretEngineManuallyConfigured != null && secretEngineManuallyConfigured;
  }

  public String getBasePath() {
    return Optional.ofNullable(basePath).filter(x -> !x.isEmpty()).orElse("/harness");
  }
}
