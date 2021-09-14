/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.invites.mapper.RoleBindingMapper.createRoleAssignmentDTOs;
import static io.harness.rule.OwnerRule.KARAN;

import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.Team;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.AccountOrgProjectHelper;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.invites.dto.RoleBinding;
import io.harness.ng.core.user.AddUserResponse;
import io.harness.ng.core.user.AddUsersDTO;
import io.harness.ng.core.user.AddUsersResponse;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.ng.core.user.entities.UserMetadata.UserMetadataKeys;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.repositories.user.spring.UserMetadataRepository;
import io.harness.rule.Owner;
import io.harness.user.remote.UserClient;
import io.harness.utils.PageTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class NgUserServiceImplTest extends CategoryTest {
  @Mock private UserClient userClient;
  @Mock private UserMembershipRepository userMembershipRepository;
  @Mock private AccessControlAdminClient accessControlAdminClient;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private OutboxService outboxService;
  @Mock private UserGroupService userGroupService;
  @Mock private UserMetadataRepository userMetadataRepository;
  @Mock private ExecutorService executorService;
  @Mock private NotificationClient notificationClient;
  @Mock private AccountOrgProjectHelper accountOrgProjectHelper;
  @Spy @Inject @InjectMocks private NgUserServiceImpl ngUserService;
  private String accountIdentifier;
  private String orgIdentifier;
  private static final String ACCOUNT_VIEWER = "_account_viewer";
  private static final String ORGANIZATION_VIEWER = "_organization_viewer";
  private static final String PROJECT_VIEWER = "_project_viewer";

  @Before
  public void setup() throws NoSuchFieldException {
    initMocks(this);
    accountIdentifier = randomAlphabetic(10);
    orgIdentifier = randomAlphabetic(10);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void listUsers() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).build();
    String userId = randomAlphabetic(10);
    List<String> userIds = Collections.singletonList(userId);
    List<UserMetadata> userMetadata = Collections.singletonList(UserMetadata.builder().userId(userId).build());

    final ArgumentCaptor<Criteria> userMembershipCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    final ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(userMembershipRepository.findAllUserIds(any(), any())).thenReturn(PageTestUtils.getPage(userIds, 1));
    when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(userMetadata, 1));

    ngUserService.listUsers(scope, pageRequest, null);

    verify(userMembershipRepository, times(1)).findAllUserIds(userMembershipCriteriaArgumentCaptor.capture(), any());
    verify(userMetadataRepository, times(1)).findAll(userMetadataCriteriaArgumentCaptor.capture(), any());

    Criteria userMembershipCriteria = userMembershipCriteriaArgumentCaptor.getValue();
    assertNotNull(userMembershipCriteria);
    String userMembershipCriteriaAccount =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY);
    String userMembershipCriteriaOrg =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.ORG_IDENTIFIER_KEY);
    String userMembershipCriteriaProject =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY);
    assertEquals(accountIdentifier, userMembershipCriteriaAccount);
    assertNull(userMembershipCriteriaOrg);
    assertNull(userMembershipCriteriaProject);
    assertEquals(3, userMembershipCriteria.getCriteriaObject().size());

    assertUserMetadataCriteria(userMetadataCriteriaArgumentCaptor, userId);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void listUsersStrictlyParentScopes() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    String userId = randomAlphabetic(10);
    List<String> userIds = Collections.singletonList(userId);
    List<UserMetadata> userMetadata = Collections.singletonList(UserMetadata.builder().userId(userId).build());

    final ArgumentCaptor<Criteria> userMembershipCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    final ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(userMembershipRepository.findAllUserIds(any(), any())).thenReturn(PageTestUtils.getPage(userIds, 1));
    when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(userMetadata, 1));
    doReturn(Collections.emptyList()).when(ngUserService).listUserIds(scope);

    ngUserService.listUsers(
        scope, pageRequest, UserFilter.builder().parentFilter(UserFilter.ParentFilter.STRICTLY_PARENT_SCOPES).build());

    verify(userMembershipRepository, times(1)).findAllUserIds(userMembershipCriteriaArgumentCaptor.capture(), any());
    verify(userMetadataRepository, times(1)).findAll(userMetadataCriteriaArgumentCaptor.capture(), any());

    Criteria userMembershipCriteria = userMembershipCriteriaArgumentCaptor.getValue();
    assertNotNull(userMembershipCriteria);
    assertEquals(2, userMembershipCriteria.getCriteriaObject().size());

    BasicDBList orList = (BasicDBList) userMembershipCriteria.getCriteriaObject().get("$or");
    assertEquals(2, orList.size());
    assertEquals(accountIdentifier, (String) ((Document) orList.get(0)).get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY));
    assertNull(((Document) orList.get(0)).get(UserMembershipKeys.ORG_IDENTIFIER_KEY));
    assertNull(((Document) orList.get(0)).get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY));
    assertEquals(accountIdentifier, (String) ((Document) orList.get(1)).get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY));
    assertEquals(orgIdentifier, (String) ((Document) orList.get(1)).get(UserMembershipKeys.ORG_IDENTIFIER_KEY));
    assertNull(((Document) orList.get(1)).get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY));

    Document userIdMembershipDocument =
        (Document) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.userId);
    assertNotNull(userIdMembershipDocument);
    assertEquals(1, userIdMembershipDocument.size());
    List<?> negativeList = (List<?>) userIdMembershipDocument.get("$nin");
    assertEquals(0, negativeList.size());

    assertUserMetadataCriteria(userMetadataCriteriaArgumentCaptor, userId);
  }

  private void assertUserMetadataCriteria(ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor, String userId) {
    Criteria userMetadataCriteria = userMetadataCriteriaArgumentCaptor.getValue();
    assertNotNull(userMetadataCriteria);
    Document userMetadataCriteriaUserId =
        (Document) userMetadataCriteria.getCriteriaObject().get(UserMetadataKeys.userId);
    assertEquals(1, userMetadataCriteriaUserId.size());
    List<?> userMetadataCriteriaUserIds = (List<?>) userMetadataCriteriaUserId.get("$in");
    assertEquals(1, userMetadataCriteriaUserIds.size());
    assertEquals(userId, userMetadataCriteriaUserIds.get(0));
  }

  private AddUsersDTO setupAddUsersDTO(List<String> emails) {
    String userGroupIdentifier = randomAlphabetic(10);
    String roleIdentifier = randomAlphabetic(5) + '_' + randomAlphabetic(5) + '_' + randomAlphabetic(5);
    String resourceGroupIdentifier = randomAlphabetic(10);
    RoleBinding roleBinding =
        RoleBinding.builder().roleIdentifier(roleIdentifier).resourceGroupIdentifier(resourceGroupIdentifier).build();

    return AddUsersDTO.builder()
        .emails(emails)
        .userGroups(Collections.singletonList(userGroupIdentifier))
        .roleBindings(Collections.singletonList(roleBinding))
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersAddUserAlreadyPartOfAccount() {
    String orgName = randomAlphabetic(10);
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);

    String emailAlreadyPartOfAccount = randomAlphabetic(10) + '@' + randomAlphabetic(10);
    String userIdAlreadyPartOfAccount = randomAlphabetic(10);

    List<String> emails = Lists.newArrayList(emailAlreadyPartOfAccount);
    AddUsersDTO addUsersDTO = setupAddUsersDTO(emails);

    Criteria criteria = Criteria.where(UserMetadataKeys.email).in(addUsersDTO.getEmails());
    UserMetadata userMetadataAlreadyPartOfAccount =
        UserMetadata.builder().userId(userIdAlreadyPartOfAccount).email(emailAlreadyPartOfAccount).build();
    when(userMetadataRepository.findAll(criteria, Pageable.unpaged()))
        .thenReturn(PageTestUtils.getPage(Lists.newArrayList(userMetadataAlreadyPartOfAccount), 1));

    doReturn(Sets.newHashSet(userIdAlreadyPartOfAccount))
        .when(ngUserService)
        .getUsersAtScope(Sets.newHashSet(userIdAlreadyPartOfAccount), Scope.of(accountIdentifier, null, null));

    preDirectlyAddUsers(scope, orgName, Lists.newArrayList(userIdAlreadyPartOfAccount), addUsersDTO);

    ArgumentCaptor<NotificationChannel> notificationChannelArgumentCaptor =
        ArgumentCaptor.forClass(NotificationChannel.class);
    when(notificationClient.sendNotificationAsync(any())).thenReturn(null);

    AddUsersResponse response = ngUserService.addUsers(scope, addUsersDTO);
    assertEquals(1, response.getAddUserResponseMap().size());
    assertEquals(
        AddUserResponse.USER_ADDED_SUCCESSFULLY, response.getAddUserResponseMap().get(emailAlreadyPartOfAccount));

    verify(userGroupService, times(1)).list(any());
    verify(userMetadataRepository, times(1)).findAll(any(), any());
    verify(ngUserService, times(1)).getUsersAtScope(any(), any());

    assertDirectlyAddUsers(scope, Lists.newArrayList(emailAlreadyPartOfAccount));
    assertDirectlyAddedNotification(
        notificationChannelArgumentCaptor, Lists.newArrayList(emailAlreadyPartOfAccount), "organizationname", orgName);
  }

  private void preDirectlyAddUsers(
      Scope scope, String resourceName, List<String> toBeDirectlyAddedUserIds, AddUsersDTO addUsersDTO) {
    if (isEmpty(toBeDirectlyAddedUserIds)) {
      return;
    }
    when(accountOrgProjectHelper.getResourceScopeName(scope)).thenReturn(resourceName);
    when(accountOrgProjectHelper.getBaseUrl(accountIdentifier)).thenReturn("qa.harness.io");

    UserGroupFilterDTO userGroupFilterDTO = UserGroupFilterDTO.builder()
                                                .accountIdentifier(scope.getAccountIdentifier())
                                                .orgIdentifier(scope.getOrgIdentifier())
                                                .projectIdentifier(scope.getProjectIdentifier())
                                                .identifierFilter(new HashSet<>(addUsersDTO.getUserGroups()))
                                                .build();
    List<UserGroup> userGroups =
        addUsersDTO.getUserGroups()
            .stream()
            .map(userGroupIdentifier -> UserGroup.builder().identifier(userGroupIdentifier).build())
            .collect(toList());
    when(userGroupService.list(userGroupFilterDTO)).thenReturn(userGroups);

    List<Scope> parentScopes = new ArrayList<>();
    if (isNotEmpty(scope.getProjectIdentifier())) {
      parentScopes.add(Scope.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), null));
    }
    if (isNotEmpty(scope.getOrgIdentifier())) {
      parentScopes.add(Scope.of(scope.getAccountIdentifier(), null, null));
    }

    toBeDirectlyAddedUserIds.forEach(userIdAlreadyPartOfAccount -> {
      doReturn(false).when(ngUserService).isUserAtScope(userIdAlreadyPartOfAccount, scope);
      when(userMetadataRepository.findDistinctByUserId(userIdAlreadyPartOfAccount))
          .thenReturn(Optional.of(UserMetadata.builder().userId(userIdAlreadyPartOfAccount).build()));
      doNothing()
          .when(ngUserService)
          .addUserToScopeInternal(
              userIdAlreadyPartOfAccount, UserMembershipUpdateSource.USER, scope, getDefaultRoleIdentifier(scope));

      parentScopes.forEach(parentScope
          -> doNothing()
                 .when(ngUserService)
                 .addUserToScopeInternal(userIdAlreadyPartOfAccount, UserMembershipUpdateSource.USER, parentScope,
                     getDefaultRoleIdentifier(parentScope)));
      doNothing()
          .when(ngUserService)
          .createRoleAssignments(userIdAlreadyPartOfAccount, scope,
              createRoleAssignmentDTOs(addUsersDTO.getRoleBindings(), userIdAlreadyPartOfAccount));
      doNothing()
          .when(userGroupService)
          .addUserToUserGroups(scope, userIdAlreadyPartOfAccount, addUsersDTO.getUserGroups());
    });
  }

  private String getDefaultRoleIdentifier(Scope scope) {
    if (isNotEmpty(scope.getProjectIdentifier())) {
      return PROJECT_VIEWER;
    } else if (isNotEmpty(scope.getOrgIdentifier())) {
      return ORGANIZATION_VIEWER;
    }
    return ACCOUNT_VIEWER;
  }

  private int getRank(Scope scope) {
    if (isNotEmpty(scope.getProjectIdentifier())) {
      return 3;
    } else if (isNotEmpty(scope.getOrgIdentifier())) {
      return 2;
    }
    return 1;
  }

  private void assertDirectlyAddUsers(Scope scope, List<String> toBeDirectlyAddedUserIds) {
    verify(accountOrgProjectHelper, times(1)).getResourceScopeName(any());
    verify(accountOrgProjectHelper, times(1)).getBaseUrl(any());
    verify(ngUserService, times(toBeDirectlyAddedUserIds.size())).isUserAtScope(any(), any());
    verify(userMetadataRepository, times(toBeDirectlyAddedUserIds.size())).findDistinctByUserId(any());
    verify(ngUserService, times(toBeDirectlyAddedUserIds.size() * getRank(scope)))
        .addUserToScopeInternal(any(), any(), any(), any());
    verify(ngUserService, times(toBeDirectlyAddedUserIds.size())).createRoleAssignments(any(), any(), any());
    verify(userGroupService, times(toBeDirectlyAddedUserIds.size())).list(any());
    verify(userGroupService, times(toBeDirectlyAddedUserIds.size()))
        .addUserToUserGroups(any(Scope.class), any(), any());
  }

  private void assertDirectlyAddedNotification(ArgumentCaptor<NotificationChannel> notificationChannelArgumentCaptor,
      List<String> emails, String resourceKey, String resourceName) {
    verify(notificationClient, times(1)).sendNotificationAsync(notificationChannelArgumentCaptor.capture());
    NotificationChannel notificationChannel = notificationChannelArgumentCaptor.getValue();
    assertNotNull(notificationChannel);
    assertTrue(notificationChannel instanceof EmailChannel);
    assertEquals(emails, ((EmailChannel) notificationChannel).getRecipients());
    assertEquals(accountIdentifier, notificationChannel.getAccountId());
    assertEquals("email_notify", notificationChannel.getTemplateId());
    assertEquals(Team.PL, notificationChannel.getTeam());
    assertTrue(notificationChannel.getTemplateData().containsKey(resourceKey));
    assertEquals(resourceName, notificationChannel.getTemplateData().get(resourceKey));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersInviteHarnessUser() {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    String emailAlreadyPartOfHarness = randomAlphabetic(10) + '@' + randomAlphabetic(10);
    String userIdAlreadyPartOfHarness = randomAlphabetic(10);

    List<String> emails = Lists.newArrayList(emailAlreadyPartOfHarness);
    AddUsersDTO addUsersDTO = setupAddUsersDTO(emails);

    Criteria criteria = Criteria.where(UserMetadataKeys.email).in(addUsersDTO.getEmails());
    UserMetadata userMetadataAlreadyPartOfHarness =
        UserMetadata.builder().userId(userIdAlreadyPartOfHarness).email(emailAlreadyPartOfHarness).build();
    when(userMetadataRepository.findAll(criteria, Pageable.unpaged()))
        .thenReturn(PageTestUtils.getPage(Lists.newArrayList(userMetadataAlreadyPartOfHarness), 1));

    doReturn(Sets.newHashSet())
        .when(ngUserService)
        .getUsersAtScope(Sets.newHashSet(userIdAlreadyPartOfHarness), Scope.of(accountIdentifier, null, null));

    assertInviteUser(scope, Collections.singletonList(emailAlreadyPartOfHarness), addUsersDTO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersInviteNewEmail() {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    String newEmail = randomAlphabetic(10) + '@' + randomAlphabetic(10);

    List<String> emails = Lists.newArrayList(newEmail);
    AddUsersDTO addUsersDTO = setupAddUsersDTO(emails);

    Criteria criteria = Criteria.where(UserMetadataKeys.email).in(addUsersDTO.getEmails());
    when(userMetadataRepository.findAll(criteria, Pageable.unpaged()))
        .thenReturn(PageTestUtils.getPage(Lists.newArrayList(), 0));

    doReturn(Sets.newHashSet())
        .when(ngUserService)
        .getUsersAtScope(Sets.newHashSet(), Scope.of(accountIdentifier, null, null));

    assertInviteUser(scope, Collections.singletonList(newEmail), addUsersDTO);
  }

  private void assertInviteUser(Scope scope, List<String> emailsExpectedToBeInvited, AddUsersDTO addUsersDTO) {
    Map<String, AddUserResponse> inviteResponse = new HashMap<>();
    emailsExpectedToBeInvited.forEach(email -> inviteResponse.put(email, AddUserResponse.USER_INVITED_SUCCESSFULLY));
    ArgumentCaptor<List> toBeInvitedEmailsArgumentCaptor = ArgumentCaptor.forClass(List.class);
    doReturn(inviteResponse)
        .when(ngUserService)
        .inviteUsers(eq(scope), eq(addUsersDTO.getRoleBindings()), eq(addUsersDTO.getUserGroups()), any());

    AddUsersResponse response = ngUserService.addUsers(scope, addUsersDTO);

    assertEquals(emailsExpectedToBeInvited.size(), response.getAddUserResponseMap().size());
    emailsExpectedToBeInvited.forEach(
        email -> assertEquals(AddUserResponse.USER_INVITED_SUCCESSFULLY, response.getAddUserResponseMap().get(email)));

    verify(userMetadataRepository, times(1)).findAll(any(), any());
    verify(ngUserService, times(1)).getUsersAtScope(any(), any());

    verify(ngUserService, times(1)).inviteUsers(any(), any(), any(), toBeInvitedEmailsArgumentCaptor.capture());
    List<String> toBeInvitedEmails = toBeInvitedEmailsArgumentCaptor.getValue();
    assertEquals(emailsExpectedToBeInvited.size(), toBeInvitedEmails.size());
    Collections.sort(toBeInvitedEmails);
    Collections.sort(emailsExpectedToBeInvited);
    assertEquals(emailsExpectedToBeInvited, toBeInvitedEmails);
  }
}
