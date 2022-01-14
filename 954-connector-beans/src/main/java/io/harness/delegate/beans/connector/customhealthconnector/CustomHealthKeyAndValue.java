package io.harness.delegate.beans.connector.customhealthconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("CustomHealthKeyAndValue")
public class CustomHealthKeyAndValue implements DecryptableEntity {
  @NotNull String key;
  @NotNull boolean isValueEncrypted;
  @SecretReference SecretRefData encryptedValueRef;
  String value;
}
