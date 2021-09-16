package io.harness.cvng.core.transformer.changeSource;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.PagerDutyChangeSourceSpec;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;

@OwnedBy(CV)
public class PagerDutyChangeSourceSpecTransformer
    extends ChangeSourceSpecTransformer<PagerDutyChangeSource, PagerDutyChangeSourceSpec> {
  @Override
  public PagerDutyChangeSource getEntity(ServiceEnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO) {
    PagerDutyChangeSourceSpec pagerDutyChangeSourceSpec = (PagerDutyChangeSourceSpec) changeSourceDTO.getSpec();
    return PagerDutyChangeSource.builder()
        .accountId(environmentParams.getAccountIdentifier())
        .orgIdentifier(environmentParams.getOrgIdentifier())
        .projectIdentifier(environmentParams.getProjectIdentifier())
        .serviceIdentifier(environmentParams.getServiceIdentifier())
        .envIdentifier(environmentParams.getEnvironmentIdentifier())
        .identifier(changeSourceDTO.getIdentifier())
        .name(changeSourceDTO.getName())
        .enabled(changeSourceDTO.isEnabled())
        .type(ChangeSourceType.PAGER_DUTY)
        .connectorIdentifier(pagerDutyChangeSourceSpec.getConnectorRef())
        .pagerDutyServiceId(pagerDutyChangeSourceSpec.getPagerDutyServiceId())
        .build();
  }

  @Override
  protected PagerDutyChangeSourceSpec getSpec(PagerDutyChangeSource changeSource) {
    return PagerDutyChangeSourceSpec.builder()
        .connectorRef(changeSource.getConnectorIdentifier())
        .pagerDutyServiceId(changeSource.getPagerDutyServiceId())
        .build();
  }
}
