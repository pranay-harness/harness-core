package io.harness.security;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.security.dto.PrincipalType.USER;

import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Singleton;
import java.util.function.Predicate;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
@Singleton
@Priority(AUTHORIZATION)
public class UserPrincipalVerificationFilter implements ContainerRequestFilter {
  @Context private ResourceInfo resourceInfo;
  private final Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate;
  private final NgUserService ngUserService;

  public UserPrincipalVerificationFilter(
      Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate, NgUserService ngUserService) {
    this.predicate = predicate;
    this.ngUserService = ngUserService;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (predicate.test(Pair.of(resourceInfo, containerRequestContext))) {
      if (SecurityContextBuilder.getPrincipal() != null
          && USER.equals(SecurityContextBuilder.getPrincipal().getType())) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextBuilder.getPrincipal();
        MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();
        String accountIdentifier = queryParameters.getFirst(ACCOUNT_KEY);
        if (isNotBlank(userPrincipal.getAccountId())
            && !ngUserService.isUserInAccount(userPrincipal.getAccountId(), userPrincipal.getUserId())) {
          throw new InvalidRequestException(
              String.format("User does not belong to account %s", userPrincipal.getAccountId()), USER_DOES_NOT_EXIST,
              WingsException.USER);
        }
        // for environments without gateway, we don't change the accountId in token while switching accounts so
        // validating accountIdentifier == accountId is wrong
        if (isNotBlank(accountIdentifier)
            && !ngUserService.isUserInAccount(accountIdentifier, userPrincipal.getUserId())) {
          throw new InvalidRequestException(String.format("User does not belong to account %s", accountIdentifier),
              USER_DOES_NOT_EXIST, WingsException.USER);
        }
      }
    }
  }
}
