package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.beans.ServiceVariable.Type.TEXT;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.io.IOException;
import java.util.List;

/**
 * Created by anubhaw on 8/9/16.
 */
public class ServiceVariableServiceTest extends WingsBaseTest {
  private static final ServiceVariable SERVICE_VARIABLE = ServiceVariable.builder()
                                                              .envId(ENV_ID)
                                                              .entityType(EntityType.SERVICE_TEMPLATE)
                                                              .entityId(TEMPLATE_ID)
                                                              .templateId(TEMPLATE_ID)
                                                              .name(SERVICE_VARIABLE_NAME)
                                                              .type(TEXT)
                                                              .value("8080".toCharArray())
                                                              .build();

  private static final ServiceVariable ENCRYPTED_SERVICE_VARIABLE = ServiceVariable.builder()
                                                                        .envId(ENV_ID)
                                                                        .entityType(EntityType.SERVICE_TEMPLATE)
                                                                        .entityId(TEMPLATE_ID)
                                                                        .templateId(TEMPLATE_ID)
                                                                        .name(SERVICE_VARIABLE_NAME + "2")
                                                                        .type(ENCRYPTED_TEXT)
                                                                        .value("9090".toCharArray())
                                                                        .build();

  static {
    SERVICE_VARIABLE.setUuid(SERVICE_VARIABLE_ID);
    SERVICE_VARIABLE.setAppId(APP_ID);

    ENCRYPTED_SERVICE_VARIABLE.setUuid(SERVICE_VARIABLE_ID + "2");
    ENCRYPTED_SERVICE_VARIABLE.setAppId(APP_ID);
  }
  /**
   * The Query.
   */
  @Mock Query<ServiceVariable> query;
  @Mock UpdateOperations<ServiceVariable> updateOperations;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private EnvironmentService environmentService;
  @Mock private AppService appService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Inject @InjectMocks private ServiceVariableService serviceVariableService;

