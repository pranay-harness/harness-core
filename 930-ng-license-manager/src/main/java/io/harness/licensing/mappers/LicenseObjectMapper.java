/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.ModuleLicense;

@OwnedBy(HarnessTeam.GTM)
public interface LicenseObjectMapper<T extends ModuleLicense, K extends ModuleLicenseDTO> {
  K toDTO(T moduleLicense);

  T toEntity(K moduleLicenseDTO);
}
