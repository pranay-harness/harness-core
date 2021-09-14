/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.feature.EnforcementConfiguration;
import io.harness.feature.bases.Feature;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.example.RateLimitExampleImpl;
import io.harness.feature.example.StaticLimitExampleImpl;
import io.harness.feature.interfaces.RateLimitInterface;
import io.harness.feature.interfaces.StaticLimitInterface;
import io.harness.licensing.Edition;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(GTM)
public class FeatureLoaderTest extends CategoryTest {
  FeatureLoaderImpl featuresManagementJob;
  FeatureServiceImpl featureService;
  Injector injector;

  @Before
  public void setup() throws IllegalAccessException {
    featureService = mock(FeatureServiceImpl.class);
    injector = mock(Injector.class);
    featuresManagementJob = new FeatureLoaderImpl(featureService,
        EnforcementConfiguration.builder()
            .featureYmlPaths(Lists.newArrayList("features/example_features.yml"))
            .build());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testFeaturesManagementJobInitialization() throws ClassNotFoundException {
    Class<StaticLimitInterface> staticImplClass =
        (Class<StaticLimitInterface>) Class.forName("io.harness.feature.example.StaticLimitExampleImpl");
    Class<RateLimitInterface> rateImplClass =
        (Class<RateLimitInterface>) Class.forName("io.harness.feature.example.RateLimitExampleImpl");
    when(injector.getInstance(staticImplClass)).thenReturn(new StaticLimitExampleImpl());
    when(injector.getInstance(rateImplClass)).thenReturn(new RateLimitExampleImpl());
    featuresManagementJob.run(injector);

    ArgumentCaptor<String> featureNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Feature> featureCaptor = ArgumentCaptor.forClass(Feature.class);
    verify(featureService, times(3)).registerFeature(featureNameCaptor.capture(), featureCaptor.capture());

    List<String> allFeatureNames = featureNameCaptor.getAllValues();
    assertThat(allFeatureNames.get(0)).isEqualTo("TEST1");
    assertThat(allFeatureNames.get(1)).isEqualTo("TEST2");
    assertThat(allFeatureNames.get(2)).isEqualTo("TEST3");

    List<Feature> allFeatures = featureCaptor.getAllValues();
    assertThat(allFeatures.get(0).getModuleType()).isEqualTo(ModuleType.CD);
    assertThat(allFeatures.get(0).getRestrictions().get(Edition.FREE).getRestrictionType())
        .isEqualTo(RestrictionType.ENABLED);
    assertThat(allFeatures.get(0).getRestrictions().get(Edition.TEAM).getRestrictionType())
        .isEqualTo(RestrictionType.ENABLED);
    assertThat(allFeatures.get(0).getRestrictions().get(Edition.ENTERPRISE).getRestrictionType())
        .isEqualTo(RestrictionType.ENABLED);

    assertThat(allFeatures.get(1).getRestrictions().get(Edition.FREE).getRestrictionType())
        .isEqualTo(RestrictionType.ENABLED);
    assertThat(allFeatures.get(1).getRestrictions().get(Edition.TEAM).getRestrictionType())
        .isEqualTo(RestrictionType.STATIC_LIMIT);
    assertThat(allFeatures.get(1).getRestrictions().get(Edition.ENTERPRISE).getRestrictionType())
        .isEqualTo(RestrictionType.ENABLED);

    assertThat(allFeatures.get(2).getRestrictions().get(Edition.FREE).getRestrictionType())
        .isEqualTo(RestrictionType.RATE_LIMIT);
    assertThat(allFeatures.get(2).getRestrictions().get(Edition.TEAM).getRestrictionType())
        .isEqualTo(RestrictionType.RATE_LIMIT);
    assertThat(allFeatures.get(2).getRestrictions().get(Edition.ENTERPRISE).getRestrictionType())
        .isEqualTo(RestrictionType.ENABLED);
  }
}
