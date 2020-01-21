package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;

public class WorkflowYAMLHelperTest extends WingsBaseTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;

  @InjectMocks @Inject private WorkflowYAMLHelper workflowYAMLHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBeanSkip() {
    String variableValue = "random_val";
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(ACCOUNT_ID, ENV_ID, APP_ID, null, variableValue, false))
        .isEqualTo(variableValue);

    variableValue = "${srv}";
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), variableValue, false))
        .isEqualTo(variableValue);

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), null, true))
        .isNull();
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), "", true))
        .isEmpty();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.ENVIRONMENT.name(), ENV_NAME, false))
        .isNull();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBean() {
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), SERVICE_NAME, false))
        .isEqualTo(SERVICE_ID);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), SERVICE_NAME))
        .isEqualTo(SERVICE_ID);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBeanInvalidId() {
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME, false)).thenReturn(null);
    workflowYAMLHelper.getWorkflowVariableValueBean(
        ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), SERVICE_NAME, false);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBeanInvalidEntityType() {
    String value = "random_trigger_value";
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.TRIGGER.name(), value, false))
        .isEqualTo(value);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueYamlSkip() {
    String variableValue = "random_val";
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, variableValue, null, false))
        .isEqualTo(variableValue);

    variableValue = "${srv}";
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, variableValue, EntityType.SERVICE, false))
        .isEqualTo(variableValue);

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, null, EntityType.SERVICE, true)).isNull();
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, "", EntityType.SERVICE, true)).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueYaml() {
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment().name(ENV_NAME).build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, ENV_ID, EntityType.ENVIRONMENT, false))
        .isEqualTo(ENV_NAME);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, ENV_ID, EntityType.ENVIRONMENT))
        .isEqualTo(ENV_NAME);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueYamlInvalidId() {
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(null);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, ENV_ID, EntityType.ENVIRONMENT, false))
        .isEqualTo(ENV_ID);
  }
}
