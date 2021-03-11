package io.harness.ng.core.invites.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.invites.InviteOperationResponse.FAIL;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.USER_INITIATED_INVITE;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.Team;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.mongo.MongoConfig;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.EmailChannel.EmailChannelBuilder;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.repositories.invites.spring.InvitesRepository;
import io.harness.utils.PageUtils;
import io.harness.utils.RetryUtils;

import com.auth0.jwt.interfaces.Claim;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoClientURI;
import com.mongodb.client.result.UpdateResult;
import java.net.URI;
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
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
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
  private static final String PROJECT_URL_FORMAT = "account/%s/projects/%s/orgs/%s/details";
  private static final String ORG_URL_FORMAT = "account/%s/admin/organizations/%s";
  private static final String ACCOUNT_URL_FORMAT = "account/%s/projects";
  private static final String SIGN_UP_URL = "#/login";
  private final String uiBaseUrl;
  private final String ngUiBaseUrl;

  private final String jwtPasswordSecret;
  private final JWTGeneratorUtils jwtGeneratorUtils;
  private final NgUserService ngUserService;
  private final InvitesRepository invitesRepository;
  private final String verificationBaseUrl;
  private final boolean useMongoTransactions;
  private final TransactionTemplate transactionTemplate;
  private final NotificationClient notificationClient;
  private final AccessControlAdminClient accessControlAdminClient;

  private final RetryPolicy<Object> transactionRetryPolicy =
      RetryUtils.getRetryPolicy("[Retrying]: Failed to mark previous invites as stale; attempt: {}",
          "[Failed]: Failed to mark previous invites as stale; attempt: {}",
          ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public InviteServiceImpl(@Named("ngManagerBaseUrl") String baseURL,
      @Named("userVerificationSecret") String jwtPasswordSecret, MongoConfig mongoConfig,
      JWTGeneratorUtils jwtGeneratorUtils, NgUserService ngUserService, TransactionTemplate transactionTemplate,
      InvitesRepository invitesRepository, NotificationClient notificationClient, @Named("uiBaseUrl") String uiBaseUrl,
      @Named("ngUiBaseUrl") String ngUiBaseUrl, AccessControlAdminClient accessControlAdminClient) {
    this.jwtPasswordSecret = jwtPasswordSecret;
    this.jwtGeneratorUtils = jwtGeneratorUtils;
    this.ngUserService = ngUserService;
    this.invitesRepository = invitesRepository;
    this.transactionTemplate = transactionTemplate;
    this.notificationClient = notificationClient;
    this.ngUiBaseUrl = ngUiBaseUrl;
    this.uiBaseUrl = uiBaseUrl;
    this.accessControlAdminClient = accessControlAdminClient;
    verificationBaseUrl = baseURL + "invites/verify?token=%s&accountIdentifier=%s";
    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri());
    useMongoTransactions = uri.getHosts().size() > 2;
  }

  @Override
  public InviteOperationResponse create(Invite invite) {
    try {
      if (null == invite) {
        return FAIL;
      }
      if (checkIfUserAlreadyAdded(invite)) {
        return InviteOperationResponse.USER_ALREADY_ADDED;
      }
      Optional<Invite> existingInviteOptional = getExistingInvite(invite);
      if (existingInviteOptional.isPresent() && TRUE.equals(existingInviteOptional.get().getApproved())) {
        return InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
      }
      return createInvite(invite, existingInviteOptional.orElse(null));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(getExceptionMessage(invite), USER_SRE, ex);
    }
  }

  private boolean checkIfUserAlreadyAdded(Invite invite) {
    Optional<User> userOptional = ngUserService.getUserFromEmail(invite.getEmail());
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
      return wrapperForTransactions(this::newInviteHandler, newInvite);
    }
    wrapperForTransactions(this::processResendInvitationMail, existingInvite, newInvite);
    return InviteOperationResponse.USER_INVITE_RESENT;
  }

  @Override
  public Optional<Invite> get(String inviteId) {
    return invitesRepository.findDistinctByIdAndDeleted(inviteId, FALSE);
  }

  @Override
  public PageResponse<Invite> getAllPendingInvites(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = Criteria.where(InviteKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(InviteKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(InviteKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(InviteKeys.deleted)
                            .is(false);
    return PageUtils.getNGPageResponse(invitesRepository.findAll(criteria, pageable));
  }

  @Override
  public Page<Invite> list(@NotNull Criteria criteria, Pageable pageable) {
    return invitesRepository.findAll(criteria, pageable);
  }

  @Override
  public Optional<Invite> deleteInvite(String inviteId) {
    Optional<Invite> inviteOptional = get(inviteId);
    if (inviteOptional.isPresent()) {
      Update update = new Update().set(InviteKeys.deleted, TRUE);
      UpdateResult updateResponse = invitesRepository.updateInvite(inviteId, update);
      return updateResponse.getModifiedCount() != 1 ? Optional.empty() : inviteOptional;
    }
    return Optional.empty();
  }

  public Invite processResendInvitationMail(Invite existingInvite, Invite newInvite) {
    // check if the creator of the invite has permissions to create role assignments.
    Update update = new Update()
                        .set(InviteKeys.createdAt, new Date())
                        .set(InviteKeys.validUntil,
                            Date.from(OffsetDateTime.now().plusDays(INVITATION_VALIDITY_IN_DAYS).toInstant()))
                        .set(InviteKeys.roleAssignments, newInvite.getRoleAssignments());
    invitesRepository.updateInvite(existingInvite.getId(), update);
    sendInvitationMail(existingInvite);
    return existingInvite;
  }

  public Optional<Invite> verify(String jwtToken) {
    Optional<String> inviteIdOptional = getInviteIdFromToken(jwtToken);
    if (!inviteIdOptional.isPresent()) {
      throw new InvalidArgumentsException("Invalid token. verification failed");
    }
    Optional<Invite> inviteOptional = get(inviteIdOptional.get());

    Optional<Invite> returnInviteOptional = Optional.empty();
    if (!inviteOptional.isPresent()) {
      log.warn("Invite token {} for usermail expired. Retry", jwtToken);
    } else if (!inviteOptional.get().getInviteToken().equals(jwtToken)) {
      log.warn("Invite token {} is invalid", jwtToken);
    } else {
      Invite invite = inviteOptional.get();
      Optional<User> userOptional = ngUserService.getUserFromEmail(invite.getEmail());
      returnInviteOptional = Optional.of(processInvite(invite, userOptional.orElse(null)));
    }
    return returnInviteOptional;
  }

  @Override
  public Optional<Invite> updateInvite(Invite updatedInvite) {
    Optional<Invite> inviteOptional = get(updatedInvite.getId());
    if (!inviteOptional.isPresent()) {
      return Optional.empty();
    }

    Invite existingInvite = inviteOptional.get();
    if (existingInvite.getInviteType() == ADMIN_INITIATED_INVITE) {
      //      check whether the user updating the invite has role assignment permissions
      wrapperForTransactions(this::processResendInvitationMail, existingInvite, updatedInvite);
      return Optional.of(existingInvite);
    } else if (existingInvite.getInviteType() == USER_INITIATED_INVITE && updatedInvite.getApproved().equals(TRUE)) {
      throw new UnsupportedOperationException("User initiated requests are not supported yet");
    }
    return Optional.empty();
  }

  @Override
  public URI getRedirectURIForAcceptedInvite(Invite invite) {
    if (Objects.isNull(invite)) {
      return null;
    }
    URI redirectURI = null;
    if (TRUE.equals(invite.getDeleted())) {
      if (!isBlank(invite.getProjectIdentifier())) {
        redirectURI = URI.create(ngUiBaseUrl
            + String.format(PROJECT_URL_FORMAT, invite.getAccountIdentifier(), invite.getProjectIdentifier(),
                invite.getOrgIdentifier()));
      } else if (isBlank(invite.getOrgIdentifier())) {
        redirectURI = URI.create(
            ngUiBaseUrl + String.format(ORG_URL_FORMAT, invite.getAccountIdentifier(), invite.getOrgIdentifier()));
      } else {
        redirectURI = URI.create(ngUiBaseUrl + String.format(ACCOUNT_URL_FORMAT, invite.getAccountIdentifier()));
      }
    } else if (TRUE.equals(invite.getApproved())) {
      redirectURI = URI.create(uiBaseUrl + SIGN_UP_URL);
    }
    return redirectURI;
  }

  @Override
  public boolean newUserEventHandler(String userId, String emailId) {
    List<Invite> unprocessedInvites = findAllUnprocessedInvites(emailId);
    for (Invite invite : unprocessedInvites) {
      if (!useMongoTransactions) {
        processInvite(invite, userId, emailId);
      } else {
        Failsafe.with(transactionRetryPolicy)
            .get(() -> transactionTemplate.execute(status -> processInvite(invite, userId, emailId)));
      }
    }
    return true;
  }

  private List<Invite> findAllUnprocessedInvites(String emailId) {
    return invitesRepository.findAllByEmailAndApprovedAndDeleted(emailId, true, false);
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
    return invitesRepository
        .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
            invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier(), invite.getEmail(),
            invite.getInviteType(), FALSE);
  }

  private InviteOperationResponse newInviteHandler(Invite invite) {
    Invite savedInvite = invitesRepository.save(invite);
    sendInvitationMail(savedInvite);
    return InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
  }

  private List<RoleAssignmentDTO> createRoleAssignments(Invite invite, List<RoleAssignmentDTO> roleAssignments) {
    List<RoleAssignmentResponseDTO> createdRoleAssignmentResponseDTOs;
    try {
      createdRoleAssignmentResponseDTOs = getResponse(
          accessControlAdminClient.createMultiRoleAssignment(invite.getAccountIdentifier(), invite.getOrgIdentifier(),
              invite.getProjectIdentifier(), new RoleAssignmentCreateRequestDTO(roleAssignments)));
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
    invitesRepository.updateInvite(invite.getId(), update);
  }

  private void sendInvitationMail(Invite invite) {
    updateJWTTokenInInvite(invite);
    EmailChannelBuilder emailChannelBuilder = EmailChannel.builder()
                                                  .accountId(invite.getAccountIdentifier())
                                                  .recipients(Collections.singletonList(invite.getEmail()))
                                                  .team(Team.PL)
                                                  .templateId("email_invite")
                                                  .userGroupIds(Collections.emptyList());
    Map<String, String> templateData = new HashMap<>();
    String url = String.format(verificationBaseUrl, invite.getInviteToken(), invite.getAccountIdentifier());
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

  private Invite processInvite(Invite invite, User user) {
    if (Objects.nonNull(user)) {
      return processInvite(invite, user.getUuid(), user.getEmail());
    }
    markInviteApproved(invite);
    return invite;
  }

  private Invite processInvite(Invite invite, String userId, String emailId) {
    invite.getRoleAssignments().forEach(roleAssignment -> roleAssignment.setDisabled(false));
    createRoleAssignments(invite, invite.getRoleAssignments());
    ngUserService.addUserToScope(userId, emailId,
        Scope.builder()
            .accountIdentifier(invite.getAccountIdentifier())
            .orgIdentifier(invite.getOrgIdentifier())
            .projectIdentifier(invite.getProjectIdentifier())
            .build());
    markInviteApprovedAndDeleted(invite);
    return invite;
  }

  private void markInviteApproved(Invite invite) {
    invite.setApproved(TRUE);
    Update update = new Update().set(InviteKeys.approved, TRUE);
    invitesRepository.updateInvite(invite.getId(), update);
  }

  private void markInviteApprovedAndDeleted(Invite invite) {
    invite.setApproved(TRUE);
    invite.setDeleted(TRUE);
    Update update = new Update().set(InviteKeys.approved, TRUE).set(InviteKeys.deleted, TRUE);
    invitesRepository.updateInvite(invite.getId(), update);
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
