package software.wings.service.impl;

import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.SettingsService;

/**
 * Created by peeyushaggarwal on 10/25/16.
 */
public class JenkinsSettingProviderTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private SettingsService settingsService;

  @InjectMocks private JenkinsSettingProvider jenkinsSettingProvider = new JenkinsSettingProvider();

  @Test
  public void shouldGetJenkinsSettingData() throws Exception {
    jenkinsSettingProvider.getData(APP_ID);
    verify(settingsService).getSettingAttributesByType(APP_ID, SettingVariableTypes.JENKINS.name());
  }
}
