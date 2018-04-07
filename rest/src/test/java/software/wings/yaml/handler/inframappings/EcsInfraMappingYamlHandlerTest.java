package software.wings.yaml.handler.inframappings;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.amazonaws.services.ecs.model.LaunchType;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.Key;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping.Yaml;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.HarnessException;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.EcsInfraMappingYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class EcsInfraMappingYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock protected SettingsService settingsService;
  @Mock protected ServiceResourceService serviceResourceService;
  @Mock protected ServiceTemplateService serviceTemplateService;
  @Mock protected AppService appService;
  @Mock protected EnvironmentService environmentService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private ContainerService containerService;
  @Mock @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Mock private YamlDirectoryService yamlDirectoryService;

  @InjectMocks @Inject protected YamlHelper yamlHelper;
  @InjectMocks @Inject protected InfrastructureMappingService infrastructureMappingService;
  @InjectMocks @Inject protected EcsInfraMappingYamlHandler yamlHandler;

  private String validYamlContent1 = "harnessApiVersion: '1.0'\n"
      + "type: AWS_ECS\n"
      + "assignPublicIp: false\n"
      + "cluster: ec2ecs\n"
      + "computeProviderName: ecs-infra\n"
      + "computeProviderType: AWS\n"
      + "deploymentType: ECS\n"
      + "infraMappingType: AWS_ECS\n"
      + "launchType: EC2\n"
      + "region: us-east-1\n"
      + "serviceName: dockersvc";

  private String validYamlContent2 = "harnessApiVersion: '1.0'\n"
      + "type: AWS_ECS\n"
      + "assignPublicIp: true\n"
      + "cluster: ABfargate\n"
      + "computeProviderName: ecs-infra\n"
      + "computeProviderType: AWS\n"
      + "deploymentType: ECS\n"
      + "infraMappingType: AWS_ECS\n"
      + "launchType: FARGATE\n"
      + "region: us-east-1\n"
      + "securityGroupIds: sg-4e8f0c38\n"
      + "serviceName: dockersvc\n"
      + "subnetIds: subnet-2fa27920,subnet-ad9b92c9\n"
      + "vpcId: vpc-bfff4dc4";

  private String invalidYamlContent = "InvalidharnessApiVersion: '1.0'\n"
      + "type: AWS_ECS\n"
      + "assignPublicIp: true\n"
      + "cluster: ABfargate\n"
      + "NocomputeProviderName: aws\n"
      + "computeProviderType: AWS\n"
      + "deploymentType: ECS\n"
      + "infraMappingType: AWS_ECS\n"
      + "launchType: FARGATE\n"
      + "region: us-east-1\n"
      + "securityGroupIds: sg-4e8f0c38\n"
      + "serviceName: fargate\n"
      + "subnetIds: subnet-2fa27920,subnet-ad9b92c9\n"
      + "vpcId: vpc-bfff4dc4";

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENVIRONMENT_NAME/Service Infrastructure/ecs.yaml";
  private String infraMappingName = "ecs";
  private String serviceName = "dockersvc";
  private String computeProviderName = "ecs-infra";
  private ServiceTemplate serviceTemplate =
      ServiceTemplate.Builder.aServiceTemplate().withUuid("uuid").withName("name").build();
  private SettingAttribute settingAttribute = getSettingAttribute();

  @Before
  public void runBeforeTest() {
    setup();
  }

  private void setup() {
    MockitoAnnotations.initMocks(this);

    when(settingsService.getByName(anyString(), anyString(), anyString())).thenReturn(settingAttribute);
    when(settingsService.get(anyString())).thenReturn(settingAttribute);
    when(appService.get(anyString())).thenReturn(getApplication());
    when(appService.getAppByName(anyString(), anyString())).thenReturn(getApplication());
    when(environmentService.getEnvironmentByName(anyString(), anyString())).thenReturn(getEnvironment());
    when(containerService.validate(anyObject())).thenReturn(true);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(containerService);
    when(serviceResourceService.getServiceByName(anyString(), anyString())).thenReturn(getService());
    when(serviceResourceService.get(anyString(), anyString())).thenReturn(getService());
    when(serviceTemplateService.getTemplateRefKeysByService(anyString(), anyString(), anyString()))
        .thenReturn(asList(new Key(ServiceTemplate.class, "serviceTemplates", SERVICE_ID)));
    when(serviceTemplateService.get(anyString(), anyString())).thenReturn(serviceTemplate);
  }

  @Test
  public void tbsValidateNetworkParameters() throws Exception {
    Yaml yaml = Yaml.builder()
                    .assignPublicIp(true)
                    .cluster(CLUSTER_NAME)
                    .computeProviderName(COMPUTE_PROVIDER_ID)
                    .launchType(LaunchType.FARGATE.name())
                    .build();
    EcsInfrastructureMapping ecsInfrastructureMapping =
        anEcsInfrastructureMapping().withAppId(APP_ID).withName("name").build();

    String errorMsg = "";
    try {
      errorMsg = (String) MethodUtils.invokeMethod(
          yamlHandler, true, "validateNetworkParameters", new Object[] {yaml, ecsInfrastructureMapping});
    } catch (Exception e) {
      assertEquals(((InvocationTargetException) e).getTargetException().getMessage(),
          new StringBuilder()
              .append("Failed to parse yaml for EcsInfraMapping: ")
              .append("name")
              .append(", App: ")
              .append(APP_ID)
              .append(
                  ", For Fargate Lauch type, VpcId  -  SubnetIds  - SecurityGroupIds are required, can not be blank")
              .toString());
    }

    yaml = Yaml.builder()
               .assignPublicIp(true)
               .cluster(CLUSTER_NAME)
               .computeProviderName(COMPUTE_PROVIDER_ID)
               .launchType(LaunchType.FARGATE.name())
               .vpcId("vpcId")
               .subnetIds("subnetId1, subnetId2")
               .securityGroupIds("sgId1")
               .build();
  }

  private Service getService() {
    return Service.Builder.aService()
        .withName(serviceName)
        .withAppId(APP_ID)
        .withUuid(SERVICE_ID)
        .withArtifactType(ArtifactType.DOCKER)
        .build();
  }

  private Environment getEnvironment() {
    return Environment.Builder.anEnvironment().withUuid("ANY_UUID").withName("ENV_NAME").build();
  }

  private Application getApplication() {
    return Application.Builder.anApplication()
        .withUuid("ANY_UUID")
        .withName(APP_NAME)
        .withAccountId(ACCOUNT_ID)
        .build();
  }

  private SettingAttribute getSettingAttribute() {
    return aSettingAttribute()
        .withName(computeProviderName)
        .withUuid(SETTING_ID)
        .withValue(AwsConfig.builder().accessKey(ACCESS_KEY).secretKey(SECRET_KEY).accountId(ACCOUNT_ID).build())
        .build();
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    // testCrud(validYamlContent1);
    testCrud(validYamlContent2);
  }

  private void testCrud(String validYamlContent) throws IOException, HarnessException {
    ChangeContext<Yaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class, false);
    changeContext.setYaml(yamlObject);

    EcsInfrastructureMapping ecsInfraMapping = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(ecsInfraMapping);
    assertEquals(ecsInfraMapping.getName(), infraMappingName);

    Yaml yaml = yamlHandler.toYaml(ecsInfraMapping, APP_ID);
    assertNotNull(yaml);
    assertEquals(InfrastructureMappingType.AWS_ECS.name(), yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContent, yamlContent);

    InfrastructureMapping infraMapping = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(infraMapping);
    assertEquals(infraMapping.getName(), infraMappingName);

    yamlHandler.delete(changeContext);

    InfrastructureMapping deletedInfraMapping = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(deletedInfraMapping);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    ChangeContext<Yaml> changeContext = getChangeContext(invalidYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent2, EcsInfrastructureMapping.Yaml.class, false);
    changeContext.setYaml(yamlObject);

    try {
      yamlObject = (Yaml) getYaml(invalidYamlContent, Yaml.class, false);
      changeContext.setYaml(yamlObject);

      yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
      failBecauseExceptionWasNotThrown(UnrecognizedPropertyException.class);
    } catch (UnrecognizedPropertyException ex) {
      // Do nothing
    }
  }

  protected <Y extends BaseYaml, H extends BaseYamlHandler> ChangeContext<Y> getChangeContext(
      String yamlContent, String yamlFilePath, H yamlHandler) {
    // Invalid yaml path
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent(yamlContent)
                                      .build();

    ChangeContext<Y> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.INFRA_MAPPING);
    changeContext.setYamlSyncHandler(yamlHandler);
    return changeContext;
  }
}
