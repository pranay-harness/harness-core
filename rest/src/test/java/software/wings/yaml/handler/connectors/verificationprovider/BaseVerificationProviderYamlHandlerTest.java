package software.wings.yaml.handler.connectors.verificationprovider;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.handler.BaseSettingAttributeYamlHandlerTest;

import java.io.IOException;

public abstract class BaseVerificationProviderYamlHandlerTest extends BaseSettingAttributeYamlHandlerTest {
  @InjectMocks @Inject protected SecretManager secretManager;
  @InjectMocks @Inject protected YamlHelper yamlHelper;
  @InjectMocks @Inject protected SettingsService settingsService;
  @Mock protected SettingValidationService settingValidationService;

  public static final String verificationProviderYamlDir = "Setup/Verification Providers/";
  public static final String invalidYamlPath = "Setup/Verification Providers Invalid/invalid.yaml";
  public static final String accountName = "accountName";
  public static final String userName = "username";
  public static final String password = "password";
  public static final String token = "5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11";
  public static final String apiKey = "5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11";
  public static final String acessId = "accessId";
  public static final String accesskey = "accesskey";

  protected String getYamlFilePath(String name) {
    return new StringBuilder().append(verificationProviderYamlDir).append(name).append(".yaml").toString();
  }

  protected <T extends BaseYaml> ChangeContext<T> getChangeContext(
      String yamlContent, String yamlPath, Class yamlClass, BaseYamlHandler yamlHandler) throws IOException {
    T yamlObject = (T) getYaml(yamlContent, yamlClass, false);

    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withFileContent(yamlContent)
                                      .withFilePath(yamlPath)
                                      .withAccountId(ACCOUNT_ID)
                                      .build();

    ChangeContext<T> changeContext = ChangeContext.Builder.aChangeContext()
                                         .withChange(gitFileChange)
                                         .withYamlType(YamlType.SETTING_VALUE)
                                         .withYamlSyncHandler(yamlHandler)
                                         .build();

    changeContext.setYaml(yamlObject);
    return changeContext;
  }
}
