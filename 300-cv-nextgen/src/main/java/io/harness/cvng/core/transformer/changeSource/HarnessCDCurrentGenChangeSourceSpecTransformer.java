package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.HarnessCDCurrentGenChangeSourceSpec;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.HarnessCDCurrentGenChangeSource;

public class HarnessCDCurrentGenChangeSourceSpecTransformer
    extends ChangeSourceSpecTransformer<HarnessCDCurrentGenChangeSource, HarnessCDCurrentGenChangeSourceSpec> {
  @Override
  public HarnessCDCurrentGenChangeSource getEntity(
      ServiceEnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO) {
    HarnessCDCurrentGenChangeSourceSpec harnessCDCurrentGenChangeSourceSpec =
        (HarnessCDCurrentGenChangeSourceSpec) changeSourceDTO.getSpec();
    return HarnessCDCurrentGenChangeSource.builder()
        .accountId(environmentParams.getAccountIdentifier())
        .orgIdentifier(environmentParams.getOrgIdentifier())
        .projectIdentifier(environmentParams.getProjectIdentifier())
        .serviceIdentifier(environmentParams.getServiceIdentifier())
        .envIdentifier(environmentParams.getEnvironmentIdentifier())
        .identifier(changeSourceDTO.getIdentifier())
        .name(changeSourceDTO.getName())
        .enabled(changeSourceDTO.isEnabled())
        .type(ChangeSourceType.HARNESS_CD_CURRENT_GEN)
        .harnessApplicationId(harnessCDCurrentGenChangeSourceSpec.getHarnessApplicationId())
        .harnessServiceId(harnessCDCurrentGenChangeSourceSpec.getHarnessServiceId())
        .harnessEnvironmentId(harnessCDCurrentGenChangeSourceSpec.getHarnessEnvironmentId())
        .build();
  }

  @Override
  protected HarnessCDCurrentGenChangeSourceSpec getSpec(HarnessCDCurrentGenChangeSource changeSource) {
    return HarnessCDCurrentGenChangeSourceSpec.builder()
        .harnessApplicationId(changeSource.getHarnessApplicationId())
        .harnessServiceId(changeSource.getHarnessServiceId())
        .harnessEnvironmentId(changeSource.getHarnessEnvironmentId())
        .build();
  }
}
