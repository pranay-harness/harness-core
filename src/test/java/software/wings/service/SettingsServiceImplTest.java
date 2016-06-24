package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/23/16.
 */
public class SettingsServiceImplTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private Datastore datastore;
  @Mock private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private SettingsService settingsService;

  private Query<SettingAttribute> spyQuery;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    spyQuery = spy(datastore.createQuery(SettingAttribute.class));
    when(wingsPersistence.createQuery(SettingAttribute.class)).thenReturn(spyQuery);
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
    settingsService.save(aSettingAttribute().build());
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
                                                                 .withName("USER_PASSWORD")
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withType(HOST_CONNECTION_ATTRIBUTES)
                                                                                .withAccessType(USER_PASSWORD)
                                                                                .withConnectionType(SSH)
                                                                                .build())
                                                                 .build());

    List<SettingAttribute> connectionAttributes =
        settingsService.getSettingAttributesByType("APP_ID", HOST_CONNECTION_ATTRIBUTES);
    assertThat(connectionAttributes)
        .isNotNull()
        .extracting(SettingAttribute::getValue)
        .hasOnlyElementsOfType(HostConnectionAttributes.class);
  }

  /**
   * Should list bastion host connection attributes.
   */
  @Test
  public void shouldListBastionHostConnectionAttributes() {
    SettingAttribute settingAttribute = settingsService.save(aSettingAttribute()
                                                                 .withAppId("APP_ID")
                                                                 .withName("USER_PASSWORD")
                                                                 .withValue(new BastionConnectionAttributes())
                                                                 .build());

    List<SettingAttribute> connectionAttributes =
        settingsService.getSettingAttributesByType("APP_ID", SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES);
    assertThat(connectionAttributes)
        .isNotNull()
        .extracting(SettingAttribute::getValue)
        .hasOnlyElementsOfType(BastionConnectionAttributes.class);
  }
}
