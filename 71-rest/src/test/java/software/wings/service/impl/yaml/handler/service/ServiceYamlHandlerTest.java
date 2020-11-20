package software.wings.service.impl.yaml.handler.service;

import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.MILOS;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.api.DeploymentType;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.Service.Yaml;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ArtifactType;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.handler.YamlHandlerTestBase;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceYamlHandlerTest extends YamlHandlerTestBase {
  @Mock YamlHelper yamlHelper;
  @Mock HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock SecretManager secretManager;
  @Mock ServiceResourceService serviceResourceService;
  @Mock AppService appService;
  @Mock ServiceVariableService serviceVariableService;
  @Mock CustomDeploymentTypeService customDeploymentTypeService;
  @Mock AppContainerService appContainerService;
  @InjectMocks @Inject private ServiceYamlHandler serviceYamlHandler;

  private Service service;
  private String validYamlFilePath = "Setup/Applications/" + APP_NAME + "/Service/" + SERVICE_NAME + "/Index.yaml";
  private ArgumentCaptor<ServiceVariable> captor = ArgumentCaptor.forClass(ServiceVariable.class);

  @Before
  public void setUp() throws Exception {
    Application application = Builder.anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).build();
    ServiceVariable enc_1 = ServiceVariable.builder()
                                .name("enc_1")
                                .type(Type.ENCRYPTED_TEXT)
                                .encryptedValue("some-secret")
                                .accountId(ACCOUNT_ID)
                                .build();
    ServiceVariable enc_2 = ServiceVariable.builder()
                                .name("enc_2")
                                .type(Type.ENCRYPTED_TEXT)
                                .encryptedValue("other-secret")
                                .accountId(ACCOUNT_ID)
                                .build();
    ServiceVariable var_1 = ServiceVariable.builder().name("var_1").type(Type.TEXT).value("var".toCharArray()).build();
    service = Service.builder()
                  .appId(APP_ID)
                  .uuid(SERVICE_ID)
                  .name(SERVICE_NAME)
                  .accountId(ACCOUNT_ID)
                  .artifactType(ArtifactType.DOCKER)
                  .appContainer(AppContainer.Builder.anAppContainer().withName("Tomcat 8").build())
                  .deploymentType(DeploymentType.SSH)
                  .serviceVariables(Arrays.asList(enc_1, enc_2, var_1))
                  .deploymentTypeTemplateId(WingsTestConstants.TEMPLATE_ID)
                  .build();
    when(secretManager.getEncryptedYamlRef(ACCOUNT_ID, enc_1.getEncryptedValue()))
        .thenReturn("safeharness:some-secret");
    when(secretManager.getEncryptedYamlRef(ACCOUNT_ID, enc_2.getEncryptedValue())).thenReturn("amazonkms:other-secret");
    doNothing().when(harnessTagYamlHelper).updateYamlWithHarnessTagLinks(any(), any(), any());
    when(yamlHelper.getAppId(ACCOUNT_ID, validYamlFilePath)).thenReturn(APP_ID);
    when(yamlHelper.getServiceName(validYamlFilePath)).thenReturn(SERVICE_NAME);
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME)).thenReturn(service);
    when(appContainerService.getByName(anyString(), anyString()))
        .thenReturn(AppContainer.Builder.anAppContainer().withName("Tomcat 7").build());
    when(serviceResourceService.save(any(), anyBoolean(), anyBoolean()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, Service.class));
    when(serviceResourceService.update(any(), anyBoolean()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, Service.class));
    when(serviceVariableService.save(any(), anyBoolean()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, ServiceVariable.class));
    when(appService.get(APP_ID)).thenReturn(application);
    when(yamlHelper.getApplicationIfPresent(ACCOUNT_ID, validYamlFilePath)).thenReturn(Optional.of(application));
    when(yamlHelper.extractEncryptedRecordId(eq("safeharness:some-secret"), anyString())).thenReturn("some-secret");
    when(yamlHelper.extractEncryptedRecordId(eq("amazonkms:other-secret"), anyString())).thenReturn("other-secret");
    when(serviceVariableService.update(any(), anyBoolean()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, ServiceVariable.class));
    when(customDeploymentTypeService.fetchDeploymentTemplateUri(WingsTestConstants.TEMPLATE_ID))
        .thenReturn("template-uri");
    when(customDeploymentTypeService.fetchDeploymentTemplateIdFromUri(anyString(), eq("template-uri")))
        .thenReturn(WingsTestConstants.TEMPLATE_ID);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void toYaml() {
    final Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    final List<String> varNames =
        yaml.getConfigVariables().stream().map(NameValuePair.Yaml::getValue).collect(Collectors.toList());
    assertThat(varNames).containsExactlyInAnyOrder("safeharness:some-secret", "amazonkms:other-secret", "var");
    assertThat(yaml.getDeploymentTypeTemplateUri()).isEqualTo("template-uri");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void upsertFromYaml() {
    final Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);
    Service fromYaml = serviceYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(fromYaml).isNotNull();
    verify(serviceVariableService, times(3)).save(captor.capture(), anyBoolean());
    assertThat(
        captor.getAllValues().stream().map(ServiceVariable::getValue).map(String::valueOf).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("some-secret", "other-secret", "var");
    assertThat(fromYaml.getDeploymentTypeTemplateId()).isEqualTo(WingsTestConstants.TEMPLATE_ID);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdateServiceVariables() {
    when(yamlHelper.getService(APP_ID, validYamlFilePath)).thenReturn(service);

    Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);
    yaml.getConfigVariables().get(0).setValue("amazonkms:other-secret");
    Service fromYaml = serviceYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(fromYaml).isNotNull();
    verify(serviceVariableService, times(2)).update(captor.capture(), anyBoolean());
    assertThat(
        captor.getAllValues().stream().map(ServiceVariable::getValue).map(String::valueOf).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("other-secret", "other-secret");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getYamlClass() {
    assertThat(serviceYamlHandler.getYamlClass()).isEqualTo(Yaml.class);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void get() {
    when(yamlHelper.getService(APP_ID, validYamlFilePath)).thenReturn(service);
    assertThat(serviceYamlHandler.get(ACCOUNT_ID, validYamlFilePath)).isEqualTo(service);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdateHelmVersionFromYaml() {
    Service.Yaml yaml = Service.Yaml.builder().helmVersion("V2").deploymentType(DeploymentType.HELM.toString()).build();
    Service service = new Service();
    serviceYamlHandler.setHelmVersion(yaml, service);
    assertThat(service.getHelmVersion()).isEqualTo(HelmVersion.V2);

    yaml.setHelmVersion("V3");
    service.setHelmVersion(null);
    serviceYamlHandler.setHelmVersion(yaml, service);
    assertThat(service.getHelmVersion()).isEqualTo(HelmVersion.V3);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldThrowIfInvalidHelmVersion() {
    Service.Yaml yaml =
        Service.Yaml.builder().helmVersion("garbage-text").deploymentType(DeploymentType.HELM.toString()).build();
    Service service = new Service();
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> serviceYamlHandler.setHelmVersion(yaml, service))
        .withMessageContaining("helmVersion");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSetHelmVersionInYaml() {
    Service helmService =
        Service.builder().appId(APP_ID).helmVersion(HelmVersion.V2).artifactType(ArtifactType.DOCKER).build();
    Yaml yaml = serviceYamlHandler.toYaml(helmService, APP_ID);
    assertThat(yaml.getHelmVersion()).isEqualTo(HelmVersion.V2.toString());

    Service sshService = Service.builder().deploymentType(DeploymentType.SSH).artifactType(ArtifactType.JAR).build();
    yaml = serviceYamlHandler.toYaml(sshService, APP_ID);
    assertThat(yaml.getHelmVersion()).isNull();
  }

  private ChangeContext<Yaml> getChangeContext(Yaml yaml) {
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);
    ChangeContext<Yaml> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYaml(yaml);
    return changeContext;
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testAddNewServiceVariables() {
    when(yamlHelper.getService(APP_ID, validYamlFilePath)).thenReturn(service);

    Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);

    NameValuePair.Yaml newServiceVar = NameValuePair.Yaml.builder()
                                           .name("enc_3")
                                           .value("safeharness:new-secret")
                                           .valueType(Type.ENCRYPTED_TEXT.name())
                                           .build();
    yaml.getConfigVariables().add(newServiceVar);
    when(yamlHelper.extractEncryptedRecordId(eq("safeharness:new-secret"), anyString())).thenReturn("new-secret");
    Service fromYaml = serviceYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(fromYaml).isNotNull();
    verify(serviceVariableService, times(1)).save(captor.capture(), anyBoolean());
    assertThat(
        captor.getAllValues().stream().map(ServiceVariable::getValue).map(String::valueOf).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("new-secret");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testAddNewServiceVariableWithDashInName() {
    when(yamlHelper.getService(APP_ID, validYamlFilePath)).thenReturn(service);

    Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);

    NameValuePair.Yaml newServiceVar = NameValuePair.Yaml.builder()
                                           .name("enc-4")
                                           .value("safeharness:new-secret")
                                           .valueType(Type.ENCRYPTED_TEXT.name())
                                           .build();
    yaml.getConfigVariables().add(newServiceVar);
    when(yamlHelper.extractEncryptedRecordId(eq("safeharness:new-secret"), anyString())).thenReturn("new-secret");
    Service fromYaml = serviceYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(fromYaml).isNotNull();
    verify(serviceVariableService, times(1)).save(captor.capture(), anyBoolean());
    assertThatThrownBy(() -> serviceYamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Adding variable name enc-4 with hyphens (dashes) is not allowed");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnEditAppStack() {
    Service initialService = Service.builder()
                                 .appContainer(AppContainer.Builder.anAppContainer().withName("Tomcat 7").build())
                                 .deploymentType(DeploymentType.ECS)
                                 .artifactType(ArtifactType.TAR)
                                 .build();
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME)).thenReturn(initialService);
    Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);
    assertThatThrownBy(() -> serviceYamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("The 'applicationStack' can not be updated when a Service is already created.");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnEditArtifactType() {
    Service initialService = Service.builder()
                                 .appContainer(AppContainer.Builder.anAppContainer().withName("Tomcat 8").build())
                                 .deploymentType(DeploymentType.ECS)
                                 .artifactType(ArtifactType.TAR)
                                 .build();
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME)).thenReturn(initialService);
    Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);
    assertThatThrownBy(() -> serviceYamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("The 'artifactType' can not be updated when a Service is already created.");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnEditDeploymentType() {
    Service initialService = Service.builder()
                                 .appContainer(AppContainer.Builder.anAppContainer().withName("Tomcat 8").build())
                                 .deploymentType(DeploymentType.ECS)
                                 .artifactType(ArtifactType.DOCKER)
                                 .build();
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME)).thenReturn(initialService);
    Yaml yaml = serviceYamlHandler.toYaml(service, APP_ID);
    ChangeContext<Yaml> changeContext = getChangeContext(yaml);
    assertThatThrownBy(() -> serviceYamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("The 'deploymentType' can not be updated when a Service is already created.");
  }
}
