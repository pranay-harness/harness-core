/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.beans.modules;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Schema(name = "AccountLicense", description = "This contains details of the Account License defined in Harness")
public class AccountLicenseDTO {
  String accountId;
  Map<ModuleType, ModuleLicenseDTO> moduleLicenses;
  Map<ModuleType, List<ModuleLicenseDTO>> allModuleLicenses;
  Long createdAt;
  Long lastUpdatedAt;
}
