package software.wings.graphql.datafetcher.connector;

import static junit.framework.TestCase.fail;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

public class ConnectorControllerTest {
  @Test
  @Category(UnitTests.class)
  public void testConnectorImplementations() {
    SettingAttribute attribute = new SettingAttribute();
    List<SettingVariableTypes> settingVariableTypes = SettingCategory.CONNECTOR.getSettingVariableTypes();
    settingVariableTypes.addAll(SettingCategory.HELM_REPO.getSettingVariableTypes());
    try {
      for (SettingVariableTypes types : settingVariableTypes) {
        SettingValue settingValue = Mockito.mock(SettingValue.class);
        Mockito.when(settingValue.getSettingType()).thenReturn(types);
        attribute.setValue(settingValue);
        ConnectorsController.getConnectorBuilder(attribute);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
