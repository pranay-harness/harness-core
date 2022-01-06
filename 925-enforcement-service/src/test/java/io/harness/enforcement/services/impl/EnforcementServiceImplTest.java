/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.enforcement.services.impl;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.enforcement.bases.AvailabilityRestriction;
import io.harness.enforcement.bases.FeatureRestriction;
import io.harness.enforcement.bases.RateLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.bases.StaticLimitRestriction;
import io.harness.enforcement.beans.FeatureRestrictionUsageDTO;
import io.harness.enforcement.beans.TimeUnit;
import io.harness.enforcement.beans.details.AvailabilityRestrictionDTO;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.exceptions.LimitExceededException;
import io.harness.enforcement.handlers.RestrictionHandlerFactory;
import io.harness.enforcement.handlers.impl.AvailabilityRestrictionHandler;
import io.harness.enforcement.handlers.impl.CustomRestrictionHandler;
import io.harness.enforcement.handlers.impl.DurationRestrictionHandler;
import io.harness.enforcement.handlers.impl.LicenseRateLimitRestrictionHandler;
import io.harness.enforcement.handlers.impl.LicenseStaticLimitRestrictionHandler;
import io.harness.enforcement.handlers.impl.RateLimitRestrictionHandler;
import io.harness.enforcement.handlers.impl.StaticLimitRestrictionHandler;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.CDLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CILicenseSummaryDTO;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;

public class EnforcementServiceImplTest extends CategoryTest {
  EnforcementServiceImpl enforcementService;
  private LicenseService licenseService;
  private RestrictionHandlerFactory restrictionHandlerFactory;
  FeatureRestriction featureRestriction;
  FeatureRestriction featureStaticLimit;
  FeatureRestriction featureRateLimit;
  FeatureRestriction ciFeatureRestriction;
  EnforcementSdkClient enforcementSdkClient;
  private static final FeatureRestrictionName FEATURE_NAME = FeatureRestrictionName.TEST1;
  private static final FeatureRestrictionName FEATURE_NAME_STATIC = FeatureRestrictionName.TEST2;
  private static final FeatureRestrictionName CI_FEATURE_NAME = FeatureRestrictionName.TEST3;
  private static final FeatureRestrictionName FEATURE_NAME_RATE = FeatureRestrictionName.TEST4;
  private static final String ACCOUNT_ID = "1";

  @Before
  public void setup() throws IOException {
    licenseService = mock(LicenseService.class);
    restrictionHandlerFactory = new RestrictionHandlerFactory(new AvailabilityRestrictionHandler(),
        new StaticLimitRestrictionHandler(), new RateLimitRestrictionHandler(), new CustomRestrictionHandler(),
        new DurationRestrictionHandler(), new LicenseRateLimitRestrictionHandler(licenseService),
        new LicenseStaticLimitRestrictionHandler(licenseService));

    enforcementSdkClient = mock(EnforcementSdkClient.class);
    Call<ResponseDTO<FeatureRestrictionUsageDTO>> featureUsageCall = mock(Call.class);
    when(featureUsageCall.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(FeatureRestrictionUsageDTO.builder().count(10).build())));
    when(enforcementSdkClient.getRestrictionUsage(any(), any(), any())).thenReturn(featureUsageCall);

    enforcementService = new EnforcementServiceImpl(licenseService, restrictionHandlerFactory);
    featureRestriction = new FeatureRestriction(FEATURE_NAME, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new AvailabilityRestriction(RestrictionType.AVAILABILITY, true))
            .build());
    featureStaticLimit = new FeatureRestriction(FEATURE_NAME_STATIC, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(
                Edition.FREE, new StaticLimitRestriction(RestrictionType.STATIC_LIMIT, 10, false, enforcementSdkClient))
            .put(Edition.TEAM, new AvailabilityRestriction(RestrictionType.AVAILABILITY, false))
            .put(Edition.ENTERPRISE, new AvailabilityRestriction(RestrictionType.AVAILABILITY, true))
            .build());

    TimeUnit timeUnit = new TimeUnit(ChronoUnit.DAYS, 1);
    featureRateLimit = new FeatureRestriction(FEATURE_NAME_RATE, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE,
                new RateLimitRestriction(RestrictionType.RATE_LIMIT, 10, timeUnit, true, enforcementSdkClient))
            .build());
    ciFeatureRestriction = new FeatureRestriction(CI_FEATURE_NAME, "description", ModuleType.CI,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new AvailabilityRestriction(RestrictionType.AVAILABILITY, false))
            .build());
    enforcementService.registerFeature(FEATURE_NAME, featureRestriction);
    enforcementService.registerFeature(FEATURE_NAME_STATIC, featureStaticLimit);
    enforcementService.registerFeature(FEATURE_NAME_RATE, featureRateLimit);
    enforcementService.registerFeature(CI_FEATURE_NAME, ciFeatureRestriction);

    when(licenseService.getLicenseSummary(any(), eq(ModuleType.CD)))
        .thenReturn(CDLicenseSummaryDTO.builder()
                        .maxExpiryTime(Long.MAX_VALUE)
                        .edition(Edition.FREE)
                        .moduleType(ModuleType.CD)
                        .build());
    when(licenseService.getLicenseSummary(any(), eq(ModuleType.CI)))
        .thenReturn(CILicenseSummaryDTO.builder()
                        .maxExpiryTime(Long.MAX_VALUE)
                        .edition(Edition.FREE)
                        .moduleType(ModuleType.CI)
                        .build());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testIsFeatureAvailable() {
    boolean result = enforcementService.isFeatureAvailable(FEATURE_NAME, ACCOUNT_ID);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testIsFeatureAvailableWithStaticExceed() {
    boolean result = enforcementService.isFeatureAvailable(FEATURE_NAME_STATIC, ACCOUNT_ID);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testIsFeatureAvailableWithRateNotExceed() {
    boolean result = enforcementService.isFeatureAvailable(FEATURE_NAME_RATE, ACCOUNT_ID);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckAvailabilityOrThrowSucceed() {
    enforcementService.checkAvailabilityOrThrow(FEATURE_NAME, ACCOUNT_ID);
  }

  @Test(expected = LimitExceededException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckAvailabilityOrThrowFailed() {
    enforcementService.checkAvailabilityOrThrow(FEATURE_NAME_STATIC, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetFeatureDetails() {
    FeatureRestrictionDetailsDTO dto = FeatureRestrictionDetailsDTO.builder()
                                           .name(FEATURE_NAME)
                                           .description("description")
                                           .moduleType(ModuleType.CD)
                                           .allowed(true)
                                           .restrictionType(RestrictionType.AVAILABILITY)
                                           .restriction(AvailabilityRestrictionDTO.builder().enabled(true).build())
                                           .build();
    FeatureRestrictionDetailsDTO featureDetail = enforcementService.getFeatureDetail(FEATURE_NAME, ACCOUNT_ID);
    assertThat(featureDetail).isEqualTo(dto);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetEnabledFeatures() {
    List<FeatureRestrictionDetailsDTO> result = enforcementService.getEnabledFeatureDetails(ACCOUNT_ID);
    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testAllFeatureRestrictionMetadata() {
    List<FeatureRestrictionMetadataDTO> result = enforcementService.getAllFeatureRestrictionMetadata();
    assertThat(result.size()).isEqualTo(4);
  }
}
