package io.harness.signup.dto;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.BillingFrequency;
import io.harness.licensing.Edition;
import io.harness.ng.core.user.SignupAction;
import io.harness.ng.core.user.UtmInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@OwnedBy(GTM)
public class SignupInviteDTO {
  String email;
  String passwordHash;
  String intent;
  SignupAction signupAction;
  Edition edition;
  BillingFrequency billingFrequency;
  UtmInfo utmInfo;
  boolean createdFromNG;
  boolean completed;
}
