package io.harness.ng.core.invites.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.invites.InviteOperationResponse.FAIL;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.USER_INITIATED_INVITE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.Team;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.mongo.MongoConfig;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.account.remote.AccountClient;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.invites.InviteAcceptResponse;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.EmailChannel.EmailChannelBuilder;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.invites.spring.InviteRepository;
import io.harness.utils.PageUtils;
import io.harness.utils.RetryUtils;

import com.auth0.jwt.interfaces.Claim;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoClientURI;
import com.mongodb.client.result.UpdateResult;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Singleton
@Slf4j
@OwnedBy(PL)
public class InviteServiceImpl implements InviteService {
  private static final int INVITATION_VALIDITY_IN_DAYS = 30;
  private static final int LINK_VALIDITY_IN_DAYS = 7;
  private static final String INVITE_URL =
      "/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s&generation=NG";
  private final String jwtPasswordSecret;
  private final JWTGeneratorUtils jwtGeneratorUtils;
  private final NgUserService ngUserService;
  private final InviteRepository inviteRepository;
  private final boolean useMongoTransactions;
  private final TransactionTemplate transactionTemplate;
  private final NotificationClient notificationClient;
  private final AccessControlAdminClient accessControlAdminClient;
  private final AccountClient accountClient;
  private final String uiBaseUrl;

  private final RetryPolicy<Object> transactionRetryPolicy =
      RetryUtils.getRetryPolicy("[Retrying]: Failed to mark previous invites as stale; attempt: {}",
          "[Failed]: Failed to mark previous invites as stale; attempt: {}",
          ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public InviteServiceImpl(@Named("userVerificationSecret") String jwtPasswordSecret, MongoConfig mongoConfig,
      JWTGeneratorUtils jwtGeneratorUtils, NgUserService ngUserService, TransactionTemplate transactionTemplate,
      InviteRepository inviteRepository, NotificationClient notificationClient,
      AccessControlAdminClient accessControlAdminClient, AccountClient accountClient,
      @Named("uiBaseUrl") String uiBaseUrl) {
    this.jwtPasswordSecret = jwtPasswordSecret;
    this.jwtGeneratorUtils = jwtGeneratorUtils;
    this.ngUserService = ngUserService;
    this.inviteRepository = inviteRepository;
    this.transactionTemplate = transactionTemplate;
    this.notificationClient = notificationClient;
    this.accessControlAdminClient = accessControlAdminClient;
    this.accountClient = accountClient;
    this.uiBaseUrl = uiBaseUrl;
    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri());
    useMongoTransactions = uri.getHosts().size() > 2;
  }

  @Override
  public InviteOperationResponse create(Invite invite) {
    if (invite == null) {
      return FAIL;
    }
    preCreateInvite(invite);
    if (checkIfUserAlreadyAdded(invite)) {
      return InviteOperationResponse.USER_ALREADY_ADDED;
    }
    Optional<Invite> existingInviteOptional = getExistingInvite(invite);
    if (existingInviteOptional.isPresent() && TRUE.equals(existingInviteOptional.get().getApproved())) {
      return InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
    }
    try {
      return createInvite(invite, existingInviteOptional.orElse(null));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(getExceptionMessage(invite), USER_SRE, ex);
    }
  }

  private void preCreateInvite(Invite invite) {
    List<RoleBinding> roleBindings = invite.getRoleBindings();
    roleBindings.forEach(roleBinding -> {
      if (isBlank(roleBinding.getResourceGroupIdentifier())) {
        Pair<String, String> resourceGroup = getResourceGroupIdentifier(
            invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier());
        roleBinding.setResourceGroupIdentifier(resourceGroup.getLeft());
        roleBinding.setResourceGroupName(resourceGroup.getRight());
      }
    });
  }

