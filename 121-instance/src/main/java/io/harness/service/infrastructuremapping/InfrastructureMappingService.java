/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.infrastructuremapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InfrastructureMappingDTO;

import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public interface InfrastructureMappingService {
  Optional<InfrastructureMappingDTO> getByInfrastructureMappingId(String infrastructureMappingId);

  Optional<InfrastructureMappingDTO> createNewOrReturnExistingInfrastructureMapping(
      InfrastructureMappingDTO infrastructureMappingDTO);
}
