package software.wings.service.impl;

import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.service.impl.appdynamics.AppDynamicsSettingProvider;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by peeyushaggarwal on 11/10/16.
 */
public class AppDynamicsSettingProviderTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private SettingsService settingsService;

  @InjectMocks private AppDynamicsSettingProvider appDynamicsSettingProvider = new AppDynamicsSettingProvider();

  @Test
  public void shouldGetAppDynamicsSettingData() throws Exception {
    appDynamicsSettingProvider.getData(APP_ID);
    verify(settingsService).getSettingAttributesByType(APP_ID, SettingVariableTypes.APP_DYNAMICS.name());
  }
}
