package io.harness.accesscontrol.permissions.harness;

import io.harness.accesscontrol.permissions.PermissionDTO;

import java.util.List;
import java.util.Optional;
import org.hibernate.validator.constraints.NotEmpty;

public interface HPermissionService {
  Optional<PermissionDTO> get(@NotEmpty String identifier);

  List<PermissionDTO> list(
      @NotEmpty String accountIdentifier, String orgIdentifier, String projectIdentifier, String resourceType);
}
