package io.harness.ng.core.invites.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.USER_INITIATED_INVITE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.RetryUtils;
import io.harness.ng.core.invites.api.InvitesService;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.invites.ext.mail.EmailData;
import io.harness.ng.core.invites.ext.mail.MailUtils;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.repositories.invites.spring.InvitesRepository;

import com.auth0.jwt.interfaces.Claim;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoClientURI;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
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
public class InvitesServiceImpl implements InvitesService {
  private static final int INVITATION_VALIDITY_IN_DAYS = 30;
  private static final int LINK_VALIDITY_IN_DAYS = 7;
  private static final String MAIL_SUBJECT_FORMAT_FOR_PROJECT = "Invitation for project %s on Harness";
  private static final String MAIL_SUBJECT_FORMAT_FOR_ORGANIZATION = "Invitation for organisation %s on Harness";
  private final String jwtPasswordSecret;
  private final JWTGeneratorUtils jwtGeneratorUtils;
  private final MailUtils mailUtils;
  private final NgUserService ngUserService;
  private final InvitesRepository invitesRepository;
  private final String verificationBaseUrl;
  private final boolean useMongoTransactions;
  private final TransactionTemplate transactionTemplate;

  private final RetryPolicy<Object> transactionRetryPolicy =
      RetryUtils.getRetryPolicy("[Retrying]: Failed to mark previous invites as stale; attempt: {}",
          "[Failed]: Failed to mark previous invites as stale; attempt: {}",
          ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public InvitesServiceImpl(@Named("baseUrl") String baseURL, @Named("userVerificationSecret") String jwtPasswordSecret,
      MongoConfig mongoConfig, JWTGeneratorUtils jwtGeneratorUtils, MailUtils mailUtils, NgUserService ngUserService,
      TransactionTemplate transactionTemplate, InvitesRepository invitesRepository) {
    this.jwtPasswordSecret = jwtPasswordSecret;
    this.jwtGeneratorUtils = jwtGeneratorUtils;
    this.mailUtils = mailUtils;
    this.ngUserService = ngUserService;
    this.invitesRepository = invitesRepository;
    this.transactionTemplate = transactionTemplate;
    verificationBaseUrl = baseURL + "ng/api/invites/verify?token=%s&accountId=%s";
    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri());
    useMongoTransactions = uri.getHosts().size() > 2;
  }

