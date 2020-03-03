package software.wings.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.reflections.Reflections;
import software.wings.WingsBaseTest;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration.CVConfigurationYaml;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;

import java.util.Set;

public class CVConfigurationYamlHandlerTest extends WingsBaseTest {
  @Spy private YamlHelper yamlHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CVConfigurationService cvConfigurationService;

  private DatadogCvConfigurationYamlHandler yamlHandler = new DatadogCvConfigurationYamlHandler();
  private DatadogCVServiceConfiguration cvServiceConfiguration =
      DatadogCVServiceConfiguration.builder().datadogServiceName("serviceName").build();
  private String accountId;
  private String envId;
  private String appId;
  private String configName;
  private String configId;

  @Before
  public void setUp() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);

    accountId = generateUuid();
    appId = generateUuid();
    envId = generateUuid();
    configName = "TestBaseConfig";
    configId = generateUuid();

    cvServiceConfiguration.setStateType(StateType.DATA_DOG);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(generateUuid());
    cvServiceConfiguration.setConnectorId(generateUuid());
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName(configName);
    cvServiceConfiguration.setUuid(configId);

    wingsPersistence.save(cvServiceConfiguration);

    FieldUtils.writeField(yamlHandler, "yamlHelper", yamlHelper, true);
    FieldUtils.writeField(yamlHandler, "cvConfigurationService", cvConfigurationService, true);

    doReturn(appId).when(yamlHelper).getAppId(any(), any());
    doReturn(envId).when(yamlHelper).getEnvironmentId(any(), any());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testYamlClass() {
    Reflections reflections = new Reflections("software.wings");
    Set<Class<? extends CVConfigurationYamlHandler>> yamlHandlerClasses =
        reflections.getSubTypesOf(CVConfigurationYamlHandler.class);
    assertThat(yamlHandlerClasses.size()).isGreaterThan(0);

    yamlHandlerClasses.forEach(yamlHandlerClass -> {
      try {
        assertThat(CVConfigurationYaml.class).isAssignableFrom(yamlHandlerClass.newInstance().getYamlClass());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDelete_whenCVConfigurationExists() {
    String filePath =
        "Setup/Applications/HarnessSampleApp/Environments/prod/Service Verification/" + configName + ".yaml";
    doReturn(configName).when(yamlHelper).getNameFromYamlFilePath(filePath);

    CVConfiguration originalConfig = wingsPersistence.get(CVConfiguration.class, configId);
    assertThat(originalConfig).isNotNull();

    ChangeContext<DatadogCVServiceConfiguration.DatadogCVConfigurationYaml> changeContext =
        ChangeContext.Builder.aChangeContext()
            .withChange(Change.Builder.aFileChange()
                            .withFilePath(filePath)
                            .withChangeType(ChangeType.DELETE)
                            .withAccountId(accountId)
                            .build())
            .build();
    yamlHandler.delete(changeContext);

    CVConfiguration updatedConfig = wingsPersistence.get(CVConfiguration.class, configId);
    assertThat(updatedConfig).isNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDelete_duplicateRequests() {
    String filePath =
        "Setup/Applications/HarnessSampleApp/Environments/prod/Service Verification/" + configName + ".yaml";
    doReturn(configName).when(yamlHelper).getNameFromYamlFilePath(filePath);

    CVConfiguration originalConfig = wingsPersistence.get(CVConfiguration.class, configId);
    assertThat(originalConfig).isNotNull();

    ChangeContext<DatadogCVServiceConfiguration.DatadogCVConfigurationYaml> changeContext =
        ChangeContext.Builder.aChangeContext()
            .withChange(Change.Builder.aFileChange()
                            .withFilePath(filePath)
                            .withChangeType(ChangeType.DELETE)
                            .withAccountId(accountId)
                            .build())
            .build();
    yamlHandler.delete(changeContext);

    CVConfiguration updatedConfig = wingsPersistence.get(CVConfiguration.class, configId);
    assertThat(updatedConfig).isNull();

    yamlHandler.delete(changeContext);
    updatedConfig = wingsPersistence.get(CVConfiguration.class, configId);
    assertThat(updatedConfig).isNull();
  }
}
