package io.harness.ng.authenticationsettings;

import static io.harness.rule.OwnerRule.MEENAKSHI;
import static io.harness.utils.PageUtils.getPageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@OwnedBy(HarnessTeam.PL)
public class NGSamlUserGroupSyncTest extends AuthenticationSettingTestBase {
  @Mock private UserGroupService userGroupService;
  @Mock private NgUserService ngUserService;
  @InjectMocks NGSamlUserGroupSync ngSamlUserGroupSync;

  @Before
  public void setup() {
    initMocks(this);
    //    userGroupService = mock(UserGroupService.class);
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void test_getUserGroupsAtScope_WithNoUsergroups() {
    String accountId = "accountId1";
    String orgId = "orgId1";
    String projectId = "projectId1";
    Page<UserGroup> pagedUserGroups = Page.empty();
    when(userGroupService.list(accountId, orgId, projectId, null,
             getPageRequest(PageRequest.builder().pageIndex(0).pageSize(40).build())))
        .thenReturn(pagedUserGroups);
    final List<UserGroup> userGroupsAtScope = ngSamlUserGroupSync.getUserGroupsAtScope(accountId, orgId, projectId);
    assertThat(userGroupsAtScope.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void test_getUserGroupsAtScope() {
    String accountId = "accountId1";
    String orgId = "orgId1";
    String projectId = "projectId1";
    Page<UserGroup> mockedUsergroupResponse = createMockedUsergroupResponse(accountId, orgId, projectId);
    when(userGroupService.list(any(), any(), any(), any(), any())).thenReturn(mockedUsergroupResponse);

    List<UserGroup> userGroupsAtScope = ngSamlUserGroupSync.getUserGroupsAtScope(accountId, orgId, projectId);
    assertThat(userGroupsAtScope.size()).isEqualTo(6);
  }

  private Page<UserGroup> createMockedUsergroupResponse(String accountId, String orgId, String projectId) {
    UserGroup userGroup1 =
        UserGroup.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    UserGroup userGroup2 =
        UserGroup.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    UserGroup userGroup3 =
        UserGroup.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    UserGroup userGroup4 =
        UserGroup.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    UserGroup userGroup5 =
        UserGroup.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    UserGroup userGroup6 =
        UserGroup.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    List<UserGroup> pagedUserGroup =
        Arrays.asList(userGroup1, userGroup2, userGroup3, userGroup4, userGroup5, userGroup6);
    return new PageImpl<>(pagedUserGroup);
  }
}
