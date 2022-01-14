package io.harness.enforcement.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestrictionUsageRegisterConfiguration {
  Map<FeatureRestrictionName, Class<? extends RestrictionUsageInterface>> restrictionNameClassMap;
}
