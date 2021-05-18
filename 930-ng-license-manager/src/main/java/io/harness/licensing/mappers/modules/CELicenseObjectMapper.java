package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CEModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

@OwnedBy(HarnessTeam.GTM)
public class CELicenseObjectMapper implements LicenseObjectMapper {
  @Override
  public ModuleLicenseDTO toDTO(ModuleLicense moduleLicense) {
    CEModuleLicense entity = (CEModuleLicense) moduleLicense;
    return CEModuleLicenseDTO.builder()
        .numberOfCluster(entity.getNumberOfCluster())
        .spendLimit(entity.getSpendLimit())
        .dataRetentionInDays(entity.getDataRetentionInDays())
        .build();
  }

  @Override
  public ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    CEModuleLicenseDTO dto = (CEModuleLicenseDTO) moduleLicenseDTO;
    return CEModuleLicense.builder()
        .numberOfCluster(dto.getNumberOfCluster())
        .spendLimit(dto.getSpendLimit())
        .dataRetentionInDays(dto.getDataRetentionInDays())
        .build();
  }
}
