/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.authenticationservice.recaptcha;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@OwnedBy(PL)
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationStatus {
  private Boolean success;
  private String hostname;
  @JsonProperty("challenge_ts") private String challengeTs;
  @JsonProperty("error-codes") private List<String> errorCodes;
}
