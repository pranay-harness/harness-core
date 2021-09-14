package io.harness.feature.services.impl;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.Restriction;
import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.beans.EnableDisableRestrictionDTO;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.FeatureUsageDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.CDLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CILicenseSummaryDTO;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;

public class FeatureServiceImplTest extends CategoryTest {
  FeatureServiceImpl featureService;
  private LicenseService licenseService;
  private RestrictionHandlerFactory restrictionHandlerFactory;
  Feature feature;
  Feature featureNotEnabled;
  Feature ciFeature;
  FeatureUsageClient featureUsageClient;
  private static final FeatureRestriction FEATURE_NAME = FeatureRestriction.TEST1;
  private static final FeatureRestriction FEATURE_NAME_STATIC = FeatureRestriction.TEST2;
  private static final FeatureRestriction CI_FEATURE_NAME = FeatureRestriction.TEST3;
  private static final String ACCOUNT_ID = "1";

  @Before
  public void setup() throws IOException {
    restrictionHandlerFactory = new RestrictionHandlerFactory();
    licenseService = mock(LicenseService.class);

    featureUsageClient = mock(FeatureUsageClient.class);
    Call<ResponseDTO<FeatureUsageDTO>> featureUsageCall = mock(Call.class);
    when(featureUsageCall.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(FeatureUsageDTO.builder().count(10).build())));
    when(featureUsageClient.getFeatureUsage(any(), any())).thenReturn(featureUsageCall);

    featureService = new FeatureServiceImpl(licenseService, restrictionHandlerFactory);
    feature = new Feature(FEATURE_NAME, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new EnableDisableRestriction(RestrictionType.AVAILABILITY, true))
            .build());
    featureNotEnabled = new Feature(FEATURE_NAME_STATIC, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new StaticLimitRestriction(RestrictionType.STATIC_LIMIT, 2, featureUsageClient))
            .put(Edition.TEAM, new EnableDisableRestriction(RestrictionType.AVAILABILITY, false))
            .put(Edition.ENTERPRISE, new EnableDisableRestriction(RestrictionType.AVAILABILITY, true))
            .build());
    ciFeature = new Feature(CI_FEATURE_NAME, "description", ModuleType.CI,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new EnableDisableRestriction(RestrictionType.AVAILABILITY, false))
            .build());
    featureService.registerFeature(FEATURE_NAME, feature);
    featureService.registerFeature(FEATURE_NAME_STATIC, featureNotEnabled);
    featureService.registerFeature(CI_FEATURE_NAME, ciFeature);

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
    boolean result = featureService.isFeatureAvailable(FEATURE_NAME, ACCOUNT_ID);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckAvailabilityOrThrowSucceed() {
    featureService.checkAvailabilityOrThrow(FEATURE_NAME, ACCOUNT_ID);
  }

  @Test(expected = LimitExceededException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckAvailabilityOrThrowFailed() {
    featureService.checkAvailabilityOrThrow(FEATURE_NAME_STATIC, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetFeatureDetails() {
    FeatureDetailsDTO dto = FeatureDetailsDTO.builder()
                                .name(FEATURE_NAME)
                                .description("description")
                                .moduleType(ModuleType.CD.name())
                                .allowed(true)
                                .restrictionType(RestrictionType.AVAILABILITY)
                                .restriction(EnableDisableRestrictionDTO.builder().enabled(true).build())
                                .build();
    FeatureDetailsDTO featureDetail = featureService.getFeatureDetail(FEATURE_NAME, ACCOUNT_ID);
    assertThat(featureDetail).isEqualTo(dto);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetEnabledFeatures() {
    List<FeatureDetailsDTO> result = featureService.getEnabledFeatureDetails(ACCOUNT_ID);
    assertThat(result.size()).isEqualTo(1);
  }
}
