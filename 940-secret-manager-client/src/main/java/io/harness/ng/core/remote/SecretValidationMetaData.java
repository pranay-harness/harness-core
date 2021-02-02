package io.harness.ng.core.remote;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.secretmanagerclient.SecretType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonTypeInfo(use = NAME, property = "type", include = EXISTING_PROPERTY, visible = true)
public abstract class SecretValidationMetaData {
  @NotNull private SecretType type;
}
