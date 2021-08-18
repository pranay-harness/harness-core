package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.core.beans.EnvironmentParams;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.HarnessCDNGChangeSourceSpec;
import io.harness.cvng.core.entities.changeSource.HarnessCDNGChangeSource;
import io.harness.cvng.core.types.ChangeSourceType;

public class HarnessCDNGChangeSourceSpecTransformer
    extends ChangeSourceSpecTransformer<HarnessCDNGChangeSource, HarnessCDNGChangeSourceSpec> {
  @Override
  public HarnessCDNGChangeSource getEntity(EnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO) {
    return HarnessCDNGChangeSource.builder()
        .accountId(environmentParams.getProjectParams().getAccountIdentifier())
        .orgIdentifier(environmentParams.getProjectParams().getOrgIdentifier())
        .projectIdentifier(environmentParams.getProjectParams().getProjectIdentifier())
        .serviceIdentifier(environmentParams.getServiceIdentifier())
        .envIdentifier(environmentParams.getEnvIdentifier())
        .identifier(changeSourceDTO.getIdentifier())
        .enabled(changeSourceDTO.isEnabled())
        .description(changeSourceDTO.getDescription())
        .type(ChangeSourceType.HARNESS_CDNG)
        .build();
  }

  @Override
  protected HarnessCDNGChangeSourceSpec getSpec(HarnessCDNGChangeSource changeSource) {
    return new HarnessCDNGChangeSourceSpec();
  }
}
