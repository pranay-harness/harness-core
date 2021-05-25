package io.harness.licensing.scheduler;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_UUID;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;

public class AccountLicenseCheckHandlerImplTest extends CategoryTest {
  @InjectMocks private AccountLicenseCheckHandlerImpl accountLicenseCheckHandler;
  @Mock private MongoTemplate mongoTemplate;
  @Mock private Map<ModuleType, LicenseCheckProcessor> checkProcessorMap;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testHandleWithRegularTrial() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().totalDevelopers(10).maxDevelopers(10).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    AccountLicense accountLicense = AccountLicense.builder()
                                        .uuid(DEFAULT_ACCOUNT_UUID)
                                        .accountIdentifier(ACCOUNT_IDENTIFIER)
                                        .moduleLicenses(ImmutableMap.of(ModuleType.CI, moduleLicense))
                                        .allInactive(false)
                                        .build();

    LicenseCheckProcessor licenseCheckProcessor = mock(LicenseCheckProcessor.class);
    when(checkProcessorMap.get(any())).thenReturn(licenseCheckProcessor);
    when(licenseCheckProcessor.checkExpiry(any(), anyLong(), any()))
        .thenReturn(CheckResult.builder().isUpdated(false).allInactive(false).build());

    accountLicenseCheckHandler.handle(accountLicense);
    verifyZeroInteractions(mongoTemplate);
    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.ACTIVE);
    assertThat(accountLicense.isAllInactive()).isEqualTo(false);
    assertThat(accountLicense.getModuleLicenses().get(ModuleType.CI).getStatus()).isEqualTo(LicenseStatus.ACTIVE);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testHandleWithTrialExpire() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().totalDevelopers(10).maxDevelopers(10).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    ModuleLicense expiredModuleLicense = CDModuleLicense.builder().build();
    expiredModuleLicense.setStatus(LicenseStatus.ACTIVE);
    expiredModuleLicense.setModuleType(ModuleType.CD);
    expiredModuleLicense.setEdition(Edition.ENTERPRISE);
    expiredModuleLicense.setLicenseType(LicenseType.TRIAL);

    AccountLicense accountLicense =
        AccountLicense.builder()
            .uuid(DEFAULT_ACCOUNT_UUID)
            .accountIdentifier(ACCOUNT_IDENTIFIER)
            .moduleLicenses(ImmutableMap.of(ModuleType.CI, moduleLicense, ModuleType.CD, expiredModuleLicense))
            .allInactive(false)
            .build();

    LicenseCheckProcessor licenseCheckProcessor = mock(LicenseCheckProcessor.class);
    when(checkProcessorMap.get(any())).thenReturn(licenseCheckProcessor);
    when(licenseCheckProcessor.checkExpiry(any(), anyLong(), eq(moduleLicense)))
        .thenReturn(CheckResult.builder().isUpdated(false).allInactive(false).build());
    when(licenseCheckProcessor.checkExpiry(any(), anyLong(), eq(expiredModuleLicense)))
        .thenReturn(CheckResult.builder().isUpdated(true).allInactive(true).build());
    when(mongoTemplate.findById(eq(DEFAULT_ACCOUNT_UUID), eq(AccountLicense.class))).thenReturn(accountLicense);

    accountLicenseCheckHandler.handle(accountLicense);

    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.ACTIVE);
    assertThat(expiredModuleLicense.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    assertThat(accountLicense.isAllInactive()).isFalse();
    verify(mongoTemplate, times(1)).save(any());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testHandleWithAllTrialExpired() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().totalDevelopers(10).maxDevelopers(10).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    ModuleLicense expiredModuleLicense = CDModuleLicense.builder().build();
    expiredModuleLicense.setStatus(LicenseStatus.ACTIVE);
    expiredModuleLicense.setModuleType(ModuleType.CD);
    expiredModuleLicense.setEdition(Edition.ENTERPRISE);
    expiredModuleLicense.setLicenseType(LicenseType.TRIAL);

    AccountLicense accountLicense =
        AccountLicense.builder()
            .uuid(DEFAULT_ACCOUNT_UUID)
            .accountIdentifier(ACCOUNT_IDENTIFIER)
            .moduleLicenses(ImmutableMap.of(ModuleType.CI, moduleLicense, ModuleType.CD, expiredModuleLicense))
            .allInactive(false)
            .build();

    LicenseCheckProcessor licenseCheckProcessor = mock(LicenseCheckProcessor.class);
    when(checkProcessorMap.get(any())).thenReturn(licenseCheckProcessor);
    when(licenseCheckProcessor.checkExpiry(any(), anyLong(), eq(moduleLicense)))
        .thenReturn(CheckResult.builder().isUpdated(true).allInactive(true).build());
    when(licenseCheckProcessor.checkExpiry(any(), anyLong(), eq(expiredModuleLicense)))
        .thenReturn(CheckResult.builder().isUpdated(true).allInactive(true).build());
    when(mongoTemplate.findById(eq(DEFAULT_ACCOUNT_UUID), eq(AccountLicense.class))).thenReturn(accountLicense);

    accountLicenseCheckHandler.handle(accountLicense);

    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    assertThat(expiredModuleLicense.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    assertThat(accountLicense.isAllInactive()).isTrue();
    verify(mongoTemplate, times(1)).save(any());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testHandleWithAlreadyExpired() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().totalDevelopers(10).maxDevelopers(10).build();
    moduleLicense.setStatus(LicenseStatus.EXPIRED);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    AccountLicense accountLicense = AccountLicense.builder()
                                        .uuid(DEFAULT_ACCOUNT_UUID)
                                        .accountIdentifier(ACCOUNT_IDENTIFIER)
                                        .moduleLicenses(ImmutableMap.of(ModuleType.CI, moduleLicense))
                                        .allInactive(true)
                                        .build();

    LicenseCheckProcessor licenseCheckProcessor = mock(LicenseCheckProcessor.class);
    when(checkProcessorMap.get(any())).thenReturn(licenseCheckProcessor);
    when(licenseCheckProcessor.checkExpiry(any(), anyLong(), eq(moduleLicense)))
        .thenReturn(CheckResult.builder().isUpdated(false).allInactive(true).build());

    accountLicenseCheckHandler.handle(accountLicense);
    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    assertThat(accountLicense.isAllInactive()).isTrue();
    verifyZeroInteractions(mongoTemplate);
  }
}
