package io.harness.signup.dto;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.BillingFrequency;
import io.harness.licensing.Edition;
import io.harness.ng.core.user.SignupAction;
import io.harness.ng.core.user.UtmInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@OwnedBy(GTM)
public class SignupDTO {
  String email;
  String password;
  UtmInfo utmInfo;
  String intent;
  SignupAction signupAction;
  Edition edition;
  BillingFrequency billingFrequency;
}
