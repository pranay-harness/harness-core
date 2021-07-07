package io.harness.accesscontrol.acl.api;

import static io.harness.exception.WingsException.USER;
import static io.harness.security.dto.PrincipalType.SERVICE;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ACLResourceHelper {
  public static boolean serviceContextAndNoPrincipalInBody(
      io.harness.security.dto.Principal principalInContext, Principal principalToCheckPermissions) {
    /*
     check if principal in context is SERVICE and principalToCheckPermissions is either null or
     the same service principal
     */
    Optional<io.harness.security.dto.Principal> serviceCall =
        Optional.ofNullable(principalInContext).filter(x -> SERVICE.equals(x.getType()));

    return serviceCall.isPresent()
        && (principalToCheckPermissions == null
            || Objects.equals(serviceCall.get().getName(), principalToCheckPermissions.getPrincipalIdentifier()));
  }

  public static boolean userContextAndDifferentPrincipalInBody(
      io.harness.security.dto.Principal principalInContext, Principal principalToCheckPermissions) {
    /* check if a principal of type other than SERVICE is trying to check permissions for any
       other principal */
    Optional<io.harness.security.dto.Principal> nonServiceCall =
        Optional.ofNullable(principalInContext).filter(x -> !SERVICE.equals(x.getType()));
    return nonServiceCall.isPresent()
        && (principalToCheckPermissions != null
            && !Objects.equals(principalInContext.getName(), principalToCheckPermissions.getPrincipalIdentifier()));
  }

  public static void checkForValidContextOrThrow(io.harness.security.dto.Principal principalInContext) {
    if (principalInContext == null || principalInContext.getName() == null || principalInContext.getType() == null) {
      throw new InvalidRequestException("Missing principal in context.", USER);
    }
  }

  public static Optional<String> getAccountIdentifier(List<PermissionCheckDTO> permissionCheckDTOList) {
    if (permissionCheckDTOList.isEmpty()) {
      return Optional.empty();
    }

    PermissionCheckDTO permissionCheckDTO = permissionCheckDTOList.get(0);
    if (permissionCheckDTO.getResourceScope() == null
        || StringUtils.isEmpty(permissionCheckDTO.getResourceScope().getAccountIdentifier())) {
      return Optional.of(permissionCheckDTO.getResourceIdentifier());
    } else {
      return Optional.of(permissionCheckDTO.getResourceScope().getAccountIdentifier());
    }
  }

  public static AccessControlDTO getAccessControlDTO(PermissionCheckDTO permissionCheckDTO, boolean permitted) {
    return AccessControlDTO.builder()
        .permission(permissionCheckDTO.getPermission())
        .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
        .resourceScope(permissionCheckDTO.getResourceScope())
        .resourceType(permissionCheckDTO.getResourceType())
        .permitted(permitted)
        .build();
  }

  public static boolean notPresent(Principal principal) {
    return !Optional.ofNullable(principal).map(Principal::getPrincipalIdentifier).filter(x -> !x.isEmpty()).isPresent();
  }
}
