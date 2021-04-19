package io.harness.ng.core.entitysetupusage.dto;

import io.harness.ng.core.EntityDetail;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntitySetupUsageDTO {
  String accountIdentifier;
  EntityDetail referredEntity;
  @NotNull EntityDetail referredByEntity;
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = SecretReferredByConnectorSetupUsageDetail.class, name = "SECRET_REFERRED_BY_CONNECTOR")
  })
  SetupUsageDetail detail;
  Long createdAt;
}
