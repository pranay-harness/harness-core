package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.principals.PrincipalType;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

public interface AccessControlClient {
  /**
   * Given principal, principal type and list of permissions on which access check is to be applied, it returns a list
   * of access control dtos, which contain whether the given principal has aforementioned permissions in the same order
   * It is guaranteed that this API will return a list of the same length as that of the list in the request
   * @param principal principal
   * @param principalType type of principal
   * @param permissionCheckDTOList list of permissions along with resource on which the permission is to checked
   * @return returns a list of access control dto
   */
  AccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckDTOList);

  AccessControlDTO checkForAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO);

  AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList);

  AccessControlDTO checkForAccess(PermissionCheckDTO permissionCheckDTO);

  boolean hasAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO);

  boolean hasAccess(PermissionCheckDTO permissionCheckDTO);

  void checkAccessOrThrow(@NotEmpty String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotEmpty String resourceType, @NotEmpty String resourceIdentifier, @NotEmpty String permissionIdentifier);
}
