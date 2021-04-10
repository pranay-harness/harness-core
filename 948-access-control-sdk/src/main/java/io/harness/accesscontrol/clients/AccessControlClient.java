package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlClient {
  /**
   * @param principal identifier of the principal for which permission is to be checked
   * @param principalType type of principal for which permission is to be checked
   * @param permissionCheckDTOList list of permissions to check
   * @return principal for which permissions were checked and list of AccessControlDTO, which contains permission and a
   * field named accessible denoting whether aforementioned principal has the given permission
   */
  AccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckDTOList);

  /**
   * @param principal identifier of the principal for which permission is to be checked
   * @param principalType type of principal for which permission is to be checked
   * @param permissionCheckDTO a single permission which is to be checked for aforementioned principal
   * @return a single AccessControlDTO, which contains a field named accessible denoting whether principal has the said
   * permission
   */
  AccessControlDTO checkForAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO);

  /**
   * Since this API takes no principal, it is picked up from the request context
   * @param permissionCheckDTOList list of permissions to check
   * @return principal for which permissions were checked for, and list of AccessControlDTO, which contains permission
   * and a field named accessible denoting * whether aforementioned principal has the given permission
   */
  AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList);

  /**
   * Since this API takes no principal, it is picked up from the request context
   * @param permissionCheckDTO single permission to check
   * @return single AccessControlDTO, which contains principal for which permission was checked and a
   * field called accessible denoting whether principal has said permission
   */
  AccessControlDTO checkForAccess(PermissionCheckDTO permissionCheckDTO);

  /**
   * @param principal identifier of the principal for which permission is to be checked
   * @param principalType type of principal for which permission is to be checked
   * @param permissionCheckDTO single permission which is to be checked for the principal
   * @return a boolean value denoting whether the principal has said permission
   */
  boolean hasAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO);

  /**
   * since this API takes no principal, it is taken up from the request context
   * @param permissionCheckDTO single permission to check
   * @return boolean value denoting whether principal taken up from request context has above permission
   */
  boolean hasAccess(@Valid @NotNull PermissionCheckDTO permissionCheckDTO);

  void checkForAccessOrThrow(ResourceScope resourceScope, Resource resource, String permission);
}
