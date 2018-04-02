package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.JENKINS_URL;
import static software.wings.utils.WingsTestConstants.JOB_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.ErrorCode;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.StringValue;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by anubhaw on 5/23/16.
 */
public class SettingsServiceImplTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private Application application;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private AwsHelperService awsHelperService;
  @Mock private SecretManager secretManager;
  @Mock private SettingValidationService settingValidationService;

  @InjectMocks @Inject private SettingsService settingsService;

  private Query<SettingAttribute> spyQuery;

  private PageResponse<SettingAttribute> pageResponse =
      aPageResponse().withResponse(asList(aSettingAttribute().withAppId(SETTING_ID).build())).build();

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    spyQuery = spy(datastore.createQuery(SettingAttribute.class));
    when(wingsPersistence.createQuery(SettingAttribute.class)).thenReturn(spyQuery);
    when(wingsPersistence.query(eq(SettingAttribute.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(wingsPersistence.saveAndGet(eq(SettingAttribute.class), any(SettingAttribute.class)))
        .thenAnswer(new Answer<SettingAttribute>() {

          @Override
          public SettingAttribute answer(InvocationOnMock invocationOnMock) throws Throwable {
            return (SettingAttribute) invocationOnMock.getArguments()[1];
          }
        });
    when(appService.get(anyString())).thenReturn(application);
    when(application.getAccountId()).thenReturn("ACCOUNT_ID");
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  /**
   * Should list settings.
   */
  @Test
  public void shouldListSettings() {
    settingsService.list(aPageRequest().build());
    verify(wingsPersistence).query(eq(SettingAttribute.class), any(PageRequest.class));
  }

  /**
   * Should save setting attribute.
   */
  @Test
  public void shouldSaveSettingAttribute() {
    settingsService.save(aSettingAttribute()
                             .withName("NAME")
                             .withAccountId("ACCOUNT_ID")
                             .withValue(StringValue.Builder.aStringValue().build())
                             .build());
    verify(wingsPersistence).saveAndGet(eq(SettingAttribute.class), any(SettingAttribute.class));
  }

  /**
   * Should get by app id.
   */
  @Test
  public void shouldGetByAppId() {
    settingsService.get(APP_ID, SETTING_ID);
    verify(wingsPersistence).createQuery(eq(SettingAttribute.class));
    verify(spyQuery).filter("appId", APP_ID);
    verify(spyQuery).filter("envId", GLOBAL_ENV_ID);
    verify(spyQuery).filter(ID_KEY, SETTING_ID);
    verify(spyQuery).get();
  }

  /**
   * Should get by app id and env id.
   */
  @Test
  public void shouldGetByAppIdAndEnvId() {
    settingsService.get(APP_ID, ENV_ID, SETTING_ID);
    verify(wingsPersistence).createQuery(eq(SettingAttribute.class));
    verify(spyQuery).filter("appId", APP_ID);
    verify(spyQuery).filter("envId", ENV_ID);
    verify(spyQuery).filter(ID_KEY, SETTING_ID);
    verify(spyQuery).get();
  }

  /**
   * Should get by id.
   */
  @Test
  public void shouldGetById() {
    settingsService.get(SETTING_ID);
    verify(wingsPersistence).get(eq(SettingAttribute.class), eq(SETTING_ID));
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId("APP_ID")
                                            .withAccountId("ACCOUNT_ID")
                                            .withName("SETTING_NAME")
                                            .withCategory(Category.CONNECTOR)
                                            .withValue(JenkinsConfig.builder()
                                                           .jenkinsUrl(JENKINS_URL)
                                                           .password(PASSWORD)
                                                           .username(USER_NAME)
                                                           .accountId("ACCOUNT_ID")
                                                           .build())
                                            .build();
    when(wingsPersistence.get(SettingAttribute.class, SETTING_ID)).thenReturn(settingAttribute);
    when(artifactStreamService.list(any(PageRequest.class))).thenReturn(aPageResponse().build());

    settingsService.delete(APP_ID, SETTING_ID);
    verify(wingsPersistence).delete(any(SettingAttribute.class));
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDeleteSettingAttributesByType() {
    settingsService.deleteSettingAttributesByType(ACCOUNT_ID, APP_ID, ENV_ID, "JENKINS");
    verify(wingsPersistence).delete(any(Query.class));
  }

  @Test
  public void shouldThroeExceptionIfReferencedConnectorDeleted() {
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId("APP_ID")
                                            .withAccountId("ACCOUNT_ID")
                                            .withName("SETTING_NAME")
                                            .withCategory(Category.CONNECTOR)
                                            .withValue(JenkinsConfig.builder()
                                                           .jenkinsUrl(JENKINS_URL)
                                                           .password(PASSWORD)
                                                           .username(USER_NAME)
                                                           .accountId("ACCOUNT_ID")
                                                           .build())
                                            .build();
    when(wingsPersistence.get(SettingAttribute.class, SETTING_ID)).thenReturn(settingAttribute);
    when(artifactStreamService.list(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(asList(
                            JenkinsArtifactStream.Builder.aJenkinsArtifactStream().withSourceName(JOB_NAME).build()))
                        .build());

    assertThatThrownBy(() -> settingsService.delete(APP_ID, SETTING_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_REQUEST.name());
  }

  @Test
  public void shouldThroeExceptionIfReferencedCloudProviderDeleted() {
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId("APP_ID")
                                            .withAccountId("ACCOUNT_ID")
                                            .withName("SETTING_NAME")
                                            .withCategory(Category.CLOUD_PROVIDER)
                                            .withValue(AwsConfig.builder().build())
                                            .build();
    when(wingsPersistence.get(SettingAttribute.class, SETTING_ID)).thenReturn(settingAttribute);
    when(infrastructureMappingService.list(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(asList(anAwsInfrastructureMapping()
                                                 .withName("NAME")
                                                 .withComputeProviderType(AWS.name())
                                                 .withComputeProviderName("NAME")
                                                 .build()))
                        .build());
    assertThatThrownBy(() -> settingsService.delete(APP_ID, SETTING_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_REQUEST.name());
  }

  /**
   * Should get by name.
   */
  @Test
  public void shouldGetByName() {
    settingsService.getByName(ACCOUNT_ID, APP_ID, "NAME");
    verify(wingsPersistence).createQuery(eq(SettingAttribute.class));
    verify(spyQuery).filter("accountId", ACCOUNT_ID);
    verify(spyQuery).field("appId");
    verify(spyQuery).field("envId");
    verify(spyQuery).filter("name", "NAME");
    verify(spyQuery).get();
  }

  /**
   * Should list connection attributes.
   */
  @Test
  public void shouldListConnectionAttributes() {
    SettingAttribute settingAttribute = settingsService.save(aSettingAttribute()
                                                                 .withAppId("APP_ID")
                                                                 .withAccountId("ACCOUNT_ID")
                                                                 .withName("USER_PASSWORD")
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withAccessType(USER_PASSWORD)
                                                                                .withConnectionType(SSH)
                                                                                .withAccountId("ACCOUNT_ID")
                                                                                .build())
                                                                 .build());
    List<SettingAttribute> connectionAttributes =
        settingsService.getSettingAttributesByType("APP_ID", HOST_CONNECTION_ATTRIBUTES.name());
    verify(wingsPersistence).query(eq(SettingAttribute.class), any(PageRequest.class));
  }

  /**
   * Should list bastion host connection attributes.
   */
  @Test
  public void shouldListBastionHostConnectionAttributes() {
    SettingAttribute settingAttribute =
        settingsService.save(aSettingAttribute()
                                 .withAppId("APP_ID")
                                 .withAccountId("ACCOUNT_ID")
                                 .withName("USER_PASSWORD")
                                 .withValue(BastionConnectionAttributes.Builder.aBastionConnectionAttributes()
                                                .withAccessType(AccessType.USER_PASSWORD)
                                                .withConnectionType(SSH)
                                                .withHostName(HOST_NAME)
                                                .withAccountId("ACCOUNT_ID")
                                                .build())
                                 .build());

    List<SettingAttribute> connectionAttributes = settingsService.getSettingAttributesByType(
        "APP_ID", SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES.name());
    verify(wingsPersistence).query(eq(SettingAttribute.class), any(PageRequest.class));
  }

  @Test
  public void updateShouldFailIfSettingAttributeDoesNotExist() {
    SettingAttribute aSettingAttribute =
        aSettingAttribute()
            .withAppId("APP_ID")
            .withAccountId("ACCOUNT_ID")
            .withName("USER_PASSWORD")
            .withValue(BastionConnectionAttributes.Builder.aBastionConnectionAttributes()
                           .withAccessType(AccessType.USER_PASSWORD)
                           .withConnectionType(SSH)
                           .withHostName(HOST_NAME)
                           .withAccountId("ACCOUNT_ID")
                           .build())
            .build();

    assertThatThrownBy(() -> settingsService.update(aSettingAttribute))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.GENERAL_ERROR.name());
  }

  @Test
  public void updateShouldWorkWithSameData() {
    final String uuid = UUID.randomUUID().toString();
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withUuid(uuid)
            .withAppId(APP_ID)
            .withAccountId(ACCOUNT_ID)
            .withName("MY_CLOUD_PROVIDER")
            .withCategory(Category.CLOUD_PROVIDER)
            .withValue(AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build())
            .build();

    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    doReturn(settingAttribute).when(wingsPersistence).get(SettingAttribute.class, uuid);
    doReturn(settingAttribute).when(spyQuery).get();

    SettingAttribute aSettingAttributeWithDummyKey = aSettingAttribute()
                                                         .withAppId("APP_ID")
                                                         .withAccountId("ACCOUNT_ID")
                                                         .withName("MY_CLOUD_PROVIDER")
                                                         .withCategory(Category.CLOUD_PROVIDER)
                                                         .withValue(AwsConfig.builder()
                                                                        .accountId("accountId")
                                                                        .accessKey("accessKey")
                                                                        .secretKey(ENCRYPTED_FIELD_MASK)
                                                                        .build())
                                                         .build();

    settingsService.update(settingAttribute, false);
  }
}