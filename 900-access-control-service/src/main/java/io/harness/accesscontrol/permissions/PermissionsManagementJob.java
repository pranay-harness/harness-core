package io.harness.accesscontrol.permissions;

import io.harness.exception.InvalidRequestException;

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
public class PermissionsManagementJob {
  private static final String PERMISSIONS_YAML_PATH = "io/harness/accesscontrol/permissions/permissions.yml";

  private final Permissions latestPermissions;
  private final Permissions currentPermissions;
  private final PermissionService permissionService;

  @Inject
  public PermissionsManagementJob(PermissionService permissionService) {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(PERMISSIONS_YAML_PATH);
    try {
      byte[] bytes = Resources.toByteArray(url);
      this.latestPermissions = om.readValue(bytes, Permissions.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Permissions file path is invalid");
    }
    this.currentPermissions =
        Permissions.builder()
            .permissions(new HashSet<>(permissionService.list(PermissionFilter.builder().build())))
            .build();
    this.permissionService = permissionService;
  }

  public void run() {
    Set<Permission> addedOrUpdatedPermissions =
        Sets.difference(latestPermissions.getPermissions(), currentPermissions.getPermissions());

    Set<String> latestIdentifiers =
        latestPermissions.getPermissions().stream().map(Permission::getIdentifier).collect(Collectors.toSet());
    Set<String> currentIdentifiers =
        currentPermissions.getPermissions().stream().map(Permission::getIdentifier).collect(Collectors.toSet());

    Set<String> addedIdentifiers = Sets.difference(latestIdentifiers, currentIdentifiers);
    Set<String> removedIdentifiers = Sets.difference(currentIdentifiers, latestIdentifiers);

    Set<Permission> addedPermissions = addedOrUpdatedPermissions.stream()
                                           .filter(p -> addedIdentifiers.contains(p.getIdentifier()))
                                           .collect(Collectors.toSet());

    Set<Permission> updatedPermissions = addedOrUpdatedPermissions.stream()
                                             .filter(p -> !addedIdentifiers.contains(p.getIdentifier()))
                                             .collect(Collectors.toSet());

    addedPermissions.forEach(permissionService::create);
    updatedPermissions.forEach(permissionService::update);
    removedIdentifiers.forEach(permissionService::delete);
  }
}
