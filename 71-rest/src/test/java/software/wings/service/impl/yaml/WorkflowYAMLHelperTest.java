package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.HINGER;
import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.INFRA_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
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
import software.wings.beans.Variable;
import software.wings.beans.security.UserGroup;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.settings.SettingVariableTypes;

public class WorkflowYAMLHelperTest extends WingsBaseTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock private UserGroupService userGroupService;
  @Mock private SettingsService settingsService;

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
    Variable variable = aVariable().name("test").build();
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, null, variableValue, false, variable))
        .isEqualTo(variableValue);

    variableValue = "${srv}";
    variable = aVariable().name("test").entityType(EntityType.SERVICE).build();
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), variableValue, false, variable))
        .isEqualTo(variableValue);

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), null, true, variable))
        .isNull();
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), "", true, variable))
        .isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBean() {
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).build());

    Variable variable = aVariable().name("test").entityType(EntityType.SERVICE).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), SERVICE_NAME, false, variable))
        .isEqualTo(SERVICE_ID);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), SERVICE_NAME, variable))
        .isEqualTo(SERVICE_ID);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBeanInvalidId() {
    Variable variable = aVariable().name("test").entityType(EntityType.SERVICE).build();

    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME, false)).thenReturn(null);
    workflowYAMLHelper.getWorkflowVariableValueBean(
        ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), SERVICE_NAME, false, variable);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueBeanInvalidEntityType() {
    String value = "random_trigger_value";
    Variable variable = aVariable().name("test").entityType(EntityType.TRIGGER).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.TRIGGER.name(), value, false, variable))
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
  public void shouldGetWorkflowVariableValueEnvironment() {
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment().name(ENV_NAME).build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, ENV_ID, EntityType.ENVIRONMENT, false))
        .isEqualTo(ENV_NAME);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, ENV_ID, EntityType.ENVIRONMENT))
        .isEqualTo(ENV_NAME);

    when(environmentService.getEnvironmentByName(APP_ID, ENV_NAME, false))
        .thenReturn(Environment.Builder.anEnvironment().uuid(ENV_ID).build());

    Variable variable = aVariable().name("test").entityType(EntityType.ENVIRONMENT).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, null, APP_ID, EntityType.ENVIRONMENT.name(), ENV_NAME, false, variable))
        .isEqualTo(ENV_ID);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, null, APP_ID, EntityType.ENVIRONMENT.name(), ENV_NAME, variable))
        .isEqualTo(ENV_ID);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableValueYamlInvalidId() {
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(null);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, ENV_ID, EntityType.ENVIRONMENT, false))
        .isEqualTo(ENV_ID);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableYAMLService() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, SERVICE_ID, EntityType.SERVICE, false))
        .isEqualTo(SERVICE_NAME);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, SERVICE_ID, EntityType.SERVICE))
        .isEqualTo(SERVICE_NAME);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableYAMLInfraMapping() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(aPhysicalInfrastructureMapping().withName(INFRA_NAME).build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(
                   APP_ID, INFRA_MAPPING_ID, EntityType.INFRASTRUCTURE_MAPPING, false))
        .isEqualTo(INFRA_NAME);
    assertThat(
        workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, INFRA_MAPPING_ID, EntityType.INFRASTRUCTURE_MAPPING))
        .isEqualTo(INFRA_NAME);

    when(infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(aPhysicalInfrastructureMapping().withName(INFRA_NAME).withUuid(INFRA_MAPPING_ID).build());

    Variable variable = aVariable().name("test").entityType(EntityType.INFRASTRUCTURE_MAPPING).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.INFRASTRUCTURE_MAPPING.name(), INFRA_NAME, false, variable))
        .isEqualTo(INFRA_MAPPING_ID);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.INFRASTRUCTURE_MAPPING.name(), INFRA_NAME, variable))
        .isEqualTo(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableYAMLInfraDefinitions() {
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(InfrastructureDefinition.builder().name(INFRA_NAME).build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(
                   APP_ID, INFRA_DEFINITION_ID, EntityType.INFRASTRUCTURE_DEFINITION, false))
        .isEqualTo(INFRA_NAME);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(
                   APP_ID, INFRA_DEFINITION_ID, EntityType.INFRASTRUCTURE_DEFINITION))
        .isEqualTo(INFRA_NAME);

    when(infrastructureDefinitionService.getInfraDefByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(InfrastructureDefinition.builder().name(INFRA_NAME).uuid(INFRA_DEFINITION_ID).build());

    Variable variable = aVariable().name("test").entityType(EntityType.INFRASTRUCTURE_DEFINITION).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(ACCOUNT_ID, ENV_ID, APP_ID,
                   EntityType.INFRASTRUCTURE_DEFINITION.name(), INFRA_NAME, false, variable))
        .isEqualTo(INFRA_DEFINITION_ID);
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.INFRASTRUCTURE_DEFINITION.name(), INFRA_NAME, variable))
        .isEqualTo(INFRA_DEFINITION_ID);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableYAMLMultipleInfraDefinitions() {
    when(infrastructureDefinitionService.get(APP_ID, "infra1"))
        .thenReturn(InfrastructureDefinition.builder().name("infra1Name").build());
    when(infrastructureDefinitionService.get(APP_ID, "infra2"))
        .thenReturn(InfrastructureDefinition.builder().name("infra2Name").build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(
                   APP_ID, "infra1,infra2", EntityType.INFRASTRUCTURE_DEFINITION, false))
        .isEqualTo("infra1Name,infra2Name");
    assertThat(
        workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, "infra1, infra2", EntityType.INFRASTRUCTURE_DEFINITION))
        .isEqualTo("infra1Name,infra2Name");

    when(infrastructureDefinitionService.getInfraDefByName(APP_ID, ENV_ID, "infra1Name"))
        .thenReturn(InfrastructureDefinition.builder().name("infra1Name").uuid("infra1").build());

    when(infrastructureDefinitionService.getInfraDefByName(APP_ID, ENV_ID, "infra2Name"))
        .thenReturn(InfrastructureDefinition.builder().name("infra2Name").uuid("infra2").build());

    Variable variable =
        aVariable().name("test").allowMultipleValues(true).entityType(EntityType.INFRASTRUCTURE_DEFINITION).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(ACCOUNT_ID, ENV_ID, APP_ID,
                   EntityType.INFRASTRUCTURE_DEFINITION.name(), "infra2Name,infra1Name", false, variable))
        .isEqualTo("infra2,infra1");
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(ACCOUNT_ID, ENV_ID, APP_ID,
                   EntityType.INFRASTRUCTURE_DEFINITION.name(), " infra2Name , infra1Name ", variable))
        .isEqualTo("infra2,infra1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableYAMLMultipleUserGroups() {
    when(userGroupService.get("ug1")).thenReturn(UserGroup.builder().name("userGroup1").build());
    when(userGroupService.get("ug2")).thenReturn(UserGroup.builder().name("userGroup2").build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, "ug1,ug2", EntityType.USER_GROUP, false))
        .isEqualTo("userGroup1,userGroup2");
    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, " ug1 , ug2 ", EntityType.USER_GROUP))
        .isEqualTo("userGroup1,userGroup2");

    when(userGroupService.fetchUserGroupByName(ACCOUNT_ID, "userGroup1"))
        .thenReturn(UserGroup.builder().uuid("ug1").build());

    when(userGroupService.fetchUserGroupByName(ACCOUNT_ID, "userGroup2"))
        .thenReturn(UserGroup.builder().uuid("ug2").build());

    Variable variable = aVariable().name("test").allowMultipleValues(true).entityType(EntityType.USER_GROUP).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.USER_GROUP.name(), "userGroup2,userGroup1", false, variable))
        .isEqualTo("ug2,ug1");
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.USER_GROUP.name(), " userGroup2 , userGroup1 ", variable))
        .isEqualTo("ug2,ug1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetWorkflowVariableYAMLSSHConnectionAttribute() {
    when(settingsService.get("ssh-con-id")).thenReturn(aSettingAttribute().withName("SSH").build());

    assertThat(workflowYAMLHelper.getWorkflowVariableValueYaml(
                   APP_ID, "ssh-con-id", EntityType.SS_SSH_CONNECTION_ATTRIBUTE, false))
        .isEqualTo("SSH");
    assertThat(
        workflowYAMLHelper.getWorkflowVariableValueYaml(APP_ID, "ssh-con-id", EntityType.SS_SSH_CONNECTION_ATTRIBUTE))
        .isEqualTo("SSH");

    when(
        settingsService.fetchSettingAttributeByName(ACCOUNT_ID, "SSH", SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES))
        .thenReturn(aSettingAttribute().withUuid("ssh-con-id").build());

    Variable variable = aVariable().name("test").entityType(EntityType.SS_SSH_CONNECTION_ATTRIBUTE).build();

    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SS_SSH_CONNECTION_ATTRIBUTE.name(), "SSH", false, variable))
        .isEqualTo("ssh-con-id");
    assertThat(workflowYAMLHelper.getWorkflowVariableValueBean(
                   ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SS_SSH_CONNECTION_ATTRIBUTE.name(), "SSH", variable))
        .isEqualTo("ssh-con-id");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldThrowErrorWhenVariableValueBlank() {
    Variable variable = aVariable().name("test").entityType(EntityType.SS_SSH_CONNECTION_ATTRIBUTE).build();

    workflowYAMLHelper.getWorkflowVariableValueBean(
        ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SS_SSH_CONNECTION_ATTRIBUTE.name(), "    ", false, variable);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void showThrowErrorWhenEmptyCustomExpressionUsed() {
    Variable variable = aVariable().name("test").entityType(EntityType.SERVICE).build();

    workflowYAMLHelper.getWorkflowVariableValueBean(
        ACCOUNT_ID, ENV_ID, APP_ID, EntityType.SERVICE.name(), "${}", false, variable);
  }
}
