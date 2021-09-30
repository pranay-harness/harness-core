package io.harness.enforcement.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.enforcement.bases.FeatureRestriction;
import io.harness.enforcement.bases.RateLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.bases.StaticLimitRestriction;
import io.harness.enforcement.configs.FeatureRestrictionConfig;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.serializer.kryo.KryoConverterFactory;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(GTM)
public class FeatureRestrictionLoaderTest extends CategoryTest {
  FeatureRestrictionLoaderImpl featuresManagementJob;
  EnforcementServiceImpl enforcementService;
  KryoConverterFactory kryoConverterFactory;
  private final List<FeatureRestrictionConfig> featureRestrictionConfigs = new ArrayList<>();

  @Before
  public void setup() throws IllegalAccessException {
    enforcementService = mock(EnforcementServiceImpl.class);
    kryoConverterFactory = mock(KryoConverterFactory.class);
    featuresManagementJob = new FeatureRestrictionLoaderImpl(enforcementService, kryoConverterFactory);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testYamlInitialization() {
    featuresManagementJob.loadYmlToFeaturesConfig(featureRestrictionConfigs);
    for (FeatureRestrictionConfig config : featureRestrictionConfigs) {
      for (FeatureRestriction featureRestriction : config.getFeatures()) {
        featuresManagementJob.validFeatureInfo(featureRestriction);
        for (Restriction restriction : featureRestriction.getRestrictions().values()) {
          featuresManagementJob.validRestriction(restriction);

          switch (restriction.getRestrictionType()) {
            case RATE_LIMIT:
              RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
              if (!validClientDefined(rateLimitRestriction.getClientName())) {
                throw new InvalidArgumentsException(
                    String.format("Client name [%s] is not defined in clients", rateLimitRestriction.getClientName()));
              }
              break;
            case STATIC_LIMIT:
              StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
              if (!validClientDefined(staticLimitRestriction.getClientName())) {
                throw new InvalidArgumentsException(String.format(
                    "Client name [%s] is not defined in clients", staticLimitRestriction.getClientName()));
              }
              break;
            default:
              break;
          }
        }
      }
    }
  }

  private boolean validClientDefined(String clientName) {
    return featureRestrictionConfigs.stream()
        .map(config -> config.getClients())
        .flatMap(List::stream)
        .anyMatch(client -> client.getName().equals(clientName));
  }
}