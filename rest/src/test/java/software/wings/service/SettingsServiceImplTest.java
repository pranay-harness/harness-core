package software.wings.service;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by anubhaw on 5/23/16.
 */
public class SettingsServiceImplTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private Datastore datastore;
  @Mock private WingsPersistence wingsPersistence;

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
    verify(spyQuery).field("appId");
    verify(spyQuery).field("envId");
    verify(spyQuery).field(ID_KEY);
    verify(spyQuery).get();
  }

  /**
   * Should get by app id and env id.
   */
  @Test
  public void shouldGetByAppIdAndEnvId() {
    settingsService.get(APP_ID, ENV_ID, SETTING_ID);
    verify(wingsPersistence).createQuery(eq(SettingAttribute.class));
    verify(spyQuery).field("appId");
    verify(spyQuery).field("envId");
    verify(spyQuery).field(ID_KEY);
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
    settingsService.delete(APP_ID, SETTING_ID);
    verify(wingsPersistence).createQuery(eq(SettingAttribute.class));
    verify(spyQuery).field("appId");
    verify(spyQuery).field("envId");
    verify(spyQuery).field(ID_KEY);
    verify(wingsPersistence).delete(spyQuery);
  }

  /**
   * Should get by name.
   */
  @Test
  public void shouldGetByName() {
    settingsService.getByName(APP_ID, "NAME");
    verify(wingsPersistence).createQuery(eq(SettingAttribute.class));
    verify(spyQuery).field("appId");
    verify(spyQuery).field("envId");
    verify(spyQuery).field("name");
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
}
