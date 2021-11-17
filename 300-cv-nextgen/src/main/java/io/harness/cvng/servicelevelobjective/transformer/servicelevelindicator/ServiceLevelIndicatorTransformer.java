package io.harness.cvng.servicelevelobjective.transformer.servicelevelindicator;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

public abstract class ServiceLevelIndicatorTransformer<E extends ServiceLevelIndicator, S
                                                           extends ServiceLevelIndicatorSpec> {
  public abstract E getEntity(ProjectParams projectParams, ServiceLevelIndicatorDTO serviceLevelIndicatorDTO);

  public final ServiceLevelIndicatorDTO getDTO(E serviceLevelIndicator) {
    return ServiceLevelIndicatorDTO.builder()
        .spec(getSpec(serviceLevelIndicator))
        .name(serviceLevelIndicator.getName())
        .identifier(serviceLevelIndicator.getIdentifier())
        .type(serviceLevelIndicator.getType())
        .build();
  }

  protected abstract S getSpec(E serviceLevelIndicator);
}
