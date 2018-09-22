package software.wings.yaml.handler;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Environment.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.yaml.handler.environment.EnvironmentYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceVariableService;

import java.io.IOException;

/**
 * @author rktummala on 1/9/18
 */
@SetupScheduler
public class EnvironmentYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock AppService appService;
  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject ServiceVariableService serviceVariableService;
  @InjectMocks @Inject EnvironmentService environmentService;
  @InjectMocks @Inject private EnvironmentYamlHandler yamlHandler;

  private final String APP_NAME = "app1";
  private final String ENV_NAME = "env1";
  private Environment environment;

  private String validYamlContent =
      "harnessApiVersion: '1.0'\ntype: ENVIRONMENT\ndescription: valid env yaml\nenvironmentType: NON_PROD";
  private String validYamlFilePath = "Setup/Applications/" + APP_NAME + "/Environments/" + ENV_NAME + "/Index.yaml";
  private String invalidYamlFilePath =
      "Setup/Applications/" + APP_NAME + "/EnvironmentsInvalid/" + ENV_NAME + "/Index.yaml";

  @Before
  public void setUp() throws IOException {
    environment = Environment.Builder.anEnvironment()
                      .withName(ENV_NAME)
                      .withUuid(ENV_ID)
                      .withAppId(APP_ID)
                      .withEnvironmentType(EnvironmentType.NON_PROD)
                      .withDescription("valid env yaml")
                      .build();
    when(appService.getAppByName(anyString(), anyString()))
        .thenReturn(anApplication().withName(APP_NAME).withUuid(APP_ID).build());
    when(appService.getAccountIdByAppId(anyString())).thenReturn(ACCOUNT_ID);
    when(appService.get(anyString())).thenReturn(anApplication().withName(APP_NAME).withUuid(APP_ID).build());
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.ENVIRONMENT);
    changeContext.setYamlSyncHandler(yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    Environment savedEnv = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareEnv(environment, savedEnv);

    Yaml yaml = yamlHandler.toYaml(this.environment, APP_ID);
    assertNotNull(yaml);
    assertNotNull(yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContent, yamlContent);

    Environment envFromGet = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    compareEnv(environment, envFromGet);

    //    yamlHandler.delete(changeContext);
    //
    //    Environment environment = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    //    assertNull(environment);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    // Invalid yaml path
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(invalidYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Yaml> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.ENVIRONMENT);
    changeContext.setYamlSyncHandler(yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    thrown.expect(WingsException.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }

  private void compareEnv(Environment lhs, Environment rhs) {
    assertEquals(lhs.getName(), rhs.getName());
    assertEquals(lhs.getAppId(), rhs.getAppId());
    assertEquals(lhs.getEnvironmentType(), rhs.getEnvironmentType());
    assertEquals(lhs.getDescription(), rhs.getDescription());
  }
}
