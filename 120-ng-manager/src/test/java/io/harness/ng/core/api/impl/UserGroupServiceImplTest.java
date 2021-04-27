package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.core.spring.UserGroupRepository;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(PL)
public class UserGroupServiceImplTest extends CategoryTest {
  @Mock private UserGroupRepository userGroupRepository;
  @Mock private UserClient userClient;
  @Mock private OutboxService outboxService;
  @Mock private AccessControlAdminClient accessControlAdminClient;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private NgUserService ngUserService;
  @Inject @InjectMocks private UserGroupServiceImpl userGroupService;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testCreateValidate() throws IOException {
    List<String> users = Arrays.asList("u1", "u2", "u3");
    List<UserInfo> userInfos = new ArrayList<>();
    userInfos.add(UserInfo.builder().uuid("u1").build());
    userInfos.add(UserInfo.builder().uuid("u2").build());

    Call<RestResponse<List<UserInfo>>> userClientResponseMock = Mockito.mock(Call.class);
    doReturn(userClientResponseMock).when(userClient).listUsers(any(), any());
    when(userClientResponseMock.execute()).thenReturn(Response.success(new RestResponse<>(userInfos)));

    UserGroupDTO userGroupDTO =
        UserGroupDTO.builder().users(users).accountIdentifier("A1").orgIdentifier("O1").projectIdentifier("P1").build();
    // Users with u3 missing
    assertThatThrownBy(() -> userGroupService.create(userGroupDTO))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("The following user is not valid: [u3]");
    userInfos.add(UserInfo.builder().uuid("u3").build());

    // Users with all valid users with failing membership
    assertThatThrownBy(() -> userGroupService.create(userGroupDTO))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("The following users are not valid: [u1, u2, u3]");

    doReturn(Sets.newSet("u1", "u2"))
        .when(ngUserService)
        .filterUsersWithScopeMembership(users, userGroupDTO.getAccountIdentifier(), userGroupDTO.getOrgIdentifier(),
            userGroupDTO.getProjectIdentifier());

    // Users with all valid users with few memberships
    assertThatThrownBy(() -> userGroupService.create(userGroupDTO))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("The following user is not valid: [u3]");

    doReturn(Sets.newSet("u1", "u2", "u3"))
        .when(ngUserService)
        .filterUsersWithScopeMembership(users, userGroupDTO.getAccountIdentifier(), userGroupDTO.getOrgIdentifier(),
            userGroupDTO.getProjectIdentifier());

    // Users with all valid users with all memberships
    userGroupService.create(userGroupDTO);

    UserGroup userGroup = UserGroup.builder().users(users).build();
    doReturn(userGroup).when(transactionTemplate).execute(any());
    assertThat(userGroupService.create(userGroupDTO)).isEqualTo(userGroup);
  }
}
