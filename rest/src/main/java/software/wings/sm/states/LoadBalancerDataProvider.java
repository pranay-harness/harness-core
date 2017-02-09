package software.wings.sm.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 2/9/17.
 */

@Singleton
public class LoadBalancerDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    Map<String, String> elbMap = new HashMap<>();
    List<SettingAttribute> settingAttributesByType =
        settingsService.getSettingAttributesByType(appId, SettingVariableTypes.ELB.name());

    if (settingAttributesByType == null && settingAttributesByType.isEmpty()) {
      return new HashMap<>();
    }

    settingAttributesByType.forEach(settingAttribute -> {
      ElasticLoadBalancerConfig elasticLoadBalancerConfig = (ElasticLoadBalancerConfig) settingAttribute.getValue();
      elbMap.put(settingAttribute.getUuid(), elasticLoadBalancerConfig.getLoadBalancerName());
    });
    return elbMap;
  }
}
