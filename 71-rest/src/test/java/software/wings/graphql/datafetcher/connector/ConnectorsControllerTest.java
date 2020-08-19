package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.TMACARI;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.UrlType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.graphql.schema.type.connector.QLGitConnector.QLGitConnectorBuilder;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectorsControllerTest extends CategoryTest {
  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testConnectorImplementations() {
    SettingAttribute attribute = new SettingAttribute();
    List<SettingVariableTypes> settingVariableTypes = SettingCategory.CONNECTOR.getSettingVariableTypes();
    settingVariableTypes.addAll(SettingCategory.HELM_REPO.getSettingVariableTypes());
    Map<SettingVariableTypes, Class> settingVariableTypesClassMapping = new HashMap<>();
    settingVariableTypesClassMapping.put(SettingVariableTypes.GIT, GitConfig.class);
    try {
      for (SettingVariableTypes types : settingVariableTypes) {
        SettingValue settingValue;
        if (settingVariableTypesClassMapping.get(types) != null) {
          settingValue = (SettingValue) Mockito.mock(settingVariableTypesClassMapping.get(types));
        } else {
          settingValue = Mockito.mock(SettingValue.class);
        }
        Mockito.when(settingValue.getSettingType()).thenReturn(types);
        attribute.setValue(settingValue);
        ConnectorsController.getConnectorBuilder(attribute);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testPopulatedGitConnectorBuilder() {
    SettingAttribute settingAttribute = new SettingAttribute();
    GitConfig gitConfig = GitConfig.builder()
                              .username("testUsername")
                              .password(new String("testPassword").toCharArray())
                              .branch("testBranch")
                              .authorEmailId("email")
                              .urlType(UrlType.REPO)
                              .build();
    settingAttribute.setValue(gitConfig);
    QLGitConnectorBuilder qlGitConnectorBuilder =
        ConnectorsController.getPrePopulatedGitConnectorBuilder(settingAttribute);
    QLGitConnector qlGitConnector = qlGitConnectorBuilder.build();
    assertThat(qlGitConnector.getUserName()).isEqualTo("testUsername");
    assertThat(qlGitConnector.getPasswordSecretId()).isEqualTo("testPassword");
    assertThat(qlGitConnector.getBranch()).isEqualTo("testBranch");
    assertThat(qlGitConnector.getCustomCommitDetails().getAuthorEmailId()).isEqualTo("email");
    assertThat(qlGitConnector.getUrlType()).isEqualTo(UrlType.REPO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCheckIfInputIsNotPresent() {
    ConnectorsController.checkIfInputIsNotPresent(QLConnectorType.GIT, null);
  }
}