  /**
   * Sets up.
   *
   * @throws IOException the io exception
   */
  @Before
  public void setUp() throws IOException {
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1l);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter("appId", EQ, APP_ID)
                                  .addFilter("envId", EQ, ENV_ID)
                                  .addFilter("templateId", EQ, TEMPLATE_ID)
                                  .addFilter("entityId", EQ, "ENTITY_ID")
                                  .build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceVariable> configserviceSettingsiles = serviceVariableService.list(pageRequest);
    assertThat(configserviceSettingsiles).isNotNull();
    assertThat(configserviceSettingsiles.getResponse().get(0)).isInstanceOf(ServiceVariable.class);
  }

  /**
   * Should save.
   */
  @Test
  public void shouldSave() {
    serviceVariableService.save(SERVICE_VARIABLE);
    verify(wingsPersistence).saveAndGet(ServiceVariable.class, SERVICE_VARIABLE);
  }

  /**
   * Should throw exception for unsupported entity types.
   */
  @Test
  public void shouldThrowExceptionForUnsupportedEntityTypes() {
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .envId(ENV_ID)
                                          .name(SERVICE_VARIABLE_NAME)
                                          .entityType(EntityType.APPLICATION)
                                          .entityId(TEMPLATE_ID)
                                          .templateId(TEMPLATE_ID)
                                          .type(TEXT)
                                          .value("8080".toCharArray())
                                          .build();
    serviceVariable.setAppId(APP_ID);
    serviceVariable.setUuid(SERVICE_VARIABLE_ID);

    Assertions.assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> serviceVariableService.save(serviceVariable));
  }

  /**
   * Should get.
   */
  @Test
  public void shouldGet() {
    ServiceVariable variable = ServiceVariable.builder().build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    ServiceVariable serviceVariable = serviceVariableService.get(APP_ID, SERVICE_VARIABLE_ID);
    verify(wingsPersistence).get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID);
    assertThat(serviceVariable.getUuid()).isEqualTo(SERVICE_VARIABLE_ID);
  }

  /**
   * Should get by template.
   */
  @Test
  public void shouldGetByTemplate() {
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();

    PageRequest<ServiceVariable> request = aPageRequest()
                                               .addFilter("appId", Operator.EQ, APP_ID)
                                               .addFilter("envId", Operator.EQ, ENV_ID)
                                               .addFilter("templateId", Operator.EQ, TEMPLATE_ID)
                                               .build();
    PageResponse<ServiceVariable> resp = new PageResponse<>();
    resp.setResponse(asList(SERVICE_VARIABLE));
    when(wingsPersistence.query(ServiceVariable.class, request)).thenReturn(resp);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(SERVICE_VARIABLE);
    List<ServiceVariable> serviceVariables =
        serviceVariableService.getServiceVariablesByTemplate(APP_ID, ENV_ID, serviceTemplate, false);

    verify(wingsPersistence).query(ServiceVariable.class, request);
    assertThat(serviceVariables.get(0)).isEqualTo(SERVICE_VARIABLE);
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdateNone() {
    ServiceVariable variable =
        ServiceVariable.builder().name(SERVICE_VARIABLE_NAME).entityType(EntityType.SERVICE_TEMPLATE).build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);
    verify(wingsPersistence, times(0))
        .updateFields(ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("type", TEXT));
  }

  /**
   * Should override service variable.
   */
  @Test
  public void shouldUpdateServiceVariable() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .value("test".toCharArray())
                                   .build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);

    verify(wingsPersistence, times(0))
        .updateFields(
            ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("value", variable.getValue().toString()));
  }

  /**
   * Should override service variable (Name null).
   */
  @Test
  public void shouldAllowUpdateServiceVariableWhenNameNull() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(null)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .value("test".toCharArray())
                                   .build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);

    verify(wingsPersistence, times(0))
        .updateFields(
            ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("value", variable.getValue().toString()));
  }

  /**
   * Should Allow service variable override (Saved name = current name).
   */
  @Test
  public void shouldAllowUpdateServiceVariable() {
    ServiceVariable variable =
        ServiceVariable.builder().name(SERVICE_VARIABLE_NAME).entityType(EntityType.SERVICE_TEMPLATE).build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);

    String secondVariableId = "SERVICE_VARIABLE_ID";
    ServiceVariable variable2 =
        ServiceVariable.builder().name(SERVICE_VARIABLE_NAME).entityType(EntityType.SERVICE_TEMPLATE).build();

    variable.setAppId(APP_ID);
    variable.setUuid(secondVariableId);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, secondVariableId)).thenReturn(variable2);
    serviceVariableService.update(variable);
  }

  /**
   * Should Throw exception for service variable override.
   */
  @Test(expected = InvalidRequestException.class)
  public void shouldThrowExceptionUpdateServiceVariable() {
    ServiceVariable variable =
        ServiceVariable.builder().name(SERVICE_VARIABLE_NAME).entityType(EntityType.SERVICE_TEMPLATE).build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);

    String secondVariableName = "SERVICE_VARIABLE_NAME_2";
    String secondVariableId = "SERVICE_VARIABLE_ID";
    ServiceVariable variable2 =
        ServiceVariable.builder().name(secondVariableName).entityType(EntityType.SERVICE_TEMPLATE).build();

    variable.setAppId(APP_ID);
    variable.setUuid(secondVariableId);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, secondVariableId)).thenReturn(variable2);
    serviceVariableService.update(variable);
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdateValueAndType() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .value(SERVICE_VARIABLE.getValue())
                                   .type(TEXT)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);
    verify(wingsPersistence)
        .updateFields(ServiceVariable.class, SERVICE_VARIABLE_ID,
            ImmutableMap.of("type", TEXT, "value", SERVICE_VARIABLE.getValue()));
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    ServiceVariable variable = ServiceVariable.builder().entityType(EntityType.SERVICE_TEMPLATE).build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(false);
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(ServiceVariable.class)).thenReturn(updateOperations);
    when(updateOperations.unset(any())).thenReturn(updateOperations);
    serviceVariableService.delete(APP_ID, SERVICE_VARIABLE_ID);
    verify(wingsPersistence).delete(query);
  }

  /**
   * Should get for entity.
   */
  @Test
  public void shouldGetForEntity() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1l);

    PageRequest<ServiceVariable> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, APP_ID).addFilter("entityId", Operator.EQ, "ENTITY_ID").build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);

    serviceVariableService.getServiceVariablesForEntity(APP_ID, "ENTITY_ID", false);
    verify(wingsPersistence).query(ServiceVariable.class, pageRequest);
  }

  /**
   * Should delete by entity id.
   */
  @Test
  public void shouldDeleteByEntityId() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1l);

    PageRequest<ServiceVariable> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, APP_ID).addFilter("entityId", Operator.EQ, "ENTITY_ID").build();

    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);

    serviceVariableService.pruneByService(APP_ID, "ENTITY_ID");
    verify(wingsPersistence).delete(query);
  }

  /**
   * Should mask encrypted fields.
   */
  @Test
  public void shouldMaskEncryptedFields() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(ENCRYPTED_SERVICE_VARIABLE));
    pageResponse.setTotal(1l);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter("appId", EQ, APP_ID)
                                  .addFilter("envId", EQ, ENV_ID)
                                  .addFilter("templateId", EQ, TEMPLATE_ID)
                                  .addFilter("entityId", EQ, "ENTITY_ID")
                                  .build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceVariable> serviceVariablePageResponse = serviceVariableService.list(pageRequest);
    assertThat(serviceVariablePageResponse).isNotNull();
    assertThat(serviceVariablePageResponse.getResponse().get(0)).isInstanceOf(ServiceVariable.class);
    assertThat(serviceVariablePageResponse.getResponse().get(0).getValue()).isEqualTo("9090".toCharArray());
  }
}
