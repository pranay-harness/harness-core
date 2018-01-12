package software.wings.yaml.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.Application;
import software.wings.beans.Application.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.yaml.handler.app.ApplicationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author rktummala on 1/9/18
 */
@SetupScheduler
public class ApplicationYamlHandlerTest extends BaseYamlHandlerTest {
  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject AppService appService;
  @InjectMocks @Inject private ApplicationYamlHandler yamlHandler;

  private final String APP_NAME = "app1";
  private Application application;

  private String validYamlContent = "description: valid application yaml\ntype: APPLICATION";
  private String validYamlFilePath = "Setup/Applications/" + APP_NAME + "/Index.yaml";
  private String invalidYamlContent = "description1: valid application yaml\ntype: APPLICATION";
  private String invalidYamlFilePath = "Setup/ApplicationsInvalid/" + APP_NAME + "/Index.yaml";

  @Before
  public void setUp() throws IOException {
    //    File validYamlFile = new
    //    File(getClass().getClassLoader().getResource("./yaml/handler/app/valid.yaml").getFile()); validYamlContent =
    //    new String(Files.readAllBytes(Paths.get(validYamlFile.getCanonicalPath())));
    application = Application.Builder.anApplication()
                      .withName(APP_NAME)
                      .withUuid(APP_ID)
                      .withAccountId(ACCOUNT_ID)
                      .withDescription("valid application yaml")
                      .build();
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Application.Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.APPLICATION);
    changeContext.setYamlSyncHandler(yamlHandler);

    Application.Yaml yamlObject = (Application.Yaml) getYaml(validYamlContent, Yaml.class, false);
    changeContext.setYaml(yamlObject);

    Application savedApplication = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
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
    GitFileChange gitFileChange = spy(GitFileChange.class);
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(invalidYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Application.Yaml> changeContext = spy(ChangeContext.class);
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.APPLICATION);
    changeContext.setYamlSyncHandler(yamlHandler);

    Application.Yaml yamlObject = (Application.Yaml) getYaml(validYamlContent, Yaml.class, false);
    changeContext.setYaml(yamlObject);

    try {
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (WingsException ex) {
    }

    // Invalid yaml content
    gitFileChange.setFileContent(invalidYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);

    try {
      yamlObject = (Application.Yaml) getYaml(invalidYamlContent, Yaml.class, false);
      changeContext.setYaml(yamlObject);

      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (UnrecognizedPropertyException ex) {
    }
  }

  private void compareApp(Application lhs, Application rhs) {
    assertEquals(lhs.getName(), rhs.getName());
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.getDescription(), rhs.getDescription());
  }
}
