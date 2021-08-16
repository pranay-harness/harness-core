package io.harness.accesscontrol.roles;

import static io.harness.accesscontrol.permissions.PermissionFilter.IncludedInAllRolesFilter.PERMISSIONS_INCLUDED_IN_ALL_ROLES;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import io.harness.accesscontrol.AccessControlTestBase;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.commons.bootstrap.ConfigurationState;
import io.harness.accesscontrol.commons.bootstrap.ConfigurationStateRepository;
import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.accesscontrol.permissions.PermissionsConfig;
import io.harness.accesscontrol.permissions.PermissionsManagementJob;
import io.harness.accesscontrol.resources.resourcetypes.ResourceTypeManagementJob;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.beans.PageRequest;
import io.harness.reflection.ReflectionUtils;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class RolesManagementJobTest extends AccessControlTestBase {
  @Inject private RoleService roleService;
  @Inject private ConfigurationStateRepository configurationStateRepository;
  @Inject private PermissionService permissionService;
  @Inject private ResourceTypeManagementJob resourceTypeManagementJob;
  @Inject private PermissionsManagementJob permissionsManagementJob;
  @Inject private RolesManagementJob rolesManagementJob;
  private static final String PERMISSIONS_CONFIG_FIELD = "permissionsConfig";
  private static final String ROLES_CONFIG_FIELD = "rolesConfig";
  private static final String VERSION_FIELD = "version";
  private static final String ACCOUNT_SCOPE_LEVEL = "account";
  private static final String RANDOM_NAME = "randomName";
  private static final String RANDOM_DESCRIPTION = "randomDescription";
  private static final String NEW_PERMISSION = "core_project_randomIdentifier";

  @Before
  public void setUp() {
    resourceTypeManagementJob.run();
    permissionsManagementJob.run();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testSave() {
    RolesConfig rolesConfig = (RolesConfig) ReflectionUtils.getFieldValue(rolesManagementJob, ROLES_CONFIG_FIELD);
    RolesConfig rolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(rolesConfig);

    rolesManagementJob.run();
    validate(rolesConfigClone);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdate() throws NoSuchFieldException, IllegalAccessException {
    Field f = rolesManagementJob.getClass().getDeclaredField(ROLES_CONFIG_FIELD);
    RolesConfig rolesConfig = (RolesConfig) ReflectionUtils.getFieldValue(rolesManagementJob, ROLES_CONFIG_FIELD);
    ReflectionUtils.setObjectField(rolesConfig.getClass().getDeclaredField(VERSION_FIELD), rolesConfig, 2);
    RolesConfig latestRolesConfig = (RolesConfig) NGObjectMapperHelper.clone(rolesConfig);
    RolesConfig currentRolesConfig =
        RolesConfig.builder().version(1).name(latestRolesConfig.getName()).roles(new HashSet<>()).build();
    RolesConfig currentRolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(currentRolesConfig);
    ReflectionUtils.setObjectField(f, rolesManagementJob, currentRolesConfig);
    rolesManagementJob.run();
    validate(currentRolesConfigClone);

    RolesConfig latestRolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(latestRolesConfig);
    ReflectionUtils.setObjectField(f, rolesManagementJob, latestRolesConfig);
    rolesManagementJob.run();
    validate(latestRolesConfigClone);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddNewRole() throws NoSuchFieldException, IllegalAccessException {
    rolesManagementJob.run();

    RolesConfig rolesConfig = (RolesConfig) ReflectionUtils.getFieldValue(rolesManagementJob, ROLES_CONFIG_FIELD);
    Set<Role> currentRoles = rolesConfig.getRoles();
    Set<String> allowedScopeLevels = new HashSet<>();
    allowedScopeLevels.add(ACCOUNT_SCOPE_LEVEL);
    currentRoles.add(Role.builder()
                         .identifier("randomIdentifier")
                         .name(RANDOM_NAME)
                         .description(RANDOM_DESCRIPTION)
                         .managed(true)
                         .allowedScopeLevels(allowedScopeLevels)
                         .permissions(new HashSet<>())
                         .build());

    int currentVersion = rolesConfig.getVersion();
    ReflectionUtils.setObjectField(
        rolesConfig.getClass().getDeclaredField(VERSION_FIELD), rolesConfig, currentVersion + 1);

    RolesConfig rolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(rolesConfig);

    rolesManagementJob.run();
    validate(rolesConfigClone);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRemoveOldRole() throws NoSuchFieldException, IllegalAccessException {
    Field f = rolesManagementJob.getClass().getDeclaredField(ROLES_CONFIG_FIELD);
    RolesConfig currentRolesConfig =
        (RolesConfig) ReflectionUtils.getFieldValue(rolesManagementJob, ROLES_CONFIG_FIELD);
    RolesConfig latestRolesConfig = (RolesConfig) NGObjectMapperHelper.clone(currentRolesConfig);
    ReflectionUtils.setObjectField(latestRolesConfig.getClass().getDeclaredField(VERSION_FIELD), latestRolesConfig,
        currentRolesConfig.getVersion() + 1);
    Set<String> allowedScopeLevels = new HashSet<>();
    allowedScopeLevels.add(ACCOUNT_SCOPE_LEVEL);
    currentRolesConfig.getRoles().add(Role.builder()
                                          .identifier("randomIdentifier")
                                          .name(RANDOM_NAME)
                                          .description(RANDOM_DESCRIPTION)
                                          .managed(true)
                                          .allowedScopeLevels(allowedScopeLevels)
                                          .permissions(new HashSet<>())
                                          .build());

    RolesConfig currentRolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(currentRolesConfig);
    rolesManagementJob.run();
    validate(currentRolesConfigClone);

    ReflectionUtils.setObjectField(f, rolesManagementJob, latestRolesConfig);
    RolesConfig latestRolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(latestRolesConfig);
    rolesManagementJob.run();
    validate(latestRolesConfigClone);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRemovePermissionFromAccountScope() throws NoSuchFieldException, IllegalAccessException {
    PermissionsConfig permissionsConfig =
        (PermissionsConfig) ReflectionUtils.getFieldValue(permissionsManagementJob, PERMISSIONS_CONFIG_FIELD);
    permissionsConfig.getPermissions().clear();
    Set<String> allowedScopeLevels = new HashSet<>();
    allowedScopeLevels.add(ACCOUNT_SCOPE_LEVEL);
    allowedScopeLevels.add("organization");
    allowedScopeLevels.add("project");
    permissionsConfig.getPermissions().add(Permission.builder()
                                               .identifier(NEW_PERMISSION)
                                               .name(RANDOM_NAME)
                                               .status(PermissionStatus.ACTIVE)
                                               .includeInAllRoles(false)
                                               .allowedScopeLevels(allowedScopeLevels)
                                               .build());
    ReflectionUtils.setObjectField(permissionsConfig.getClass().getDeclaredField(VERSION_FIELD), permissionsConfig,
        permissionsConfig.getVersion() + 1);
    permissionsManagementJob.run();

    Set<String> permissions = new HashSet<>();
    permissions.add(NEW_PERMISSION);
    RolesConfig rolesConfig = (RolesConfig) ReflectionUtils.getFieldValue(rolesManagementJob, ROLES_CONFIG_FIELD);
    rolesConfig.getRoles().clear();
    Set<String> accountScopeLevel = new HashSet<>();
    accountScopeLevel.add(ACCOUNT_SCOPE_LEVEL);
    rolesConfig.getRoles().add(Role.builder()
                                   .identifier("randomIdentifierWithAccountPermission")
                                   .name("randomNameWithAccountPermission")
                                   .description(RANDOM_DESCRIPTION)
                                   .managed(true)
                                   .allowedScopeLevels(accountScopeLevel)
                                   .permissions(permissions)
                                   .build());
    Set<String> orgScopeLevel = new HashSet<>();
    orgScopeLevel.add("organization");
    rolesConfig.getRoles().add(Role.builder()
                                   .identifier("randomIdentifierWithOrgPermission")
                                   .name("randomNameWithOrgPermission")
                                   .description(RANDOM_DESCRIPTION)
                                   .managed(true)
                                   .allowedScopeLevels(orgScopeLevel)
                                   .permissions(permissions)
                                   .build());

    RolesConfig rolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(rolesConfig);
    rolesManagementJob.run();
    validate(rolesConfigClone);

    allowedScopeLevels.remove(ACCOUNT_SCOPE_LEVEL);
    permissionsConfig.getPermissions().forEach(permission -> permission.setAllowedScopeLevels(allowedScopeLevels));
    ReflectionUtils.setObjectField(permissionsConfig.getClass().getDeclaredField(VERSION_FIELD), permissionsConfig,
        permissionsConfig.getVersion() + 1);
    permissionsManagementJob.run();

    ReflectionUtils.setObjectField(
        rolesConfig.getClass().getDeclaredField(VERSION_FIELD), rolesConfig, rolesConfig.getVersion() + 1);
    try {
      rolesManagementJob.run();
      fail();
    } catch (InvalidArgumentsException exception) {
      // exceptions are good
    }

    rolesConfig.getRoles().forEach(role -> {
      if (!role.getAllowedScopeLevels().contains("organization") && !role.getAllowedScopeLevels().contains("project")) {
        role.getPermissions().remove(NEW_PERMISSION);
      }
    });
    rolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(rolesConfig);
    rolesManagementJob.run();
    validate(rolesConfigClone);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidPermission() {
    Set<String> permissions = new HashSet<>();
    permissions.add(NEW_PERMISSION);
    RolesConfig rolesConfig = (RolesConfig) ReflectionUtils.getFieldValue(rolesManagementJob, ROLES_CONFIG_FIELD);
    Set<String> accountScopeLevel = new HashSet<>();
    accountScopeLevel.add(ACCOUNT_SCOPE_LEVEL);
    rolesConfig.getRoles().add(Role.builder()
                                   .identifier("randomIdentifier")
                                   .name(RANDOM_NAME)
                                   .description(RANDOM_DESCRIPTION)
                                   .managed(true)
                                   .allowedScopeLevels(accountScopeLevel)
                                   .permissions(permissions)
                                   .build());

    RolesConfig rolesConfigClone = (RolesConfig) NGObjectMapperHelper.clone(rolesConfig);
    rolesManagementJob.run();
    validate(rolesConfigClone);
  }

  private void validate(RolesConfig rolesConfig) {
    Optional<ConfigurationState> optional = configurationStateRepository.getByIdentifier(rolesConfig.getName());
    assertTrue(optional.isPresent());
    assertEquals(rolesConfig.getVersion(), optional.get().getConfigVersion());

    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(100).build();
    RoleFilter roleFilter = RoleFilter.builder().managedFilter(ManagedFilter.ONLY_MANAGED).build();

    Set<Role> currentRoles = new HashSet<>(roleService.list(pageRequest, roleFilter).getContent());
    assertEquals(rolesConfig.getRoles().size(), currentRoles.size());
    Map<String, Role> currentRolesMap = new HashMap<>();
    currentRoles.forEach(role -> currentRolesMap.put(role.getIdentifier() + "#" + role.getScopeIdentifier(), role));

    rolesConfig.getRoles().forEach(role -> {
      Role currentRole = currentRolesMap.get(role.getIdentifier() + "#" + role.getScopeIdentifier());
      assertNotNull(currentRole);
      assertEquals(role.getAllowedScopeLevels(), currentRole.getAllowedScopeLevels());
      assertEquals(role.getName(), currentRole.getName());
      assertEquals(role.getIdentifier(), currentRole.getIdentifier());
      assertEquals(role.getScopeIdentifier(), currentRole.getScopeIdentifier());
      role.getPermissions().forEach(permission -> assertTrue(currentRole.getPermissions().contains(permission)));
    });

    PermissionFilter permissionFilter =
        PermissionFilter.builder().includedInAllRolesFilter(PERMISSIONS_INCLUDED_IN_ALL_ROLES).build();
    Set<Permission> currentIncludeInAllRolesPermissions = new HashSet<>(permissionService.list(permissionFilter));
    currentRoles.forEach(currentRole -> currentIncludeInAllRolesPermissions.forEach(permission -> {
      if (!Sets.intersection(currentRole.getAllowedScopeLevels(), permission.getAllowedScopeLevels()).isEmpty()) {
        assertTrue(currentRole.getPermissions().contains(permission.getIdentifier()));
      }
    }));
  }
}
