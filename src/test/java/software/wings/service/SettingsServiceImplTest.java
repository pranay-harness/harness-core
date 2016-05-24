package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.HostConnectionAttributes.AccessType.PASSWORD;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue;
import software.wings.service.intfc.SettingsService;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 5/23/16.
 */
public class SettingsServiceImplTest extends WingsBaseTest {
  @Inject private SettingsService settingsService;

  @Test
  public void shouldListConnectionAttributes() {
    SettingAttribute settingAttribute = settingsService.save(
        aSettingAttribute()
            .withAppId("APP_ID")
            .withName("PASSWORD")
            .withValue(aHostConnectionAttributes().withAccessType(PASSWORD).withConnectionType(SSH).build())
            .build());
    MultivaluedMap<String, String> queryMap = new MultivaluedHashMap<>();
    queryMap.put("appId", Arrays.asList("APP_ID"));

    List<SettingAttribute> connectionAttributes = settingsService.getConnectionAttributes(queryMap);
    assertThat(connectionAttributes).isNotNull();
    assertThat(((SettingValue) connectionAttributes.get(0).getValue())).isInstanceOf(HostConnectionAttributes.class);
  }

  @Test
  public void shouldListBastionHostConnectionAttributes() {
    SettingAttribute settingAttribute = settingsService.save(aSettingAttribute()
                                                                 .withAppId("APP_ID")
                                                                 .withName("PASSWORD")
                                                                 .withValue(new BastionConnectionAttributes())
                                                                 .build());
    MultivaluedMap<String, String> queryMap = new MultivaluedHashMap<>();
    queryMap.put("appId", Arrays.asList("APP_ID"));

    List<SettingAttribute> connectionAttributes = settingsService.getBastionHostAttributes(queryMap);
    assertThat(connectionAttributes).isNotNull();
    assertThat(((SettingValue) connectionAttributes.get(0).getValue())).isInstanceOf(BastionConnectionAttributes.class);
  }
}
