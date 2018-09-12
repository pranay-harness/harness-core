package software.wings.service;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.EnvFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.PIPELINE;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.UsageRestrictions.AppEnvRestriction.builder;
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
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Ignore;
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
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.StringValue;
import software.wings.beans.StringValue.Builder;
import software.wings.beans.User;
import software.wings.beans.ValidationResult;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.security.WorkflowFilter;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by anubhaw on 5/23/16.
 */
public class SettingsServiceImplTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private AuthHandler authHandler;
  @Mock private Application application;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private AwsHelperService awsHelperService;
  @Mock private SecretManager secretManager;
  @Mock private SettingValidationService settingValidationService;
  @Mock private UserGroupService userGroupService;

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
    settingsService.list(aPageRequest().build(), null, null);
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
        .thenReturn(
            aPageResponse().withResponse(asList(JenkinsArtifactStream.builder().sourceName(JOB_NAME).build())).build());

    assertThatThrownBy(() -> settingsService.delete(APP_ID, SETTING_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_REQUEST.name());
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
        .hasMessage(INVALID_REQUEST.name());
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
    settingsService.getSettingAttributesByType("APP_ID", HOST_CONNECTION_ATTRIBUTES.name());
    verify(wingsPersistence).query(eq(SettingAttribute.class), any(PageRequest.class));
  }

  /**
   * Should list bastion host connection attributes.
   */
  @Test
  public void shouldListBastionHostConnectionAttributes() {
    settingsService.getSettingAttributesByType(
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

    settingsService.update(settingAttribute, false);
  }

  @Test
  public void testValidateWhenValid() {
    final String uuid = UUID.randomUUID().toString();
    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withUuid(uuid)
            .withAppId(APP_ID)
            .withAccountId(ACCOUNT_ID)
            .withName("MY_CLOUD_PROVIDER_00")
            .withCategory(Category.CLOUD_PROVIDER)
            .withValue(AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build())
            .build();
    doReturn(true).when(settingValidationService).validate(settingAttribute);
    final ValidationResult result = settingsService.validate(settingAttribute);
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isTrue();
  }

  @Test
  public void testValidateWhenNotValid() {
    final String uuid = UUID.randomUUID().toString();
    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withUuid(uuid)
            .withAppId(APP_ID)
            .withAccountId(ACCOUNT_ID)
            .withName("MY_CLOUD_PROVIDER_01")
            .withCategory(Category.CLOUD_PROVIDER)
            .withValue(AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build())
            .build();
    doReturn(false).when(settingValidationService).validate(settingAttribute);
    final ValidationResult result = settingsService.validate(settingAttribute);
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isFalse();
  }

  @Test
  public void testValidateIdValid() {
    final String uuid = UUID.randomUUID().toString();
    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withUuid(uuid)
            .withAppId(APP_ID)
            .withAccountId(ACCOUNT_ID)
            .withName("MY_CLOUD_PROVIDER_02")
            .withCategory(Category.CLOUD_PROVIDER)
            .withValue(AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build())
            .build();
    doReturn(settingAttribute).when(wingsPersistence).get(SettingAttribute.class, uuid);
    doReturn(true).when(settingValidationService).validate(settingAttribute);
    final ValidationResult result = settingsService.validate(uuid);
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isTrue();
  }

  @Test
  public void testValidateIdWhenNotExists() {
    final String uuid = UUID.randomUUID().toString();
    doReturn(null).when(wingsPersistence).get(SettingAttribute.class, uuid);
    final ValidationResult result = settingsService.validate(uuid);
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isFalse();
  }

  @Test
  @Ignore
  public void testUsageRestrictionsWithNothingSet() {
    try {
      String ENV_ID_1 = "ENV_ID_1";
      String ENV_ID_2 = "ENV_ID_2";
      String ENV_ID_3 = "ENV_ID_3";

      String APP_ID_1 = "APP_ID_1";
      String APP_ID_2 = "APP_ID_2";
      String APP_ID_3 = "APP_ID_3";

      String SA_1 = "SA_1";
      String SA_2 = "SA_2";
      String SA_3 = "SA_3";

      List<SettingAttribute> settingAttributeList = Lists.newArrayList();

      SettingValue settingValue1 =
          JenkinsConfig.builder().jenkinsUrl("").password("".toCharArray()).username("").build();
      SettingAttribute settingAttribute1 = aSettingAttribute()
                                               .withAccountId(ACCOUNT_ID)
                                               .withEnvId(ENV_ID_1)
                                               .withAppId(APP_ID_1)
                                               .withName(SA_1)
                                               .withValue(settingValue1)
                                               .withCategory(Category.CONNECTOR)
                                               .build();

      SettingValue settingValue2 =
          JenkinsConfig.builder().jenkinsUrl("").password("".toCharArray()).username("").build();
      SettingAttribute settingAttribute2 = aSettingAttribute()
                                               .withAccountId(ACCOUNT_ID)
                                               .withEnvId(ENV_ID_2)
                                               .withAppId(APP_ID_2)
                                               .withName(SA_2)
                                               .withValue(settingValue2)
                                               .withCategory(Category.CONNECTOR)
                                               .build();

      SettingValue settingValue3 =
          JenkinsConfig.builder().jenkinsUrl("").password("".toCharArray()).username("").build();
      SettingAttribute settingAttribute3 = aSettingAttribute()
                                               .withAccountId(ACCOUNT_ID)
                                               .withEnvId(ENV_ID_3)
                                               .withAppId(APP_ID_3)
                                               .withName(SA_3)
                                               .withValue(settingValue3)
                                               .withCategory(Category.CONNECTOR)
                                               .build();

      settingAttributeList.add(settingAttribute1);
      settingAttributeList.add(settingAttribute2);
      settingAttributeList.add(settingAttribute3);

      PageResponse<SettingAttribute> pageResponse = aPageResponse().withResponse(settingAttributeList).build();
      when(wingsPersistence.query(any(), any(PageRequest.class))).thenReturn(pageResponse);

      // Scenario 1: With no usage restrictions set
      List<SettingAttribute> filteredSettingAttributesByType = settingsService.getFilteredSettingAttributesByType(
          APP_ID, SettingVariableTypes.JENKINS.name(), APP_ID, ENV_ID);

      assertThat(filteredSettingAttributesByType)
          .containsExactlyInAnyOrder(settingAttributeList.toArray(new SettingAttribute[0]));

      User user = User.Builder.anUser().withName(USER_NAME).withUuid(USER_ID).build();

      Map<String, AppPermissionSummaryForUI> appPermissionsMap = Maps.newHashMap();

      Map<String, Set<Action>> envPermissionMap = Maps.newHashMap();
      envPermissionMap.put(ENV_ID_1, newHashSet(Action.READ));

      AppPermissionSummaryForUI appPermissionSummaryForUI =
          AppPermissionSummaryForUI.builder().envPermissions(envPermissionMap).build();

      appPermissionsMap.put(APP_ID_1, appPermissionSummaryForUI);

      UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .isRbacEnabled(true)
                                                  .appPermissionMap(appPermissionsMap)
                                                  .build();

      user.setUserRequestContext(
          UserRequestContext.builder().accountId(ACCOUNT_ID).userPermissionInfo(userPermissionInfo).build());
      UserThreadLocal.set(user);

      // Scenario 2: With usage restrictions set on settingAttribute1 but for all apps
      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      EnvFilter envFilter = EnvFilter.builder().filterTypes(newHashSet(PROD, NON_PROD)).build();
      AppEnvRestriction appEnvRestriction = builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      settingAttribute1.setUsageRestrictions(usageRestrictions);
      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      filteredSettingAttributesByType = settingsService.getFilteredSettingAttributesByType(
          APP_ID, SettingVariableTypes.JENKINS.name(), APP_ID, ENV_ID);

      assertThat(filteredSettingAttributesByType)
          .containsExactlyInAnyOrder(settingAttributeList.toArray(new SettingAttribute[0]));

      // Scenario 3: With usage restrictions set on settingAttribute1 for app match but env mismatch
      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilter =
          EnvFilter.builder().ids(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3)).filterTypes(newHashSet(SELECTED)).build();
      appEnvRestriction = builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      settingAttribute1.setUsageRestrictions(usageRestrictions);
      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3));

      filteredSettingAttributesByType = settingsService.getFilteredSettingAttributesByType(
          APP_ID, SettingVariableTypes.JENKINS.name(), APP_ID, ENV_ID);

      assertThat(filteredSettingAttributesByType)
          .containsExactlyInAnyOrder(new SettingAttribute[] {settingAttribute2, settingAttribute3});

      // Scenario 4: With usage restrictions set on settingAttribute1 for app mismatch
      appFilter = GenericEntityFilter.builder()
                      .filterType(FilterType.SELECTED)
                      .ids(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3))
                      .build();
      envFilter = EnvFilter.builder().filterTypes(newHashSet(NON_PROD, PROD)).build();
      appEnvRestriction = builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      settingAttribute1.setUsageRestrictions(usageRestrictions);
      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3));

      filteredSettingAttributesByType = settingsService.getFilteredSettingAttributesByType(
          APP_ID, SettingVariableTypes.JENKINS.name(), APP_ID, ENV_ID);

      assertThat(filteredSettingAttributesByType)
          .containsExactlyInAnyOrder(new SettingAttribute[] {settingAttribute2, settingAttribute3});

      // Scenario 5: With usage restrictions set on settingAttribute1 for app and env match
      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilter = EnvFilter.builder().filterTypes(newHashSet(NON_PROD, PROD)).build();
      appEnvRestriction = builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      settingAttribute1.setUsageRestrictions(usageRestrictions);
      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(APP_ID_1));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3));

      filteredSettingAttributesByType = settingsService.getFilteredSettingAttributesByType(
          APP_ID_1, SettingVariableTypes.JENKINS.name(), APP_ID_1, ENV_ID_1);

      assertThat(filteredSettingAttributesByType)
          .containsExactlyInAnyOrder(settingAttributeList.toArray(new SettingAttribute[0]));

      // Scenario 6: With usage restrictions set on settingAttribute1, but no appId and envId was passed
      // and common usage restrictions and user permissions.
      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(APP_ID_1));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class))).thenReturn(newHashSet(ENV_ID_1));

      List<Action> allActions = asList(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE);

      EnvFilter envFilter1 = new EnvFilter();
      envFilter1.setFilterTypes(Sets.newHashSet(PROD));

      WorkflowFilter workflowFilter = new WorkflowFilter();
      workflowFilter.setFilterTypes(Sets.newHashSet(PROD));

      AppPermission envPermission = AppPermission.builder()
                                        .permissionType(ENV)
                                        .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                        .entityFilter(envFilter1)
                                        .actions(new HashSet(allActions))
                                        .build();

      AppPermission workflowPermission =
          AppPermission.builder()
              .permissionType(WORKFLOW)
              .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
              .entityFilter(workflowFilter)
              .actions(new HashSet(allActions))
              .build();

      AppPermission pipelinePermission =
          AppPermission.builder()
              .permissionType(PIPELINE)
              .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
              .entityFilter(envFilter)
              .actions(new HashSet(allActions))
              .build();

      AppPermission deploymentPermission =
          AppPermission.builder()
              .permissionType(DEPLOYMENT)
              .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
              .entityFilter(envFilter)
              .actions(new HashSet(allActions))
              .build();

      UserGroup userGroup =
          UserGroup.builder()
              .appPermissions(
                  new HashSet(asList(envPermission, workflowPermission, pipelinePermission, deploymentPermission)))
              .accountId(ACCOUNT_ID)
              .name("userGroup1")
              .memberIds(asList(USER_ID))
              .build();
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(aPageResponse().withResponse(Arrays.asList(userGroup)).build());
      when(userGroupService.getUserGroupsByAccountId(anyString(), any(User.class)))
          .thenReturn(aPageResponse().withResponse(Arrays.asList(userGroup)).build());
      filteredSettingAttributesByType =
          settingsService.getFilteredSettingAttributesByType(null, SettingVariableTypes.JENKINS.name(), null, null);

      assertThat(filteredSettingAttributesByType)
          .containsExactlyInAnyOrder(settingAttributeList.toArray(new SettingAttribute[0]));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void shouldGetApplicationDefaults() {
    when(wingsPersistence.createQuery(SettingAttribute.class)).thenReturn(spyQuery);
    when(spyQuery.filter(ACCOUNT_ID_KEY, ACCOUNT_ID)).thenReturn(spyQuery);
    when(spyQuery.filter(APP_ID_KEY, APP_ID)).thenReturn(spyQuery);
    when(spyQuery.filter(SettingAttribute.VALUE_TYPE_KEY, SettingVariableTypes.STRING.name())).thenReturn(spyQuery);

    when(spyQuery.asList())
        .thenReturn(asList(aSettingAttribute()
                               .withName("NAME")
                               .withAccountId("ACCOUNT_ID")
                               .withAppId(APP_ID)
                               .withValue(Builder.aStringValue().build())
                               .build(),
            aSettingAttribute()
                .withName("NAME2")
                .withAccountId("ACCOUNT_ID")
                .withAppId(APP_ID)
                .withValue(Builder.aStringValue().withValue("VALUE").build())
                .build()));

    Map<String, String> accountDefaults = settingsService.listAppDefaults(ACCOUNT_ID, APP_ID);
    assertThat(accountDefaults).isNotEmpty().containsKeys("NAME", "NAME2");
    assertThat(accountDefaults).isNotEmpty().containsValues("", "VALUE");
    verify(wingsPersistence).createQuery(SettingAttribute.class);
    verify(spyQuery, times(2)).filter(ACCOUNT_ID_KEY, ACCOUNT_ID);
    verify(spyQuery, times(2)).filter(APP_ID_KEY, APP_ID);
    verify(spyQuery, times(2)).filter(SettingAttribute.VALUE_TYPE_KEY, SettingVariableTypes.STRING.name());
  }

  @Test
  public void shouldGetAccountDefaults() {
    when(wingsPersistence.createQuery(SettingAttribute.class)).thenReturn(spyQuery);
    when(spyQuery.filter(ACCOUNT_ID_KEY, ACCOUNT_ID)).thenReturn(spyQuery);
    when(spyQuery.filter(APP_ID_KEY, APP_ID)).thenReturn(spyQuery);
    when(spyQuery.filter(SettingAttribute.VALUE_TYPE_KEY, SettingVariableTypes.STRING.name())).thenReturn(spyQuery);
    when(spyQuery.asList())
        .thenReturn(asList(aSettingAttribute()
                               .withName("NAME")
                               .withAccountId("ACCOUNT_ID")
                               .withAppId(GLOBAL_APP_ID)
                               .withValue(Builder.aStringValue().build())
                               .build(),
            aSettingAttribute()
                .withName("NAME2")
                .withAccountId("ACCOUNT_ID")
                .withAppId(GLOBAL_APP_ID)
                .withValue(Builder.aStringValue().withValue("VALUE").build())
                .build()));
    Map<String, String> accountDefaults = settingsService.listAccountDefaults(ACCOUNT_ID);
    assertThat(accountDefaults).isNotEmpty().containsKeys("NAME", "NAME2");
    assertThat(accountDefaults).isNotEmpty().containsValues("", "VALUE");
    verify(wingsPersistence).createQuery(SettingAttribute.class);
    verify(spyQuery, times(2)).filter(ACCOUNT_ID_KEY, ACCOUNT_ID);
    verify(spyQuery, times(2)).filter(SettingAttribute.VALUE_TYPE_KEY, SettingVariableTypes.STRING.name());
  }
}