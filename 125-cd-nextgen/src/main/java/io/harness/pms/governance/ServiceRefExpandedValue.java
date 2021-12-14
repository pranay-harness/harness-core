package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.ExpansionKeysConstants;
import io.harness.ng.core.service.entity.ServiceBasicInfo;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceElementMapper;
import io.harness.pms.sdk.core.governance.ExpandedValue;
import io.harness.serializer.JsonUtils;

import lombok.Builder;

@OwnedBy(CDC)
@Builder
public class ServiceRefExpandedValue implements ExpandedValue {
  ServiceEntity serviceEntity;

  @Override
  public String getKey() {
    return ExpansionKeysConstants.SERVICE_EXPANSION_KEY;
  }

  @Override
  public String toJson() {
    ServiceBasicInfo serviceBasicInfo = ServiceElementMapper.toBasicInfo(serviceEntity);
    return JsonUtils.asJson(serviceBasicInfo);
  }
}