  private static Pair<String, String> getResourceGroupIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    String resourceGroupName;
    if (projectIdentifier != null) {
      resourceGroupName = projectIdentifier;
    } else if (orgIdentifier != null) {
      resourceGroupName = orgIdentifier;
    } else {
      resourceGroupName = accountIdentifier;
    }
    return Pair.of(String.format("_%s", resourceGroupName), resourceGroupName);
  }

  private boolean checkIfUserAlreadyAdded(Invite invite) {
    Optional<UserInfo> userOptional = ngUserService.getUserFromEmail(invite.getEmail(), invite.getAccountIdentifier());
    if (!userOptional.isPresent()) {
      return false;
    }
    Optional<UserMembership> userMembershipOptional = ngUserService.getUserMembership(userOptional.get().getUuid());
    Scope scope = Scope.builder()
                      .accountIdentifier(invite.getAccountIdentifier())
                      .orgIdentifier(invite.getOrgIdentifier())
                      .projectIdentifier(invite.getProjectIdentifier())
                      .build();
    return userMembershipOptional.isPresent() && userMembershipOptional.get().getScopes().contains(scope);
  }

  private InviteOperationResponse createInvite(Invite newInvite, Invite existingInvite) {
    if (Objects.isNull(existingInvite)) {
      return wrapperForTransactions(this::newInvite, newInvite);
    }
    wrapperForTransactions(this::resendInvite, existingInvite, newInvite);
    return InviteOperationResponse.USER_ALREADY_INVITED;
  }

  @Override
  public Optional<Invite> getInvite(String inviteId) {
    return inviteRepository.findFirstByIdAndDeleted(inviteId, FALSE);
  }

  @Override
  public PageResponse<Invite> getInvites(Criteria criteria, PageRequest pageRequest) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    return PageUtils.getNGPageResponse(inviteRepository.findAll(criteria, pageable));
  }

  @Override
  public Optional<Invite> deleteInvite(String inviteId) {
    Optional<Invite> inviteOptional = getInvite(inviteId);
    if (inviteOptional.isPresent()) {
      Update update = new Update().set(InviteKeys.deleted, TRUE);
      UpdateResult updateResponse = inviteRepository.updateInvite(inviteId, update);
      return updateResponse.getModifiedCount() != 1 ? Optional.empty() : inviteOptional;
    }
    return Optional.empty();
  }

  @Override
  public boolean deleteInvite(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String emailId) {
    Optional<Invite> inviteOptional =
        inviteRepository.findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
            accountIdentifier, orgIdentifier, projectIdentifier, emailId);
    if (!inviteOptional.isPresent()) {
      return false;
    }
    deleteInvite(inviteOptional.get().getId());
    return true;
  }

  private Invite resendInvite(Invite existingInvite, Invite newInvite) {
    Update update = new Update()
                        .set(InviteKeys.createdAt, new Date())
                        .set(InviteKeys.validUntil,
                            Date.from(OffsetDateTime.now().plusDays(INVITATION_VALIDITY_IN_DAYS).toInstant()))
                        .set(InviteKeys.roleBindings, newInvite.getRoleBindings());
    inviteRepository.updateInvite(existingInvite.getId(), update);
    try {
      sendInvitationMail(existingInvite);
    } catch (URISyntaxException e) {
      log.error("Mail embed url incorrect. can't sent email", e);
    }
    return existingInvite;
  }

  public InviteAcceptResponse acceptInvite(String jwtToken) {
    Optional<Invite> inviteOptional = getInviteFromToken(jwtToken);
    if (!inviteOptional.isPresent() || !inviteOptional.get().getInviteToken().equals(jwtToken)) {
      log.warn("Invite token {} is invalid", jwtToken);
      return InviteAcceptResponse.builder().response(InviteOperationResponse.FAIL).build();
    }

    Invite invite = inviteOptional.get();
    Optional<UserInfo> ngUserOpt = ngUserService.getUserFromEmail(invite.getEmail(), invite.getAccountIdentifier());
    markInviteApproved(invite);
    return InviteAcceptResponse.builder()
        .response(InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED)
        .userInfo(ngUserOpt.orElse(null))
        .build();
  }

  private Optional<Invite> getInviteFromToken(String jwtToken) {
    if (isBlank(jwtToken)) {
      return Optional.empty();
    }
    Optional<String> inviteIdOptional = Optional.empty();
    try {
      inviteIdOptional = getInviteIdFromToken(jwtToken);
    } catch (InvalidRequestException e) {
      log.error("Invalid invite JWT token", e);
    }
    if (!inviteIdOptional.isPresent()) {
      log.warn("Invalid token. verification failed");
      return Optional.empty();
    }
    return getInvite(inviteIdOptional.get());
  }

  @Override
  public Optional<Invite> updateInvite(Invite updatedInvite) {
    if (updatedInvite == null) {
      return Optional.empty();
    }
    preCreateInvite(updatedInvite);
    Optional<Invite> inviteOptional = getInvite(updatedInvite.getId());
    if (!inviteOptional.isPresent()) {
      return Optional.empty();
    }

    Invite existingInvite = inviteOptional.get();
    if (existingInvite.getInviteType() == ADMIN_INITIATED_INVITE) {
      wrapperForTransactions(this::resendInvite, existingInvite, updatedInvite);
      return Optional.of(existingInvite);
    } else if (existingInvite.getInviteType() == USER_INITIATED_INVITE) {
      throw new UnsupportedOperationException("User initiated requests are not supported yet");
    }
    return Optional.empty();
  }

  private String getExceptionMessage(Invite invite) {
    String message = String.format("Invite [%s] under account [%s] ", invite.getId(), invite.getAccountIdentifier());
    if (!isBlank(invite.getProjectIdentifier())) {
      message = message
          + String.format(",organization [%s] and project [%s] already exists", invite.getOrgIdentifier(),
              invite.getProjectIdentifier());
    } else if (!isBlank(invite.getOrgIdentifier())) {
      message = message + String.format("and organization [%s] already exists", invite.getOrgIdentifier());
    } else {
      message = message + "already exists";
    }
    return message;
  }

  private Optional<Invite> getExistingInvite(Invite invite) {
    return inviteRepository.findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
        invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier(), invite.getEmail());
  }

  private InviteOperationResponse newInvite(Invite invite) {
    Invite savedInvite = inviteRepository.save(invite);
    try {
      sendInvitationMail(savedInvite);
    } catch (URISyntaxException e) {
      log.error("Mail embed url incorrect. can't sent email", e);
    }
    return InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
  }

  private List<RoleAssignmentDTO> createRoleAssignments(Invite invite, String userId) {
    List<RoleBinding> roleBindings = invite.getRoleBindings();
    List<RoleAssignmentDTO> newRoleAssignments =
        roleBindings.stream()
            .map(roleBinding
                -> RoleAssignmentDTO.builder()
                       .roleIdentifier(roleBinding.getRoleIdentifier())
                       .resourceGroupIdentifier(roleBinding.getResourceGroupIdentifier())
                       .principal(PrincipalDTO.builder().type(PrincipalType.USER).identifier(userId).build())
                       .disabled(false)
                       .build())
            .collect(Collectors.toList());
    List<RoleAssignmentResponseDTO> createdRoleAssignmentResponseDTOs;
    try {
      createdRoleAssignmentResponseDTOs = NGRestUtils.getResponse(accessControlAdminClient.createMultiRoleAssignment(
          invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier(),
          RoleAssignmentCreateRequestDTO.builder().roleAssignments(newRoleAssignments).build()));
    } catch (Exception e) {
      log.error("Can't create roleassignments while inviting {}", invite.getEmail());
      return Collections.emptyList();
    }
    return createdRoleAssignmentResponseDTOs.stream()
        .map(RoleAssignmentResponseDTO::getRoleAssignment)
        .collect(Collectors.toList());
  }

  private void updateJWTTokenInInvite(Invite invite) {
    String jwtToken = jwtGeneratorUtils.generateJWTToken(ImmutableMap.of(InviteKeys.id, invite.getId()),
        TimeUnit.MILLISECONDS.convert(LINK_VALIDITY_IN_DAYS, TimeUnit.DAYS), jwtPasswordSecret);
    invite.setInviteToken(jwtToken);
    Update update = new Update().set(InviteKeys.inviteToken, invite.getInviteToken());
    inviteRepository.updateInvite(invite.getId(), update);
  }

  private void sendInvitationMail(Invite invite) throws URISyntaxException {
    updateJWTTokenInInvite(invite);
    String url = getInvitationMailEmbedUrl(invite);
    EmailChannelBuilder emailChannelBuilder = EmailChannel.builder()
                                                  .accountId(invite.getAccountIdentifier())
                                                  .recipients(Collections.singletonList(invite.getEmail()))
                                                  .team(Team.PL)
                                                  .templateId("email_invite")
                                                  .userGroupIds(Collections.emptyList());
    Map<String, String> templateData = new HashMap<>();
    templateData.put("url", url);
    if (!isBlank(invite.getProjectIdentifier())) {
      templateData.put("projectname", invite.getProjectIdentifier());
    } else if (!isBlank(invite.getOrgIdentifier())) {
      templateData.put("organizationname", invite.getOrgIdentifier());
    } else {
      templateData.put("accountname", invite.getAccountIdentifier());
    }
    emailChannelBuilder.templateData(templateData);
    notificationClient.sendNotificationAsync(emailChannelBuilder.build());
  }

  private String getInvitationMailEmbedUrl(Invite invite) throws URISyntaxException {
    //    String baseUrl = RestClientUtils.getResponse(accountClient.getBaseUrl(invite.getAccountIdentifier()));
    AccountDTO account = RestClientUtils.getResponse(accountClient.getAccountDTO(invite.getAccountIdentifier()));
    String fragment = String.format(INVITE_URL, invite.getAccountIdentifier(), account.getName(),
        account.getCompanyName(), invite.getEmail(), invite.getInviteToken());
    URIBuilder uriBuilder = new URIBuilder(uiBaseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public boolean completeInvite(String token) {
    Optional<Invite> inviteOpt = getInviteFromToken(token);
    if (!inviteOpt.isPresent()) {
      return false;
    }
    Invite invite = inviteOpt.get();
    String email = invite.getEmail();
    Optional<UserInfo> userOpt = ngUserService.getUserFromEmail(email, invite.getAccountIdentifier());
    Preconditions.checkState(userOpt.isPresent(), "Illegal state: user doesn't exits");
    UserInfo user = userOpt.get();
    Scope scope = Scope.builder()
                      .accountIdentifier(invite.getAccountIdentifier())
                      .orgIdentifier(invite.getOrgIdentifier())
                      .projectIdentifier(invite.getProjectIdentifier())
                      .build();
    ngUserService.addUserToScope(user, scope);
    createRoleAssignments(invite, user.getUuid());
    markInviteApprovedAndDeleted(invite);
    return true;
  }

  private void markInviteApproved(Invite invite) {
    invite.setApproved(TRUE);
    Update update = new Update().set(InviteKeys.approved, TRUE);
    inviteRepository.updateInvite(invite.getId(), update);
  }

  private void markInviteApprovedAndDeleted(Invite invite) {
    invite.setApproved(TRUE);
    invite.setDeleted(TRUE);
    Update update = new Update().set(InviteKeys.approved, TRUE).set(InviteKeys.deleted, TRUE);
    inviteRepository.updateInvite(invite.getId(), update);
  }

  private Optional<String> getInviteIdFromToken(String token) {
    Map<String, Claim> claims = jwtGeneratorUtils.verifyJWTToken(token, jwtPasswordSecret);
    if (!claims.containsKey(InviteKeys.id)) {
      return Optional.empty();
    }
    return Optional.of(claims.get(InviteKeys.id).asString());
  }

  private <T, S> S wrapperForTransactions(Function<T, S> function, T arg) {
    if (!useMongoTransactions) {
      return function.apply(arg);
    } else {
      return Failsafe.with(transactionRetryPolicy)
          .get(() -> transactionTemplate.execute(status -> function.apply(arg)));
    }
  }

  private <T, U, R> R wrapperForTransactions(BiFunction<T, U, R> function, T arg1, U arg2) {
    if (!useMongoTransactions) {
      return function.apply(arg1, arg2);
    } else {
      return Failsafe.with(transactionRetryPolicy)
          .get(() -> transactionTemplate.execute(status -> function.apply(arg1, arg2)));
    }
  }
}
