package software.wings.yaml.handler;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.limits.LimitCheckerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.Application;
import software.wings.beans.Application.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.yaml.handler.app.ApplicationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;

import java.io.IOException;

/**
 * @author rktummala on 1/9/18
 */
@SetupScheduler
public class ApplicationYamlHandlerTest extends BaseYamlHandlerTest {
  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject AppService appService;
  @InjectMocks @Inject private ApplicationYamlHandler yamlHandler;
  @Mock private LimitCheckerFactory limitCheckerFactory;

  private final String APP_NAME = "app1";
  private Application application;

  private String validYamlContent = "harnessApiVersion: '1.0'\ntype: APPLICATION\ndescription: valid application yaml";
  private String validYamlFilePath = "Setup/Applications/" + APP_NAME + "/Index.yaml";
  private String invalidYamlFilePath = "Setup/ApplicationsInvalid/" + APP_NAME + "/Index.yaml";

  @Before
  public void setUp() throws IOException {
    application = Application.Builder.anApplication()
                      .withName(APP_NAME)
                      .withUuid(APP_ID)
                      .withAccountId(ACCOUNT_ID)
                      .withDescription("valid application yaml")
                      .build();
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Application.Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.APPLICATION);
    changeContext.setYamlSyncHandler(yamlHandler);

    Application.Yaml yamlObject = (Application.Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    Application savedApplication = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareApp(application, savedApplication);

    Yaml yaml = yamlHandler.toYaml(this.application, APP_ID);
    assertNotNull(yaml);
    assertNotNull(yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContent, yamlContent);

    Application applicationFromGet = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    compareApp(application, applicationFromGet);

    yamlHandler.delete(changeContext);

    Application application = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(application);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    // Invalid yaml path
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(invalidYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Application.Yaml> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.APPLICATION);
    changeContext.setYamlSyncHandler(yamlHandler);

    Application.Yaml yamlObject = (Application.Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    thrown.expect(WingsException.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }

  private void compareApp(Application lhs, Application rhs) {
    assertEquals(lhs.getName(), rhs.getName());
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.getDescription(), rhs.getDescription());
  }
}
