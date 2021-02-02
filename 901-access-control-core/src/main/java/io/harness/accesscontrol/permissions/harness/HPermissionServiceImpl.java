package io.harness.accesscontrol.permissions.harness;

import io.harness.accesscontrol.permissions.PermissionDTO;
import io.harness.accesscontrol.permissions.core.PermissionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class HPermissionServiceImpl implements HPermissionService {
  private final PermissionService permissionService;

  @Inject
  public HPermissionServiceImpl(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Override
  public Optional<PermissionDTO> get(String identifier) {
    return permissionService.get(identifier);
  }

  @Override
  public List<PermissionDTO> list(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String resourceType) {
    return new ArrayList<>();
  }
}
