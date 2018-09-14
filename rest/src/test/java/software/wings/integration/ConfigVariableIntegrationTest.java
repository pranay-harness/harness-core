package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;

import java.util.UUID;

@SetupScheduler
public class ConfigVariableIntegrationTest extends BaseIntegrationTest {
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariableService;

  private Application app;
  private Service service;
  private Environment env;
  private ServiceTemplate serviceTemplate;
  private ServiceVariable serviceVariable1, serviceVariable2;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    app = appService.save(
        anApplication().withAccountId(ACCOUNT_ID).withName(APP_NAME + System.currentTimeMillis()).build());
    service = serviceResourceService.save(
        Service.builder().appId(app.getUuid()).name(SERVICE_NAME + System.currentTimeMillis()).build());
    env = anEnvironment().withAppId(app.getAppId()).withUuid(generateUuid()).withName(ENV_NAME).build();
    serviceTemplate = serviceTemplateService.save(aServiceTemplate()
                                                      .withEnvId(env.getUuid())
                                                      .withAppId(app.getAppId())
                                                      .withUuid(generateUuid())
                                                      .withServiceId(service.getUuid())
                                                      .withName(SERVICE_NAME)
                                                      .build());
  }

  @Test
  public void shouldOverrideServiceConfigVariable() {
    // Config variable - Entity type as Service
    serviceVariable1 = getServiceVariable(Type.TEXT);
    serviceVariable1.setAppId(app.getAppId());

    String svId = serviceVariableService.save(serviceVariable1).getUuid();
    ServiceVariable parentServiceVariable = serviceVariableService.get(app.getAppId(), svId);

    assertThat(parentServiceVariable.getEntityType().equals(EntityType.SERVICE));
    assertThat(parentServiceVariable.getUuid()).isEqualTo(svId);
    assertThat(parentServiceVariable.getType().name()).isEqualTo(Type.TEXT.name());

    // Config variable override - Entity type as Service Template
    serviceVariable2 = getServiceVariable(Type.TEXT);
    serviceVariable2.setAppId(app.getAppId());
    serviceVariable2.setEntityType(EntityType.SERVICE_TEMPLATE);
    serviceVariable2.setTemplateId(serviceTemplate.getUuid());
    serviceVariable2.setValue("testUpdated".toCharArray());
    serviceVariable2.setParentServiceVariableId(parentServiceVariable.getUuid());

    String newSvId = serviceVariableService.save(serviceVariable2).getUuid();
    ServiceVariable overrideServiceVariable = serviceVariableService.get(app.getAppId(), newSvId);

    assertThat(overrideServiceVariable.getEntityType().equals(EntityType.SERVICE_TEMPLATE));
    assertThat(overrideServiceVariable.getUuid()).isEqualTo(newSvId);
    assertThat(overrideServiceVariable.getType().name()).isEqualTo(Type.TEXT.name());
    assertThat(overrideServiceVariable.getParentServiceVariableId()).isEqualTo(parentServiceVariable.getUuid());
  }

  @Test
  public void shouldOverrideEncryptedServiceConfigVariable() {
    // Config variable - Entity type as Service
    serviceVariable1 = getServiceVariable(Type.ENCRYPTED_TEXT);
    serviceVariable1.setAppId(app.getAppId());
    serviceVariable1.setEncryptedValue("encryptedValue");

    String svId = serviceVariableService.save(serviceVariable1).getUuid();
    ServiceVariable parentServiceVariable = serviceVariableService.get(app.getAppId(), svId);

    assertThat(parentServiceVariable.getEntityType().equals(EntityType.SERVICE));
    assertThat(parentServiceVariable.getUuid()).isEqualTo(svId);
    assertThat(parentServiceVariable.getType().name()).isEqualTo(Type.ENCRYPTED_TEXT.name());

    // Variable override - Entity type as Service Template
    serviceVariable2 = getServiceVariable(Type.ENCRYPTED_TEXT);
    serviceVariable2.setAppId(app.getAppId());
    serviceVariable2.setEntityType(EntityType.SERVICE_TEMPLATE);
    serviceVariable2.setTemplateId(serviceTemplate.getUuid());
    serviceVariable2.setEncryptedValue("updatedEncryptedValue");
    serviceVariable2.setParentServiceVariableId(parentServiceVariable.getUuid());

    String newSvId = serviceVariableService.save(serviceVariable2).getUuid();
    ServiceVariable overrideServiceVariable = serviceVariableService.get(app.getAppId(), newSvId);

    assertThat(overrideServiceVariable.getEntityType().equals(EntityType.SERVICE_TEMPLATE));
    assertThat(overrideServiceVariable.getUuid()).isEqualTo(newSvId);
    assertThat(overrideServiceVariable.getType().name()).isEqualTo(Type.ENCRYPTED_TEXT.name());
    assertThat(overrideServiceVariable.getParentServiceVariableId()).isEqualTo(parentServiceVariable.getUuid());
  }

  private ServiceVariable getServiceVariable(Type type) {
    return ServiceVariable.builder()
        .envId(env.getUuid())
        .entityType(EntityType.SERVICE)
        .entityId(generateUuid())
        .overrideType(OverrideType.ALL)
        .accountId(ACCOUNT_ID)
        .name(UUID.randomUUID().toString())
        .value("test".toCharArray())
        .type(type)
        .build();
  }

  @After
  public void tearDown() {
    if (serviceVariable1 != null) {
      serviceVariableService.delete(app.getAppId(), serviceVariable1.getUuid());
    }
    if (serviceVariable2 != null) {
      serviceVariableService.delete(app.getAppId(), serviceVariable2.getUuid());
    }
    if (serviceTemplate != null) {
      serviceTemplateService.delete(app.getAppId(), serviceTemplate.getUuid());
    }
    if (service != null) {
      serviceResourceService.delete(app.getAppId(), service.getUuid());
    }
    if (app != null) {
      appService.delete(app.getAppId());
    }
  }
}