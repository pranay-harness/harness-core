package software.wings.resources;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.HARNESS_BAMBOO;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;

public class SettingResourceTest extends WingsBaseTest {
  @Mock private SecretManager secretManager;
  @Mock private SettingsService settingsService;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private AccountService accountService;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks private SettingResource settingResource;

  // Tests testSaveSettingAttribute and testUpdateSettingAttribute are there to make sure that no one removes the code
  // setting the decrypted field in SettingValue to true if it comes from rest layer.
  private SettingAttribute settingAttribute;

  @Before
  public void setUp() throws IllegalAccessException {
    BambooConfig bambooConfig = BambooConfig.builder()
                                    .accountId(ACCOUNT_ID)
                                    .bambooUrl(WingsTestConstants.JENKINS_URL)
                                    .username(WingsTestConstants.USER_NAME)
                                    .build();

    settingAttribute = Builder.aSettingAttribute()
                           .withAccountId(ACCOUNT_ID)
                           .withAppId(APP_ID)
                           .withName(HARNESS_BAMBOO + System.currentTimeMillis())
                           .withCategory(SettingCategory.CLOUD_PROVIDER)
                           .withValue(bambooConfig)
                           .build();
    when(featureFlagService.isGlobalEnabled(FeatureName.PERPETUAL_TASK_SERVICE)).thenReturn(false);

    FieldUtils.writeField(settingResource, "secretManager", secretManager, true);
    FieldUtils.writeField(settingResource, "settingsService", settingsService, true);
    FieldUtils.writeField(settingResource, "usageRestrictionsService", usageRestrictionsService, true);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSaveSettingAttribute() {
    settingResource.save(APP_ID, ACCOUNT_ID, settingAttribute);

    ArgumentCaptor<SettingAttribute> argumentCaptor = ArgumentCaptor.forClass(SettingAttribute.class);
    verify(settingsService).save(argumentCaptor.capture());

    settingAttribute = argumentCaptor.getValue();
    assertThat(settingAttribute.getValue().isDecrypted()).isEqualTo(true);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testUpdateSettingAttribute() {
    settingResource.update(APP_ID, ACCOUNT_ID, settingAttribute);

    ArgumentCaptor<SettingAttribute> argumentCaptor = ArgumentCaptor.forClass(SettingAttribute.class);
    verify(settingsService).update(argumentCaptor.capture());

    settingAttribute = argumentCaptor.getValue();
    assertThat(settingAttribute.getValue().isDecrypted()).isEqualTo(true);
  }
}
