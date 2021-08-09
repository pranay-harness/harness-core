package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.entities.modules.CEModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class CELicenseObjectMapper implements LicenseObjectMapper<CEModuleLicense, CEModuleLicenseDTO> {
  @Override
  public CEModuleLicenseDTO toDTO(CEModuleLicense entity) {
    return CEModuleLicenseDTO.builder().spendLimit(entity.getSpendLimit()).build();
  }

  @Override
  public CEModuleLicense toEntity(CEModuleLicenseDTO dto) {
    return CEModuleLicense.builder().spendLimit(dto.getSpendLimit()).build();
  }
}
