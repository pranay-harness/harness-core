package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.HARNESS_BAMBOO;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;

public class SettingResourceTest extends WingsBaseTest {
  @Mock private SecretManager secretManager;
  @Mock private SettingsService settingsService;
  @Mock private UsageRestrictionsService usageRestrictionsService;

  @InjectMocks private SettingResource settingResource;

  // Tests testSaveSettingAttribute and testUpdateSettingAttribute are there to make sure that no one removes the code
  // setting the decrypted field in SettingValue to true if it comes from rest layer.
  private SettingAttribute settingAttribute;

  @Before
  public void setUp() {
    BambooConfig bambooConfig = BambooConfig.builder()
                                    .accountId(ACCOUNT_ID)
                                    .bambooUrl(WingsTestConstants.JENKINS_URL)
                                    .username(WingsTestConstants.USER_NAME)
                                    .build();

    settingAttribute = Builder.aSettingAttribute()
                           .withAccountId(ACCOUNT_ID)
                           .withAppId(APP_ID)
                           .withName(HARNESS_BAMBOO + System.currentTimeMillis())
                           .withCategory(Category.CLOUD_PROVIDER)
                           .withValue(bambooConfig)
                           .build();

    setInternalState(settingResource, "secretManager", secretManager);
    setInternalState(settingResource, "settingsService", settingsService);
    setInternalState(settingResource, "usageRestrictionsService", usageRestrictionsService);
  }

  @Test
  public void testSaveSettingAttribute() {
    settingResource.save(APP_ID, ACCOUNT_ID, settingAttribute);

    ArgumentCaptor<SettingAttribute> argumentCaptor = ArgumentCaptor.forClass(SettingAttribute.class);
    verify(settingsService).save(argumentCaptor.capture());

    settingAttribute = argumentCaptor.getValue();
    assertThat(settingAttribute.getValue().isDecrypted()).isEqualTo(true);
  }

  @Test
  public void testUpdateSettingAttribute() {
    settingResource.update(APP_ID, ACCOUNT_ID, settingAttribute);

    ArgumentCaptor<SettingAttribute> argumentCaptor = ArgumentCaptor.forClass(SettingAttribute.class);
    verify(settingsService).update(argumentCaptor.capture());

    settingAttribute = argumentCaptor.getValue();
    assertThat(settingAttribute.getValue().isDecrypted()).isEqualTo(true);
  }
}
