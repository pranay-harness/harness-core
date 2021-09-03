package io.harness.feature.helpers.impl;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.summary.CDLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CELicenseSummaryDTO;
import io.harness.licensing.beans.summary.CILicenseSummaryDTO;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.remote.NgLicenseHttpClient;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;

public class CentralizedLicenseHelperTest extends CategoryTest {
  @InjectMocks CentralizedLicenseHelper centralizedLicenseHelper;
  @Mock NgLicenseHttpClient ngLicenseHttpClient;
  private CILicenseSummaryDTO ciLicenseSummaryDTO;
  private CELicenseSummaryDTO ceLicenseSummaryDTO;
  private CDLicenseSummaryDTO cdExpiredLicenseSummaryDTO;
  private static final String ACCOUNT_ID = "account_id";

  @Before
  public void setup() throws IOException {
    initMocks(this);
    ciLicenseSummaryDTO = CILicenseSummaryDTO.builder()
                              .totalDevelopers(10)
                              .moduleType(ModuleType.CI)
                              .maxExpiryTime(Long.MAX_VALUE)
                              .edition(Edition.TEAM)
                              .licenseType(LicenseType.TRIAL)
                              .build();
    Call<ResponseDTO<LicensesWithSummaryDTO>> ciSummaryCall = mock(Call.class);
    when(ciSummaryCall.execute()).thenReturn(Response.success(ResponseDTO.newResponse(ciLicenseSummaryDTO)));
    when(ngLicenseHttpClient.getLicenseSummary(ACCOUNT_ID, ModuleType.CI.name())).thenReturn(ciSummaryCall);

    ceLicenseSummaryDTO = CELicenseSummaryDTO.builder()
                              .totalSpendLimit(100)
                              .moduleType(ModuleType.CE)
                              .maxExpiryTime(Long.MAX_VALUE)
                              .edition(Edition.FREE)
                              .licenseType(LicenseType.TRIAL)
                              .build();
    Call<ResponseDTO<LicensesWithSummaryDTO>> ceSummaryCall = mock(Call.class);
    when(ceSummaryCall.execute()).thenReturn(Response.success(ResponseDTO.newResponse(ceLicenseSummaryDTO)));
    when(ngLicenseHttpClient.getLicenseSummary(ACCOUNT_ID, ModuleType.CE.name())).thenReturn(ceSummaryCall);

    cdExpiredLicenseSummaryDTO = CDLicenseSummaryDTO.builder()
                                     .totalWorkload(20)
                                     .moduleType(ModuleType.CE)
                                     .maxExpiryTime(0)
                                     .edition(Edition.ENTERPRISE)
                                     .licenseType(LicenseType.TRIAL)
                                     .build();
    Call<ResponseDTO<LicensesWithSummaryDTO>> cdSummaryCall = mock(Call.class);
    when(cdSummaryCall.execute()).thenReturn(Response.success(ResponseDTO.newResponse(cdExpiredLicenseSummaryDTO)));
    when(ngLicenseHttpClient.getLicenseSummary(ACCOUNT_ID, ModuleType.CD.name())).thenReturn(cdSummaryCall);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetLicenseSummaryWithCIModule() {
    ciLicenseSummaryDTO.setMaxExpiryTime(Long.MAX_VALUE);
    ceLicenseSummaryDTO.setMaxExpiryTime(Long.MAX_VALUE);

    Optional<LicensesWithSummaryDTO> licenseSummary =
        centralizedLicenseHelper.getLicenseSummary(ACCOUNT_ID, ModuleType.CI);
    assertThat(licenseSummary.isPresent()).isTrue();
    assertThat(licenseSummary.get()).isEqualTo(ciLicenseSummaryDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetLicenseSummaryWithCoreModule() {
    ciLicenseSummaryDTO.setMaxExpiryTime(Long.MAX_VALUE);
    ceLicenseSummaryDTO.setMaxExpiryTime(Long.MAX_VALUE);

    Optional<LicensesWithSummaryDTO> licenseSummary =
        centralizedLicenseHelper.getLicenseSummary(ACCOUNT_ID, ModuleType.CORE);
    assertThat(licenseSummary.isPresent()).isTrue();
    assertThat(licenseSummary.get()).isEqualTo(ciLicenseSummaryDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetLicenseSummaryWithCoreModuleAllInvalid() {
    ciLicenseSummaryDTO.setMaxExpiryTime(0);
    ceLicenseSummaryDTO.setMaxExpiryTime(0);

    Optional<LicensesWithSummaryDTO> licenseSummary =
        centralizedLicenseHelper.getLicenseSummary(ACCOUNT_ID, ModuleType.CORE);
    assertThat(licenseSummary.isPresent()).isFalse();
  }
}