  @Override
  public InviteOperationResponse create(Invite invite) {
    try {
      if (null == invite) {
        return InviteOperationResponse.FAIL;
      }

      Optional<User> userOptional = ngUserService.getUserFromEmail(invite.getAccountIdentifier(), invite.getEmail());
      Optional<Invite> existingInviteOptional = getExistingInvite(invite);

      if (userOptional.isPresent()) {
        // Approved and deleted fields of Invite will be toggled together for this case
        User user = userOptional.get();
        // If invitee is already part of the project
        Optional<UserProjectMap> userProjectMapOptional = ngUserService.getUserProjectMap(
            user.getUuid(), invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier());

        if (userProjectMapOptional.isPresent() && userProjectMapOptional.get().getRoles().contains(invite.getRole())) {
          return InviteOperationResponse.USER_ALREADY_ADDED;
        }

        if (!existingInviteOptional.isPresent()) {
          return wrapperForTransactions(this::newInviteHandler, invite);
        }
        resendInvitationMail(existingInviteOptional.get());
        return InviteOperationResponse.USER_INVITE_RESENT;
      }

      // Case: user is not registered in the account
      if (!existingInviteOptional.isPresent()) {
        return wrapperForTransactions(this::newInviteHandler, invite);
      }
      Invite existingInvite = existingInviteOptional.get();
      if (Boolean.TRUE.equals(existingInvite.getApproved())) {
        return InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
      }
      resendInvitationMail(existingInvite);
      return InviteOperationResponse.USER_INVITE_RESENT;

    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(isEmpty(invite.getProjectIdentifier())
              ? String.format("Invite [%s] under account [%s] and organization [%s] already exists", invite.getId(),
                  invite.getAccountIdentifier(), invite.getOrgIdentifier())
              : String.format("Invite [%s] under account [%s], organization [%s] and project [%s] already exists",
                  invite.getId(), invite.getAccountIdentifier(), invite.getOrgIdentifier(),
                  invite.getProjectIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Invite resendInvitationMail(Invite invite) {
    updateCreationDate(invite);
    sendInvitationMail(invite);
    return invite;
  }

  private Optional<Invite> getExistingInvite(Invite invite) {
    return invitesRepository
        .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
            invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier(), invite.getEmail(),
            invite.getRole(), invite.getInviteType(), Boolean.TRUE);
  }

  private void markPreviousInvitesDeleted(Invite newInvite) {
    List<Invite> existingInvitesList =
        invitesRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedNot(
            newInvite.getAccountIdentifier(), newInvite.getOrgIdentifier(), newInvite.getProjectIdentifier(),
            newInvite.getEmail(), Boolean.TRUE);

    // Marking all old invites as deleted as only the current role in the invitation is net valid.
    for (Invite tempInvite : existingInvitesList) {
      if (tempInvite.getRole().equals(newInvite.getRole()) && tempInvite.getInviteType() == newInvite.getInviteType()) {
        continue;
      }
      tempInvite.setDeleted(Boolean.TRUE);
    }
    invitesRepository.saveAll(existingInvitesList);
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

  private InviteOperationResponse newInviteHandler(Invite invite) {
    markPreviousInvitesDeleted(invite);
    Invite savedInvite = invitesRepository.save(invite);
    sendInvitationMail(savedInvite);
    return InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
  }

  @Override
  public Optional<Invite> get(String inviteId) {
    return invitesRepository.findFirstByIdAndDeletedNot(inviteId, Boolean.TRUE);
  }

  @Override
  public Page<Invite> list(@NotNull Criteria criteria, Pageable pageable) {
    return invitesRepository.findAll(criteria, pageable);
  }

  @Override
  public Optional<Invite> deleteInvite(String inviteId) {
    Optional<Invite> inviteOptional = get(inviteId);
    if (inviteOptional.isPresent()) {
      Update update = new Update().set(InviteKeys.deleted, Boolean.TRUE);
      UpdateResult updateResponse = invitesRepository.updateInvite(inviteId, update);
      return updateResponse.getModifiedCount() != 1 ? Optional.empty() : inviteOptional;
    }
    return Optional.empty();
  }

  private void updateJWTTokenInInvite(Invite invite) {
    String jwtToken = jwtGeneratorUtils.generateJWTToken(ImmutableMap.of(InviteKeys.id, invite.getId()),
        TimeUnit.MILLISECONDS.convert(LINK_VALIDITY_IN_DAYS, TimeUnit.DAYS), jwtPasswordSecret);
    invite.setInviteToken(jwtToken);
    Update update = new Update().set(InviteKeys.inviteToken, invite.getInviteToken());
    invitesRepository.updateInvite(invite.getId(), update);
  }

  private boolean sendInvitationMail(Invite invite) {
    updateJWTTokenInInvite(invite);
    String url = String.format(verificationBaseUrl, invite.getInviteToken(), invite.getAccountIdentifier());
    String subject;
    if (isEmpty(invite.getProjectIdentifier())) {
      subject = String.format(MAIL_SUBJECT_FORMAT_FOR_ORGANIZATION, invite.getOrgIdentifier());
    } else {
      subject = String.format(MAIL_SUBJECT_FORMAT_FOR_PROJECT, invite.getProjectIdentifier());
    }
    EmailData emailData = EmailData.builder().to(ImmutableList.of(invite.getEmail())).subject(subject).build();
    emailData.setTemplateName("invite");
    if (isEmpty(invite.getProjectIdentifier())) {
      emailData.setTemplateModel(ImmutableMap.of("organizationname", invite.getOrgIdentifier(), "url", url));
    } else {
      emailData.setTemplateModel(ImmutableMap.of("projectname", invite.getProjectIdentifier(), "url", url));
    }
    emailData.setRetries(2);
    emailData.setAccountId(invite.getAccountIdentifier());
    mailUtils.sendMailAsync(emailData);
    return Boolean.TRUE;
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
      Optional<User> userOptional = ngUserService.getUserFromEmail(invite.getAccountIdentifier(), invite.getEmail());

      if (userOptional.isPresent()) {
        wrapperForTransactions((arg1, arg2) -> {
          ngUserService.createUserProjectMap(arg1, arg2);
          markInviteApprovedAndDeleted(arg1);
          return arg1;
        }, invite, userOptional.get());
      } else {
        markInviteApproved(invite);
      }
      returnInviteOptional = inviteOptional;
    }
    return returnInviteOptional;
  }

  private Boolean approveUserRequest(Invite invite, Role role) {
    if (invite == null || role == null) {
      return Boolean.FALSE;
    }
    Optional<User> userOptional = ngUserService.getUserFromEmail(invite.getAccountIdentifier(), invite.getEmail());
    wrapperForTransactions((arg1, arg2) -> {
      Update update = new Update().set(InviteKeys.role, role);
      invitesRepository.updateInvite(invite.getId(), update);
      if (userOptional.isPresent()) {
        ngUserService.createUserProjectMap(invite, userOptional.get());
        markInviteApprovedAndDeleted(invite);
      } else {
        markInviteApproved(invite);
      }
      return arg1;
    }, invite, userOptional);
    return Boolean.TRUE;
  }

  private void updateCreationDate(Invite invite) {
    Update update = new Update()
                        .set(InviteKeys.createdAt, new Date())
                        .set(InviteKeys.validUntil,
                            Date.from(OffsetDateTime.now().plusDays(INVITATION_VALIDITY_IN_DAYS).toInstant()));
    invitesRepository.updateInvite(invite.getId(), update);
  }

  @Override
  public Optional<Invite> updateInvite(Invite updatedInvite) {
    Optional<Invite> inviteOptional = get(updatedInvite.getId());
    if (!inviteOptional.isPresent()) {
      return Optional.empty();
    }

    Invite invite = inviteOptional.get();
    if (invite.getInviteType() == ADMIN_INITIATED_INVITE) {
      resendInvitationMail(invite);
    } else if (invite.getInviteType() == USER_INITIATED_INVITE && updatedInvite.getApproved().equals(Boolean.TRUE)) {
      approveUserRequest(invite, updatedInvite.getRole());
    }
    return Optional.of(invite);
  }

  private void markInviteApproved(Invite invite) {
    invite.setApproved(Boolean.TRUE);
    Update update = new Update().set(InviteKeys.approved, Boolean.TRUE);
    invitesRepository.updateInvite(invite.getId(), update);
  }

  private void markInviteApprovedAndDeleted(Invite invite) {
    invite.setApproved(Boolean.TRUE);
    invite.setDeleted(Boolean.TRUE);
    Update update = new Update().set(InviteKeys.approved, Boolean.TRUE).set(InviteKeys.deleted, Boolean.TRUE);
    invitesRepository.updateInvite(invite.getId(), update);
  }

  private Optional<String> getInviteIdFromToken(String token) {
    Map<String, Claim> claims = jwtGeneratorUtils.verifyJWTToken(token, jwtPasswordSecret);
    if (!claims.containsKey(InviteKeys.id)) {
      return Optional.empty();
    }
    return Optional.of(claims.get(InviteKeys.id).asString());
  }
}
