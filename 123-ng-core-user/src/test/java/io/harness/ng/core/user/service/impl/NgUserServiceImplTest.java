package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.ng.core.user.entities.UserMetadata.UserMetadataKeys;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.repositories.user.spring.UserMetadataRepository;
import io.harness.rule.Owner;
import io.harness.user.remote.UserClient;
import io.harness.utils.PageTestUtils;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
  @Spy @Inject @InjectMocks private NgUserServiceImpl ngUserService;

  @Before
  public void setup() throws NoSuchFieldException {
    initMocks(this);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void listUsers() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    String accountIdentifier = randomAlphabetic(10);
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
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
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
    assertNull((String) ((Document) orList.get(0)).get(UserMembershipKeys.ORG_IDENTIFIER_KEY));
    assertNull((String) ((Document) orList.get(0)).get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY));
    assertEquals(accountIdentifier, (String) ((Document) orList.get(1)).get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY));
    assertEquals(orgIdentifier, (String) ((Document) orList.get(1)).get(UserMembershipKeys.ORG_IDENTIFIER_KEY));
    assertNull((String) ((Document) orList.get(1)).get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY));

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
}
