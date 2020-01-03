package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.ServiceVariable.Type.ARTIFACT;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.beans.ServiceVariable.Type.TEXT;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.io.IOException;
import java.util.ArrayList;
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
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private EntityVersionService mockEntityVersionService;
  @Inject @InjectMocks private ServiceVariableService serviceVariableService;

  /**
   * Sets up.
   *
   * @throws IOException the io exception
   */
  @Before
  public void setUp() throws IOException {
    SERVICE_VARIABLE.setAppId(APP_ID);
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(appService.get(TARGET_APP_ID)).thenReturn(Application.Builder.anApplication().accountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().accountId(ACCOUNT_ID).build());

    doNothing().when(auditServiceHelper).reportForAuditingUsingAccountId(anyString(), any(), any(), any());
    doNothing().when(auditServiceHelper).reportDeleteForAuditingUsingAccountId(anyString(), any());
    doNothing().when(auditServiceHelper).reportDeleteForAuditing(anyString(), any());
    doNothing().when(auditServiceHelper).reportForAuditingUsingAppId(anyString(), any(), any(), any());
  }

  /**
   * Should list.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldSave() {
    serviceVariableService.save(SERVICE_VARIABLE);
    verify(wingsPersistence).saveAndGet(ServiceVariable.class, SERVICE_VARIABLE);
    verify(mockEntityVersionService)
        .newEntityVersion(APP_ID, EntityType.CONFIG, SERVICE_VARIABLE.getUuid(), SERVICE_VARIABLE.getEntityId(),
            SERVICE_VARIABLE.getName(), ChangeType.CREATED, null);
  }

  /**
   * Should throw exception for unsupported entity types.
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldGet() {
    ServiceVariable variable = ServiceVariable.builder().build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    ServiceVariable serviceVariable = serviceVariableService.get(APP_ID, SERVICE_VARIABLE_ID);
    verify(wingsPersistence).getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID);
    assertThat(serviceVariable.getUuid()).isEqualTo(SERVICE_VARIABLE_ID);
  }

  /**
   * Should get by template.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID))
        .thenReturn(SERVICE_VARIABLE);
    List<ServiceVariable> serviceVariables =
        serviceVariableService.getServiceVariablesByTemplate(APP_ID, ENV_ID, serviceTemplate, OBTAIN_VALUE);

    verify(wingsPersistence).query(ServiceVariable.class, request);
    assertThat(serviceVariables.get(0)).isEqualTo(SERVICE_VARIABLE);
  }

  /**
   * Should update.
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldUpdateNone() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .type(TEXT)
                                   .value(generateUuid().toCharArray())
                                   .build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);
    verify(wingsPersistence, times(0))
        .updateFields(ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("type", TEXT));
  }

  /**
   * Should override service variable.
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldUpdateServiceVariable() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .type(TEXT)
                                   .value("test".toCharArray())
                                   .build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);

    verify(wingsPersistence, times(0))
        .updateFields(
            ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("value", variable.getValue().toString()));
    verify(mockEntityVersionService)
        .newEntityVersion(APP_ID, EntityType.CONFIG, variable.getUuid(), variable.getEntityId(), variable.getName(),
            ChangeType.UPDATED, null);
  }

  /**
   * Should override service variable (Name null).
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldAllowUpdateServiceVariableWhenNameNull() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(null)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .type(TEXT)
                                   .value("test".toCharArray())
                                   .build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);

    verify(wingsPersistence, times(0))
        .updateFields(
            ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("value", variable.getValue().toString()));
  }

  /**
   * Should Allow service variable override (Saved name = current name).
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldAllowUpdateServiceVariable() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .type(TEXT)
                                   .value(generateUuid().toCharArray())
                                   .build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);

    String secondVariableId = "SERVICE_VARIABLE_ID";
    ServiceVariable variable2 =
        ServiceVariable.builder().name(SERVICE_VARIABLE_NAME).entityType(EntityType.SERVICE_TEMPLATE).build();

    variable.setAppId(APP_ID);
    variable.setUuid(secondVariableId);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, secondVariableId)).thenReturn(variable2);
    serviceVariableService.update(variable);
    verify(mockEntityVersionService)
        .newEntityVersion(APP_ID, EntityType.CONFIG, variable.getUuid(), variable.getEntityId(), variable.getName(),
            ChangeType.UPDATED, null);
  }

  /**
   * Should Throw exception for service variable override.
   */
  @Test(expected = InvalidRequestException.class)
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionUpdateServiceVariable() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .type(TEXT)
                                   .value(generateUuid().toCharArray())
                                   .build();

    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);

    String secondVariableName = "SERVICE_VARIABLE_NAME_2";
    String secondVariableId = "SERVICE_VARIABLE_ID";
    ServiceVariable variable2 = ServiceVariable.builder()
                                    .name(secondVariableName)
                                    .entityType(EntityType.SERVICE_TEMPLATE)
                                    .type(TEXT)
                                    .value(generateUuid().toCharArray())
                                    .build();

    variable.setAppId(APP_ID);
    variable.setUuid(secondVariableId);
    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, secondVariableId)).thenReturn(variable2);
    serviceVariableService.update(variable);
  }

  /**
   * Should update.
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldUpdateValueAndType() {
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .value(SERVICE_VARIABLE.getValue())
                                   .type(TEXT)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);
    verify(wingsPersistence)
        .updateFields(ServiceVariable.class, SERVICE_VARIABLE_ID,
            ImmutableMap.of("type", TEXT, "value", SERVICE_VARIABLE.getValue(), "allowedList", new ArrayList<>()));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldUpdateAllowedList() {
    List<String> allowedList = new ArrayList<>();
    allowedList.add(ARTIFACT_STREAM_ID);
    ServiceVariable variable = ServiceVariable.builder()
                                   .name(SERVICE_VARIABLE_NAME)
                                   .type(ARTIFACT)
                                   .entityType(EntityType.SERVICE_TEMPLATE)
                                   .allowedList(allowedList)
                                   .build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(variable);
    verify(wingsPersistence)
        .updateFields(
            ServiceVariable.class, SERVICE_VARIABLE_ID, ImmutableMap.of("type", ARTIFACT, "allowedList", allowedList));
  }

  /**
   * Should delete.
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldDelete() {
    ServiceVariable variable = ServiceVariable.builder().entityType(EntityType.SERVICE_TEMPLATE).build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.getWithAppId(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetForEntity() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1l);

    PageRequest<ServiceVariable> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, APP_ID).addFilter("entityId", Operator.EQ, "ENTITY_ID").build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);

    serviceVariableService.getServiceVariablesForEntity(APP_ID, "ENTITY_ID", OBTAIN_VALUE);
    verify(wingsPersistence).query(ServiceVariable.class, pageRequest);
  }

  /**
   * Should delete by entity id.
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldDeleteByEntityId() {
    AuditServiceHelper auditServiceHelper = mock(AuditServiceHelper.class);
    doNothing().when(auditServiceHelper).reportDeleteForAuditing(anyString(), any());

    List<ServiceVariable> serviceVariables = asList(SERVICE_VARIABLE);
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(query.filter(anyString(), any())).thenReturn(query);
    when(query.asList()).thenReturn(serviceVariables);
    when(wingsPersistence.delete(any(ServiceVariable.class))).thenReturn(true);

    serviceVariableService.pruneByService(APP_ID, "ENTITY_ID");
    verify(wingsPersistence, times(1)).delete(any(ServiceVariable.class));
  }

  /**
   * Should mask encrypted fields.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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
