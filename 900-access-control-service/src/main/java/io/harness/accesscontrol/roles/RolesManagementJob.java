package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class RolesManagementJob {
  private static final String ROLES_YAML_PATH = "io/harness/accesscontrol/roles/managed-roles.yml";

  private final Roles latestRoles;
  private final Roles currentRoles;
  private final RoleService roleService;

  @Inject
  public RolesManagementJob(RoleService roleService) {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(ROLES_YAML_PATH);
    try {
      byte[] bytes = Resources.toByteArray(url);
      this.latestRoles = om.readValue(bytes, Roles.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Roles file path or format is invalid");
    }
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(100).build();
    RoleFilter roleFilter = RoleFilter.builder().managedFilter(ManagedFilter.ONLY_MANAGED).build();
    this.currentRoles =
        Roles.builder().roles(new HashSet<>(roleService.list(pageRequest, roleFilter).getContent())).build();
    this.roleService = roleService;
  }

  public void run() {
    Set<Role> addedOrUpdatedPermissions = Sets.difference(latestRoles.getRoles(), currentRoles.getRoles());

    Set<String> latestIdentifiers =
        latestRoles.getRoles().stream().map(Role::getIdentifier).collect(Collectors.toSet());
    Set<String> currentIdentifiers =
        currentRoles.getRoles().stream().map(Role::getIdentifier).collect(Collectors.toSet());

    Set<String> addedIdentifiers = Sets.difference(latestIdentifiers, currentIdentifiers);
    Set<String> removedIdentifiers = Sets.difference(currentIdentifiers, latestIdentifiers);

    Set<Role> addedRoles = addedOrUpdatedPermissions.stream()
                               .filter(p -> addedIdentifiers.contains(p.getIdentifier()))
                               .collect(Collectors.toSet());

    Set<Role> updatedRoles = addedOrUpdatedPermissions.stream()
                                 .filter(p -> !addedIdentifiers.contains(p.getIdentifier()))
                                 .collect(Collectors.toSet());

    addedRoles.forEach(roleService::create);
    updatedRoles.forEach(roleService::update);
    removedIdentifiers.forEach(identifier -> roleService.delete(identifier, null));
  }
}
