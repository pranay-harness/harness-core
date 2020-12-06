package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.graphql.datafetcher.connector.ConnectorsController.WEBHOOK_URL_PATH;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.UrlType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.graphql.schema.type.connector.QLGitConnector.QLGitConnectorBuilder;
import software.wings.helpers.ext.url.SubdomainUrlHelper;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ConnectorsControllerTest extends CategoryTest {
  @Mock SubdomainUrlHelper subdomainUrlHelper;
  @InjectMocks ConnectorsController connectorsController;

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testConnectorImplementations() {
    doReturn("BaseApiUrl").when(subdomainUrlHelper).getApiBaseUrl(any());
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
        connectorsController.getConnectorBuilder(attribute);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testPopulatedGitConnectorBuilder() {
    String webhookToken = "webhookToken";
    String accountId = "12345";
    String baseApiUrl = "BaseApiUrl/";
    doReturn(baseApiUrl).when(subdomainUrlHelper).getApiBaseUrl(accountId);
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setAccountId(accountId);
    GitConfig gitConfig = GitConfig.builder()
                              .username("testUsername")
                              .password(new String("testPassword").toCharArray())
                              .branch("testBranch")
                              .authorEmailId("email")
                              .webhookToken(webhookToken)
                              .urlType(UrlType.REPO)
                              .build();
    settingAttribute.setValue(gitConfig);
    QLGitConnectorBuilder qlGitConnectorBuilder =
        connectorsController.getPrePopulatedGitConnectorBuilder(settingAttribute);
    QLGitConnector qlGitConnector = qlGitConnectorBuilder.build();
    assertThat(qlGitConnector.getUserName()).isEqualTo("testUsername");
    assertThat(qlGitConnector.getPasswordSecretId()).isEqualTo("testPassword");
    assertThat(qlGitConnector.getBranch()).isEqualTo("testBranch");
    assertThat(qlGitConnector.getCustomCommitDetails().getAuthorEmailId()).isEqualTo("email");
    assertThat(qlGitConnector.getWebhookUrl())
        .isEqualTo(baseApiUrl + WEBHOOK_URL_PATH + webhookToken + "?accountId=" + accountId);
    assertThat(qlGitConnector.getUrlType()).isEqualTo(UrlType.REPO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCheckIfInputIsNotPresent() {
    connectorsController.checkInputExists(QLConnectorType.GIT, null);
  }
}
