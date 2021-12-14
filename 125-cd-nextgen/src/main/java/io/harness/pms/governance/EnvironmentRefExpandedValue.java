package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.ExpansionKeysConstants;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentBasicInfo;
import io.harness.ng.core.environment.mappers.EnvironmentMapper;
import io.harness.pms.sdk.core.governance.ExpandedValue;
import io.harness.serializer.JsonUtils;

import lombok.Builder;

@OwnedBy(CDC)
@Builder
public class EnvironmentRefExpandedValue implements ExpandedValue {
  Environment environment;

  @Override
  public String getKey() {
    return ExpansionKeysConstants.ENV_EXPANSION_KEY;
  }

  @Override
  public String toJson() {
    EnvironmentBasicInfo environmentBasicInfo = EnvironmentMapper.toBasicInfo(environment);
    return JsonUtils.asJson(environmentBasicInfo);
  }
}
