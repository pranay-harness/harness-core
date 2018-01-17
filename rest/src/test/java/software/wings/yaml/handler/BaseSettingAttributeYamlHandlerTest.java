package software.wings.yaml.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.yaml.BaseYaml;

public abstract class BaseSettingAttributeYamlHandlerTest extends BaseYamlHandlerTest {
  protected String getYamlContentString(SettingAttribute settingAttribute, BaseYamlHandler yamlHandler) {
    BaseYaml yaml = yamlHandler.toYaml(settingAttribute, null);

    if (yaml != null) {
      String yamlContent = getYamlContent(yaml);
      if (yamlContent != null) {
        return yamlContent.substring(0, yamlContent.length() - 1);
      }
    }

    return null;
  }

  protected void verify(SettingAttribute settingAttributeSaved, SettingAttribute settingAttribute) {
    assertEquals(settingAttributeSaved.getName(), settingAttribute.getName());
    assertNotNull(settingAttributeSaved.getValue());
    assertNotNull(settingAttribute.getValue());
    assertEquals(settingAttributeSaved.getValue().toString(), settingAttribute.getValue().toString());
  }
}
