package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceSetting.Builder.aServiceSetting;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceSetting;
import software.wings.beans.ServiceSetting.Type;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceSettingService;
import software.wings.service.intfc.ServiceTemplateService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/9/16.
 */
public class ServiceSettingServiceTest extends WingsBaseTest {
  /**
   * The Query.
   */
  @Mock Query<ServiceSetting> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceTemplateService serviceTemplateService;

  @Inject @InjectMocks private ServiceSettingService serviceSettingService;

  /**
   * Sets up.
   *
   * @throws IOException the io exception
   */
  @Before
  public void setUp() throws IOException {
    when(wingsPersistence.createQuery(ServiceSetting.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEntityType(EntityType.SERVICE)
                                        .withEntityId(SERVICE_ID)
                                        .withTemplateId(ServiceSetting.DEFAULT_TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();

    PageResponse<ServiceSetting> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(serviceSetting));
    pageResponse.setTotal(1);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter(aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("envId", EQ, ENV_ID)
                                                 .withField("templateId", EQ, TEMPLATE_ID)
                                                 .withField("entityId", EQ, "ENTITY_ID")
                                                 .build())
                                  .build();

    when(wingsPersistence.query(ServiceSetting.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceSetting> configserviceSettingsiles = serviceSettingService.list(pageRequest);
    assertThat(configserviceSettingsiles).isNotNull();
    assertThat(configserviceSettingsiles.getResponse().get(0)).isInstanceOf(ServiceSetting.class);
  }

  /**
   * Should save.
   */
  @Test
  public void shouldSave() {
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID))
        .thenReturn(aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build());
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEnvId(ENV_ID)
                                        .withUuid(FILE_ID)
                                        .withEntityType(EntityType.TAG)
                                        .withEntityId(TAG_ID)
                                        .withTemplateId(TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();
    serviceSettingService.save(serviceSetting);
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
    verify(wingsPersistence).save(serviceSetting);
  }

  /**
   * Should throw exception for unsupported entity types.
   */
  @Test
  public void shouldThrowExceptionForUnsupportedEntityTypes() {
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEnvId(ENV_ID)
                                        .withUuid(FILE_ID)
                                        .withEntityType(EntityType.ENVIRONMENT)
                                        .withEntityId(TAG_ID)
                                        .withTemplateId(TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();

    Assertions.assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> serviceSettingService.save(serviceSetting));
  }

  /**
   * Should get.
   */
  @Test
  public void shouldGet() {
    when(wingsPersistence.get(ServiceSetting.class, APP_ID, FILE_ID))
        .thenReturn(aServiceSetting().withAppId(APP_ID).withUuid(FILE_ID).build());
    ServiceSetting serviceSetting = serviceSettingService.get(APP_ID, FILE_ID);
    verify(wingsPersistence).get(ServiceSetting.class, APP_ID, FILE_ID);
    assertThat(serviceSetting.getUuid()).isEqualTo(FILE_ID);
  }

  /**
   * Should get by template.
   */
  @Test
  public void shouldGetByTemplate() {
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEntityType(EntityType.TAG)
                                        .withEntityId(TAG_ID)
                                        .withTemplateId(TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();

    when(query.asList()).thenReturn(Arrays.asList(serviceSetting));
    when(wingsPersistence.get(ServiceSetting.class, APP_ID, FILE_ID)).thenReturn(serviceSetting);
    List<ServiceSetting> serviceSettings =
        serviceSettingService.getServiceSettingByTemplate(APP_ID, ENV_ID, serviceTemplate);

    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("templateId");
    verify(end).equal(TEMPLATE_ID);
    assertThat(serviceSettings.get(0)).isEqualTo(serviceSetting);
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdate() {
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEnvId(ENV_ID)
                                        .withUuid(FILE_ID)
                                        .withEntityType(EntityType.TAG)
                                        .withEntityId(TAG_ID)
                                        .withTemplateId(TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();

    when(wingsPersistence.saveAndGet(ServiceSetting.class, serviceSetting)).thenReturn(serviceSetting);
    serviceSettingService.update(serviceSetting);

    verify(wingsPersistence).saveAndGet(eq(ServiceSetting.class), eq(serviceSetting));
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(wingsPersistence.createQuery(ServiceSetting.class)).thenReturn(query);
    serviceSettingService.delete(APP_ID, FILE_ID);
    verify(wingsPersistence).delete(query);
  }

  /**
   * Should get for entity.
   */
  @Test
  public void shouldGetForEntity() {
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEntityType(EntityType.TAG)
                                        .withEntityId(TAG_ID)
                                        .withTemplateId(TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();

    PageResponse<ServiceSetting> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(serviceSetting));
    pageResponse.setTotal(1);

    PageRequest<ServiceSetting> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("templateId", Operator.EQ, TEMPLATE_ID).build())
            .addFilter(aSearchFilter().withField("entityId", Operator.EQ, "ENTITY_ID").build())
            .build();

    when(wingsPersistence.query(ServiceSetting.class, pageRequest)).thenReturn(pageResponse);

    serviceSettingService.getSettingsForEntity(APP_ID, TEMPLATE_ID, "ENTITY_ID");
    verify(wingsPersistence).query(ServiceSetting.class, pageRequest);
  }

  /**
   * Should delete by entity id.
   */
  @Test
  public void shouldDeleteByEntityId() {
    ServiceSetting serviceSetting = aServiceSetting()
                                        .withAppId(APP_ID)
                                        .withEnvId(ENV_ID)
                                        .withUuid(FILE_ID)
                                        .withEntityType(EntityType.TAG)
                                        .withEntityId(TAG_ID)
                                        .withTemplateId(TEMPLATE_ID)
                                        .withType(Type.PORT)
                                        .withValue("8080")
                                        .build();

    PageResponse<ServiceSetting> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(serviceSetting));
    pageResponse.setTotal(1);

    PageRequest<ServiceSetting> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("templateId", Operator.EQ, TEMPLATE_ID).build())
            .addFilter(aSearchFilter().withField("entityId", Operator.EQ, "ENTITY_ID").build())
            .build();

    when(wingsPersistence.createQuery(ServiceSetting.class)).thenReturn(query);
    when(wingsPersistence.query(ServiceSetting.class, pageRequest)).thenReturn(pageResponse);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);

    serviceSettingService.deleteByEntityId(APP_ID, TEMPLATE_ID, "ENTITY_ID");
    verify(wingsPersistence).delete(query);
  }
}
