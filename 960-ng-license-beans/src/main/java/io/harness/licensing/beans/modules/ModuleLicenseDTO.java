/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.beans.modules;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.GTM)
@SuperBuilder
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "moduleType", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = CDModuleLicenseDTO.class, name = "CD")
      , @JsonSubTypes.Type(value = CIModuleLicenseDTO.class, name = "CI"),
          @JsonSubTypes.Type(value = CEModuleLicenseDTO.class, name = "CE"),
          @JsonSubTypes.Type(value = CVModuleLicenseDTO.class, name = "CV"),
          @JsonSubTypes.Type(value = CFModuleLicenseDTO.class, name = "CF"),
    })
public abstract class ModuleLicenseDTO {
  String id;
  String accountIdentifier;
  ModuleType moduleType;
  Edition edition;
  LicenseType licenseType;
  LicenseStatus status;
  long startTime;
  long expiryTime;
  Long createdAt;
  Long lastModifiedAt;
}
