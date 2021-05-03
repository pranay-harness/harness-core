package io.harness.ng.core.invites.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.invites.InviteOperationResponse.FAIL;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.USER_INITIATED_INVITE;
import static io.harness.ng.core.invites.remote.InviteMapper.writeDTO;
import static io.harness.ng.core.user.UserMembershipUpdateSource.ACCEPTED_INVITE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.Team;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.invites.remote.InviteAcceptResponse;
import io.harness.mongo.MongoConfig;
import io.harness.ng.accesscontrol.user.ACLAggregateFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.events.UserInviteCreateEvent;
import io.harness.ng.core.events.UserInviteDeleteEvent;
import io.harness.ng.core.events.UserInviteUpdateEvent;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.invites.remote.RoleBinding.RoleBindingKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.EmailChannel.EmailChannelBuilder;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.outbox.api.OutboxService;
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
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import org.apache.http.client.utils.URIBuilder;
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
  private static final int DEFAULT_PAGE_SIZE = 1000;
  private static final String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
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
  private final OutboxService outboxService;
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final String currentGenUiUrl;

  private final RetryPolicy<Object> transactionRetryPolicy =
      RetryUtils.getRetryPolicy("[Retrying]: Failed to mark previous invites as stale; attempt: {}",
          "[Failed]: Failed to mark previous invites as stale; attempt: {}",
          ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public InviteServiceImpl(@Named("userVerificationSecret") String jwtPasswordSecret, MongoConfig mongoConfig,
      JWTGeneratorUtils jwtGeneratorUtils, NgUserService ngUserService, TransactionTemplate transactionTemplate,
      InviteRepository inviteRepository, NotificationClient notificationClient,
      AccessControlAdminClient accessControlAdminClient, AccountClient accountClient, OutboxService outboxService,
      OrganizationService organizationService, ProjectService projectService,
      @Named("currentGenUiUrl") String currentGenUiUrl) {
    this.jwtPasswordSecret = jwtPasswordSecret;
    this.jwtGeneratorUtils = jwtGeneratorUtils;
    this.ngUserService = ngUserService;
    this.inviteRepository = inviteRepository;
    this.transactionTemplate = transactionTemplate;
    this.notificationClient = notificationClient;
    this.accessControlAdminClient = accessControlAdminClient;
    this.accountClient = accountClient;
    this.outboxService = outboxService;
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.currentGenUiUrl = currentGenUiUrl;
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
        roleBinding.setResourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER);
        roleBinding.setResourceGroupName(DEFAULT_RESOURCE_GROUP_NAME);
      }
    });
  }

  private boolean checkIfUserAlreadyAdded(Invite invite) {
    Optional<UserInfo> userOptional = ngUserService.getUserFromEmail(invite.getEmail());
    return userOptional
        .filter(user
            -> ngUserService.isUserAtScope(user.getUuid(),
                Scope.builder()
                    .accountIdentifier(invite.getAccountIdentifier())
                    .orgIdentifier(invite.getOrgIdentifier())
                    .projectIdentifier(invite.getProjectIdentifier())
                    .build()))
        .isPresent();
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
  public PageResponse<InviteDTO> getPendingInvites(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    validateRequest(searchTerm, aclAggregateFilter);
    PageResponse<Invite> invitesPage =
        getInvitePage(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest, aclAggregateFilter);
    List<String> userEmails = invitesPage.getContent().stream().map(Invite::getEmail).collect(toList());
    Map<String, UserMetadataDTO> userSearchDTOs = getPendingUserMap(userEmails, accountIdentifier);
    List<InviteDTO> inviteDTOS = aggregatePendingUsers(invitesPage.getContent(), userSearchDTOs);
    return PageUtils.getNGPageResponse(invitesPage, inviteDTOS);
  }

  @Override
  public Optional<Invite> deleteInvite(String inviteId) {
    return wrapperForTransactions(this::deleteInviteInternal, inviteId);
  }

  private Optional<Invite> deleteInviteInternal(String inviteId) {
    Optional<Invite> inviteOptional = getInvite(inviteId);
    if (inviteOptional.isPresent()) {
      Update update = new Update().set(InviteKeys.deleted, TRUE);
      Invite updatedInvite = inviteRepository.updateInvite(inviteId, update);
      if (updatedInvite != null) {
        outboxService.save(new UserInviteDeleteEvent(updatedInvite.getAccountIdentifier(),
            updatedInvite.getOrgIdentifier(), updatedInvite.getProjectIdentifier(), writeDTO(updatedInvite)));
      }
      return updatedInvite == null ? Optional.empty() : Optional.of(updatedInvite);
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
    outboxService.save(
        new UserInviteUpdateEvent(existingInvite.getAccountIdentifier(), existingInvite.getOrgIdentifier(),
            existingInvite.getProjectIdentifier(), writeDTO(existingInvite), writeDTO(newInvite)));
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
    Optional<UserInfo> ngUserOpt = ngUserService.getUserFromEmail(invite.getEmail());
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
    if (!inviteOptional.isPresent() || TRUE.equals(inviteOptional.get().getApproved())) {
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
    outboxService.save(new UserInviteCreateEvent(
        invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier(), writeDTO(invite)));
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
      templateData.put("projectname",
          getProjectName(invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier()));
    } else if (!isBlank(invite.getOrgIdentifier())) {
      templateData.put("organizationname", getOrgName(invite.getAccountIdentifier(), invite.getOrgIdentifier()));
    } else {
      templateData.put("accountname", getAccountName(invite.getAccountIdentifier()));
    }
    emailChannelBuilder.templateData(templateData);
    notificationClient.sendNotificationAsync(emailChannelBuilder.build());
  }

  private String getProjectName(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<Project> projectOpt = projectService.get(accountIdentifier, orgIdentifier, projectIdentifier);
    if (!projectOpt.isPresent()) {
      throw new IllegalStateException(String.format("Project with identifier [%s] doesn't exists", projectIdentifier));
    }
    return projectOpt.get().getName();
  }

  private String getOrgName(String accountIdentifier, String orgIdentifier) {
    Optional<Organization> organizationOpt = organizationService.get(accountIdentifier, orgIdentifier);
    if (!organizationOpt.isPresent()) {
      throw new IllegalStateException(String.format("Organization with identifier [%s] doesn't exists", orgIdentifier));
    }
    return organizationOpt.get().getName();
  }

  private String getAccountName(String accountIdentifier) {
    AccountDTO account = RestClientUtils.getResponse(accountClient.getAccountDTO(accountIdentifier));
    if (account == null) {
      throw new IllegalStateException(String.format("Account with identifier [%s] doesn't exists", accountIdentifier));
    }
    return account.getName();
  }

  private String getInvitationMailEmbedUrl(Invite invite) throws URISyntaxException {
    String accountBaseUrl = RestClientUtils.getResponse(accountClient.getBaseUrl(invite.getAccountIdentifier()));
    AccountDTO account = RestClientUtils.getResponse(accountClient.getAccountDTO(invite.getAccountIdentifier()));
    String fragment = String.format(INVITE_URL, invite.getAccountIdentifier(), account.getName(),
        account.getCompanyName(), invite.getEmail(), invite.getInviteToken());
    if (Objects.isNull(accountBaseUrl)) {
      accountBaseUrl = currentGenUiUrl;
    }
    URIBuilder uriBuilder = new URIBuilder(accountBaseUrl);
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
    Optional<UserInfo> userOpt = ngUserService.getUserFromEmail(email);
    Preconditions.checkState(userOpt.isPresent(), "Illegal state: user doesn't exits");
    UserInfo user = userOpt.get();
    Scope scope = Scope.builder()
                      .accountIdentifier(invite.getAccountIdentifier())
                      .orgIdentifier(invite.getOrgIdentifier())
                      .projectIdentifier(invite.getProjectIdentifier())
                      .build();

    ngUserService.addUserToScope(user, scope, ACCEPTED_INVITE);
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

  private void validateRequest(String searchTerm, ACLAggregateFilter aclAggregateFilter) {
    if (!isBlank(searchTerm) && ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      log.error("Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
      throw new InvalidRequestException(
          "Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
    }
  }

  private PageResponse<Invite> getInvitePage(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    Criteria criteria = Criteria.where(InviteKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(InviteKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(InviteKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(InviteKeys.deleted)
                            .is(FALSE);
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      if (isNotEmpty(aclAggregateFilter.getRoleIdentifiers())) {
        criteria.and(InviteKeys.roleBindings + "." + RoleBindingKeys.roleIdentifier)
            .in(aclAggregateFilter.getRoleIdentifiers());
      }
      if (isNotEmpty(aclAggregateFilter.getResourceGroupIdentifiers())) {
        criteria.and(InviteKeys.roleBindings + "." + RoleBindingKeys.resourceGroupIdentifier)
            .in(aclAggregateFilter.getResourceGroupIdentifiers());
      }
    }
    if (!isBlank(searchTerm)) {
      Page<UserInfo> userInfos = ngUserService.listCurrentGenUsers(
          accountIdentifier, searchTerm, org.springframework.data.domain.PageRequest.of(0, DEFAULT_PAGE_SIZE));
      List<String> emailIds = userInfos.stream().map(UserInfo::getEmail).collect(toList());
      Criteria searchTermCriteria = new Criteria();
      searchTermCriteria.orOperator(
          Criteria.where(InviteKeys.email).regex(quote(searchTerm)), Criteria.where(InviteKeys.email).in(emailIds));
      criteria = new Criteria().andOperator(criteria, searchTermCriteria);
    }
    return getInvites(criteria, pageRequest);
  }

  private Map<String, UserMetadataDTO> getPendingUserMap(List<String> userEmails, String accountIdentifier) {
    List<UserInfo> users = ngUserService.getUsersFromEmail(userEmails, accountIdentifier);
    Map<String, UserMetadataDTO> userSearchMap = new HashMap<>();
    users.forEach(user
        -> userSearchMap.put(user.getEmail(),
            UserMetadataDTO.builder().email(user.getEmail()).name(user.getName()).uuid(user.getUuid()).build()));
    for (String email : userEmails) {
      userSearchMap.computeIfAbsent(email, email1 -> UserMetadataDTO.builder().email(email1).build());
    }
    return userSearchMap;
  }

  private List<InviteDTO> aggregatePendingUsers(List<Invite> invites, Map<String, UserMetadataDTO> userMap) {
    Preconditions.checkState(invites.stream().map(Invite::getEmail).distinct().count() == userMap.size(),
        "Number of invites should be same as number of invites. Invariant violated");
    List<InviteDTO> inviteDTOs = new ArrayList<>();
    for (Invite invite : invites) {
      UserMetadataDTO user = userMap.get(invite.getEmail());
      inviteDTOs.add(InviteDTO.builder()
                         .id(invite.getId())
                         .name(user.getName())
                         .email(user.getEmail())
                         .roleBindings(invite.getRoleBindings())
                         .inviteType(invite.getInviteType())
                         .build());
    }
    return inviteDTOs;
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
