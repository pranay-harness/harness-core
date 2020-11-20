package software.wings.service.impl.security.auth;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.SettingsService;

import java.util.HashSet;
import java.util.Set;

public class SettingAuthHandlerTest extends WingsBaseTest {
  @Mock private AuthHandler authHandler;
  @Mock private SettingsService settingsService;
  @InjectMocks @Inject private SettingAuthHandler settingAuthHandler;

  private static final String appId = generateUuid();
  private static final String envId = generateUuid();
  private static final String entityId = generateUuid();

  private User user;
  private SettingAttribute settingAttribute;

  @Before
  public void setUp() {
    user = User.Builder.anUser().uuid(generateUuid()).build();
    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAppId(appId)
                           .withUuid(entityId)
                           .withEnvId(envId)
                           .withName("setting-attribute")
                           .build();
  }

  private void setPermissions(PermissionType permissionType) {
    Set<PermissionType> permissionTypeSet = new HashSet<>();
    permissionTypeSet.add(permissionType);
    UserPermissionInfo userPermissionInfo =
        UserPermissionInfo.builder()
            .accountPermissionSummary(AccountPermissionSummary.builder().permissions(permissionTypeSet).build())
            .build();
    UserRequestContext userRequestContext =
        UserRequestContext.builder().accountId(generateUuid()).userPermissionInfo(userPermissionInfo).build();
    user.setUserRequestContext(userRequestContext);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeCloudProviderNull() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CLOUD_PROVIDERS);
      UserThreadLocal.set(user);

      settingAttribute.setCategory(CLOUD_PROVIDER);

      settingAuthHandler.authorize(new SettingAttribute());
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeCloudProviderNotSettingCategory() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CLOUD_PROVIDERS);
      UserThreadLocal.set(user);

      settingAttribute.setCategory(SETTING);

      settingAuthHandler.authorize(new SettingAttribute());
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeCloudProvider() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CLOUD_PROVIDERS);
      UserThreadLocal.set(user);

      settingAttribute.setCategory(CLOUD_PROVIDER);

      settingAuthHandler.authorize(settingAttribute);
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeConnectors() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CONNECTORS);

      settingAttribute.setCategory(CONNECTOR);
      UserThreadLocal.set(user);

      settingAuthHandler.authorize(settingAttribute);
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeWithId() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CONNECTORS);

      settingAttribute.setCategory(CONNECTOR);
      UserThreadLocal.set(user);
      when(settingsService.get(eq(appId), eq(entityId))).thenReturn(settingAttribute);

      settingAuthHandler.authorize(appId, entityId);
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeWithIdNull() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CONNECTORS);

      settingAttribute.setCategory(CONNECTOR);
      UserThreadLocal.set(user);
      when(settingsService.get(eq(appId), eq(entityId))).thenReturn(null);

      settingAuthHandler.authorize(appId, entityId);
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuthorizeWithCloudProviderId() {
    boolean exceptionThrown = false;
    try {
      setPermissions(MANAGE_CLOUD_PROVIDERS);

      settingAttribute.setCategory(CLOUD_PROVIDER);
      UserThreadLocal.set(user);
      when(settingsService.get(eq(appId), eq(entityId))).thenReturn(settingAttribute);

      settingAuthHandler.authorize(appId, entityId);
    } catch (Exception e) {
      assertThat(e).isNull();
      exceptionThrown = true;
    } finally {
      UserThreadLocal.unset();
    }
    assertThat(exceptionThrown).isFalse();
  }
}
