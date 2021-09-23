package io.harness.feature.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.feature.client.custom.CustomFeatureRestriction;
import io.harness.feature.client.usage.PlanFeatureUsageInterface;
import io.harness.feature.constants.FeatureRestriction;

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
public class PlanFeatureRegisterConfiguration {
  Map<FeatureRestriction, Class<? extends PlanFeatureUsageInterface>> usageImplRegistrars;
  Map<FeatureRestriction, Class<? extends CustomFeatureRestriction>> customFeatureRestrictionImplRegistrars;
}
