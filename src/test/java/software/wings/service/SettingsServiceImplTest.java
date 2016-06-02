package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 5/23/16.
 */
public class SettingsServiceImplTest extends WingsBaseTest {
  @Inject private SettingsService settingsService;

  @Test
  public void shouldListConnectionAttributes() {
    SettingAttribute settingAttribute = settingsService.save(aSettingAttribute()
                                                                 .withAppId("APP_ID")
                                                                 .withName("USER_PASSWORD")
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withType(HOST_CONNECTION_ATTRIBUTES)
                                                                                .withAccessType(USER_PASSWORD)
                                                                                .withConnectionType(SSH)
                                                                                .build())
                                                                 .build());

    List<SettingAttribute> connectionAttributes =
        settingsService.getSettingAttributesByType("APP_ID", HOST_CONNECTION_ATTRIBUTES);
    assertThat(connectionAttributes).isNotNull();
    assertThat((connectionAttributes.get(0).getValue())).isInstanceOf(HostConnectionAttributes.class);
  }

  @Test
  public void shouldListBastionHostConnectionAttributes() {
    SettingAttribute settingAttribute = settingsService.save(aSettingAttribute()
                                                                 .withAppId("APP_ID")
                                                                 .withName("USER_PASSWORD")
                                                                 .withValue(new BastionConnectionAttributes())
                                                                 .build());

    List<SettingAttribute> connectionAttributes =
        settingsService.getSettingAttributesByType("APP_ID", SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES);
    assertThat(connectionAttributes).isNotNull();
    assertThat((connectionAttributes.get(0).getValue())).isInstanceOf(BastionConnectionAttributes.class);
  }
}
