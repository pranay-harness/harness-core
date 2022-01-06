/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.account.ServiceAccountConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "AccountDTO")
@OwnedBy(PL)
@Schema(name = "Account", description = "This is the view of an Account defined in Harness")
public class AccountDTO {
  @EntityIdentifier(allowBlank = false) String identifier;
  @NGEntityName String name;
  String companyName;
  String cluster;
  DefaultExperience defaultExperience;
  AuthenticationMechanism authenticationMechanism;
  ServiceAccountConfig serviceAccountConfig;
  boolean isNextGenEnabled;

  @Builder
  public AccountDTO(String identifier, String name, String companyName, String cluster,
      DefaultExperience defaultExperience, AuthenticationMechanism authenticationMechanism,
      ServiceAccountConfig serviceAccountConfig, boolean isNextGenEnabled) {
    this.identifier = identifier;
    this.name = name;
    this.companyName = companyName;
    this.cluster = cluster;
    this.defaultExperience = defaultExperience;
    this.authenticationMechanism = authenticationMechanism;
    this.isNextGenEnabled = isNextGenEnabled;
    this.serviceAccountConfig = serviceAccountConfig;
  }
}
