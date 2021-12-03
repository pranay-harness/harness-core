package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.DefaultExperience;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@Schema(
    name = "GatewayAccountRequest", description = "This is the view of Gateway Account Request as defined in Harness.")
public class GatewayAccountRequestDTO {
  String uuid;
  String accountName;
  String companyName;
  DefaultExperience defaultExperience;
  boolean createdFromNG;
  boolean isNextGenEnabled;
}
