package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *
 * @author rktummala
 */
public class HarnessUserGroupServiceTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private Account account;

  @InjectMocks @Inject private HarnessUserGroupService harnessUserGroupService;

  private String accountId1 = UUIDGenerator.generateUuid();
  private String accountId2 = UUIDGenerator.generateUuid();
  private String harnessUserGroupId1 = UUIDGenerator.generateUuid();
  private String harnessUserGroupId2 = UUIDGenerator.generateUuid();
  private String memberId1 = "memberId1";
  private String memberId2 = "memberId2";
  private String harnessUserGroupName1 = "harnessUserGroupName1";
  private String harnessUserGroupName2 = "harnessUserGroupName2";
  private String description1 = "harnessUserGroup 1";
  private String description2 = "harnessUserGroup 2";

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(accountService.get(anyString())).thenReturn(account);
  }

  /**
   * Should save and read.
   *
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testSaveAndRead() {
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .uuid(harnessUserGroupId1)
                                            .description(description1)
                                            .name(harnessUserGroupName1)
                                            .actions(Sets.newHashSet(Action.READ))
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(memberId1))
                                            .build();
    HarnessUserGroup savedHarnessUserGroup = harnessUserGroupService.save(harnessUserGroup);
    compare(harnessUserGroup, savedHarnessUserGroup);

    HarnessUserGroup harnessUserGroupFromGet = harnessUserGroupService.get(harnessUserGroupId1);
    compare(savedHarnessUserGroup, harnessUserGroupFromGet);

    harnessUserGroupFromGet = harnessUserGroupService.get(harnessUserGroupId1);
    compare(savedHarnessUserGroup, harnessUserGroupFromGet);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testList() {
    HarnessUserGroup harnessUserGroup1 = HarnessUserGroup.builder()
                                             .uuid(harnessUserGroupId1)
                                             .description(description1)
                                             .name(harnessUserGroupName1)
                                             .actions(Sets.newHashSet(Action.READ))
                                             .applyToAllAccounts(true)
                                             .memberIds(Sets.newHashSet(memberId1))
                                             .build();
    HarnessUserGroup savedHarnessUserGroup1 = harnessUserGroupService.save(harnessUserGroup1);

    HarnessUserGroup harnessUserGroup2 = HarnessUserGroup.builder()
                                             .accountIds(Sets.newHashSet(accountId1))
                                             .uuid(harnessUserGroupId2)
                                             .description(description2)
                                             .name(harnessUserGroupName2)
                                             .actions(Sets.newHashSet(Action.UPDATE))
                                             .applyToAllAccounts(false)
                                             .memberIds(Sets.newHashSet(memberId1))
                                             .build();
    HarnessUserGroup savedHarnessUserGroup2 = harnessUserGroupService.save(harnessUserGroup2);

    PageResponse pageResponse = harnessUserGroupService.list(aPageRequest().build());
    assertThat(pageResponse).isNotNull();
    List<HarnessUserGroup> harnessUserGroupList = pageResponse.getResponse();
    assertThat(harnessUserGroupList).isNotNull();
    assertThat(harnessUserGroupList).hasSize(2);
    assertThat(harnessUserGroupList).containsExactlyInAnyOrder(savedHarnessUserGroup1, savedHarnessUserGroup2);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testUpdateMembers() {
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .uuid(harnessUserGroupId1)
                                            .description(description1)
                                            .name(harnessUserGroupName1)
                                            .actions(Sets.newHashSet(Action.READ))
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(memberId1))
                                            .build();
    HarnessUserGroup savedHarnessUserGroup = harnessUserGroupService.save(harnessUserGroup);
    compare(harnessUserGroup, savedHarnessUserGroup);

    HarnessUserGroup harnessUserGroupFromGet = harnessUserGroupService.get(harnessUserGroupId1);
    compare(savedHarnessUserGroup, harnessUserGroupFromGet);
    Set<String> memberIds = Sets.newHashSet(memberId1, memberId2);
    harnessUserGroupFromGet.setMemberIds(memberIds);

    HarnessUserGroup updatedHarnessUserGroup = harnessUserGroupService.updateMembers(harnessUserGroupId1, memberIds);
    harnessUserGroupFromGet = harnessUserGroupService.get(harnessUserGroupId1);
    compare(harnessUserGroupFromGet, updatedHarnessUserGroup);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testUpdateAccounts() {
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .uuid(harnessUserGroupId1)
                                            .description(description1)
                                            .name(harnessUserGroupName1)
                                            .actions(Sets.newHashSet(Action.READ))
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(memberId1))
                                            .build();
    HarnessUserGroup savedHarnessUserGroup = harnessUserGroupService.save(harnessUserGroup);
    compare(harnessUserGroup, savedHarnessUserGroup);

    HarnessUserGroup harnessUserGroupFromGet = harnessUserGroupService.get(harnessUserGroupId1);
    compare(savedHarnessUserGroup, harnessUserGroupFromGet);
    Set<String> accountIds = Sets.newHashSet(accountId1, accountId2);
    harnessUserGroupFromGet.setAccountIds(accountIds);
    harnessUserGroupFromGet.setApplyToAllAccounts(false);

    HarnessUserGroup updatedHarnessUserGroup = harnessUserGroupService.updateAccounts(harnessUserGroupId1, accountIds);
    harnessUserGroupFromGet = harnessUserGroupService.get(harnessUserGroupId1);
    compare(harnessUserGroupFromGet, updatedHarnessUserGroup);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testAllowedActions() {
    HarnessUserGroup harnessUserGroup1 = HarnessUserGroup.builder()
                                             .uuid(harnessUserGroupId1)
                                             .description(description1)
                                             .name(harnessUserGroupName1)
                                             .actions(Sets.newHashSet(Action.READ))
                                             .applyToAllAccounts(true)
                                             .memberIds(Sets.newHashSet(memberId1))
                                             .build();
    harnessUserGroupService.save(harnessUserGroup1);

    HarnessUserGroup harnessUserGroup2 = HarnessUserGroup.builder()
                                             .accountIds(Sets.newHashSet(accountId1))
                                             .uuid(harnessUserGroupId2)
                                             .description(description2)
                                             .name(harnessUserGroupName2)
                                             .actions(Sets.newHashSet(Action.UPDATE))
                                             .applyToAllAccounts(false)
                                             .memberIds(Sets.newHashSet(memberId2))
                                             .build();
    harnessUserGroupService.save(harnessUserGroup2);

    Set<Action> actionSet = harnessUserGroupService.listAllowedUserActionsForAccount(accountId1, memberId1);
    assertThat(actionSet).isNotNull();
    assertThat(actionSet).size().isEqualTo(1);
    assertThat(actionSet).containsExactly(Action.READ);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testSupportAccountsForUser() {
    HarnessUserGroup harnessUserGroup1 = HarnessUserGroup.builder()
                                             .uuid(harnessUserGroupId1)
                                             .description(description1)
                                             .name(harnessUserGroupName1)
                                             .actions(Sets.newHashSet(Action.READ))
                                             .applyToAllAccounts(true)
                                             .memberIds(Sets.newHashSet(memberId1))
                                             .build();
    harnessUserGroupService.save(harnessUserGroup1);

    HarnessUserGroup harnessUserGroup2 = HarnessUserGroup.builder()
                                             .accountIds(Sets.newHashSet(accountId1))
                                             .uuid(harnessUserGroupId2)
                                             .description(description2)
                                             .name(harnessUserGroupName2)
                                             .actions(Sets.newHashSet(Action.UPDATE))
                                             .applyToAllAccounts(false)
                                             .memberIds(Sets.newHashSet(memberId2))
                                             .build();
    harnessUserGroupService.save(harnessUserGroup2);
    Account account1 = Builder.anAccount().withUuid(accountId1).build();
    Account account2 = Builder.anAccount().withUuid(accountId2).build();
    List<Account> accounts = Arrays.asList(account1, account2);

    // Scenario 1
    when(accountService.listAccounts(anySet())).thenReturn(accounts);
    List<Account> result = harnessUserGroupService.listAllowedSupportAccountsForUser(memberId1, Sets.newHashSet());
    assertThat(result).isNotNull();
    assertThat(result).size().isEqualTo(2);
    assertThat(result).containsExactlyInAnyOrder(accounts.toArray(new Account[0]));

    // Scenario 2
    when(accountService.listAccounts(anySet())).thenReturn(Lists.newArrayList(account2));
    when(accountService.list(any())).thenReturn(Lists.newArrayList(account2));
    result = harnessUserGroupService.listAllowedSupportAccountsForUser(memberId2, Sets.newHashSet());
    assertThat(result).isNotNull();
    assertThat(result).size().isEqualTo(1);
    assertThat(result).containsExactlyInAnyOrder(account2);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testDelete() {
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .uuid(harnessUserGroupId1)
                                            .description(description1)
                                            .name(harnessUserGroupName1)
                                            .actions(Sets.newHashSet(Action.READ))
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(memberId1))
                                            .build();
    harnessUserGroupService.save(harnessUserGroup);

    boolean delete = harnessUserGroupService.delete(harnessUserGroupId1);
    assertThat(delete).isTrue();

    HarnessUserGroup harnessUserGroupAfterDelete = harnessUserGroupService.get(harnessUserGroupId1);
    assertThat(harnessUserGroupAfterDelete).isNull();
  }

  private void compare(HarnessUserGroup lhs, HarnessUserGroup rhs) {
    assertThat(rhs.getUuid()).isEqualTo(lhs.getUuid());
    assertThat(rhs.getName()).isEqualTo(lhs.getName());
    assertThat(rhs.getDescription()).isEqualTo(lhs.getDescription());
    assertThat(rhs.isApplyToAllAccounts()).isEqualTo(lhs.isApplyToAllAccounts());
    assertThat(rhs.getAccountIds()).isEqualTo(lhs.getAccountIds());
    assertThat(rhs.getMemberIds()).isEqualTo(lhs.getMemberIds());
    assertThat(rhs.getActions()).isEqualTo(lhs.getActions());
  }
}
