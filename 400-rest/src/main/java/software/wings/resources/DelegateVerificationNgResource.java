/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_EDIT_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_RESOURCE_TYPE;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateHeartbeatDetails;
import io.harness.delegate.beans.DelegateInitializationDetails;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;

@Api("delegates-verification")
@Path("/delegates-verification")
@Produces(MediaType.APPLICATION_JSON)
@AuthRule(permissionType = LOGGED_IN)
//@Scope(DELEGATE)
// This NG specific, switching to NG access control
@Slf4j
@OwnedBy(DEL)
public class DelegateVerificationNgResource {
  private final DelegateService delegateService;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateVerificationNgResource(DelegateService delegateService, AccessControlClient accessControlClient) {
    this.delegateService = delegateService;
    this.accessControlClient = accessControlClient;
  }

  @GET
  @Path("/heartbeat")
  public RestResponse<DelegateHeartbeatDetails> getDelegatesHeartbeatDetails(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, @QueryParam("sessionId") @NotEmpty String sessionIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_EDIT_PERMISSION);

    List<String> registeredDelegateIds = delegateService.obtainDelegateIds(accountId, sessionIdentifier);

    if (CollectionUtils.isNotEmpty(registeredDelegateIds)) {
      List<String> connectedDelegates = delegateService.getConnectedDelegates(accountId, registeredDelegateIds);

      return new RestResponse<>(DelegateHeartbeatDetails.builder()
                                    .numberOfRegisteredDelegates(registeredDelegateIds.size())
                                    .numberOfConnectedDelegates(connectedDelegates.size())
                                    .build());
    }

    return new RestResponse<>(DelegateHeartbeatDetails.builder().build());
  }

  @GET
  @Path("/heartbeatV2")
  public RestResponse<DelegateHeartbeatDetails> getDelegatesHeartbeatDetailsV2(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, @QueryParam("delegateName") @NotEmpty String delegateName) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_EDIT_PERMISSION);

    List<String> registeredDelegateIds = delegateService.obtainDelegateIdsUsingName(accountId, delegateName);

    if (CollectionUtils.isNotEmpty(registeredDelegateIds)) {
      List<String> connectedDelegates = delegateService.getConnectedDelegates(accountId, registeredDelegateIds);

      return new RestResponse<>(DelegateHeartbeatDetails.builder()
                                    .numberOfRegisteredDelegates(registeredDelegateIds.size())
                                    .numberOfConnectedDelegates(connectedDelegates.size())
                                    .build());
    }

    return new RestResponse<>(DelegateHeartbeatDetails.builder().build());
  }

  @GET
  @Path("/initialized")
  public RestResponse<List<DelegateInitializationDetails>> getDelegatesInitializationDetails(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, @QueryParam("sessionId") @NotEmpty String sessionIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_EDIT_PERMISSION);

    List<String> registeredDelegateIds = delegateService.obtainDelegateIds(accountId, sessionIdentifier);

    if (CollectionUtils.isNotEmpty(registeredDelegateIds)) {
      return new RestResponse<>(delegateService.obtainDelegateInitializationDetails(accountId, registeredDelegateIds));
    }

    return new RestResponse<>(Collections.emptyList());
  }

  @GET
  @Path("/initializedV2")
  public RestResponse<List<DelegateInitializationDetails>> getDelegatesInitializationDetailsV2(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, @QueryParam("delegateName") @NotEmpty String delegateName) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_EDIT_PERMISSION);

    List<String> registeredDelegateIds = delegateService.obtainDelegateIdsUsingName(accountId, delegateName);

    if (CollectionUtils.isNotEmpty(registeredDelegateIds)) {
      return new RestResponse<>(delegateService.obtainDelegateInitializationDetails(accountId, registeredDelegateIds));
    }

    return new RestResponse<>(Collections.emptyList());
  }
}
