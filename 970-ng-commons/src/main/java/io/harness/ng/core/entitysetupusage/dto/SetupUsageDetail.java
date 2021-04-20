package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(DX)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
@JsonSubTypes({
  @JsonSubTypes.
  Type(value = SecretReferredByConnectorSetupUsageDetail.class, name = "SecretReferredByConnectorSetupUsageDetail")
})
public interface SetupUsageDetail {}
