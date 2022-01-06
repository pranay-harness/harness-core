/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.utils.PageUtils.getPageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupAggregateDTO;
import io.harness.ng.core.entities.NotificationSettingConfig;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(PL)
public class AggregateUserGroupServiceImplTest extends CategoryTest {
  @Mock private UserGroupService userGroupService;
  @Mock private AccessControlAdminClient accessControlAdminClient;
  @Mock private NgUserService ngUserService;
  @Inject @InjectMocks private AggregateUserGroupServiceImpl aggregateUserGroupService;

  private static final String ACCOUNT_IDENTIFIER = "ACCOUNT_IDENTIFIER";
  private static final String ORG_IDENTIFIER = "ORG_IDENTIFIER";
  private static final String PROJECT_IDENTIFIER = "PROJECT_IDENTIFIER";

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testListAggregateUserGroups() throws IOException {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(2).build();
    List<NotificationSettingConfig> notificationConfigs = new ArrayList<>();
    List<String> users1 = Lists.newArrayList("u1", "u2", "u3", "u4", "u5", "u6", "u7");
    List<String> users2 = Lists.newArrayList("u3", "u4", "u5", "u6", "u7", "u8");
    List<String> users3 = Lists.newArrayList("u2");
    List<String> users4 = Lists.newArrayList();
    List<UserMetadataDTO> users =
        Lists.newArrayList(getUserMetadata("u1"), getUserMetadata("u2"), getUserMetadata("u3"), getUserMetadata("u4"),
            getUserMetadata("u5"), getUserMetadata("u6"), getUserMetadata("u7"), getUserMetadata("u8"));

    doReturn(new PageImpl<>(Lists.newArrayList(
                 UserGroup.builder().identifier("UG1").users(users1).notificationConfigs(notificationConfigs).build(),
                 UserGroup.builder().identifier("UG2").users(users2).notificationConfigs(notificationConfigs).build(),
                 UserGroup.builder().identifier("UG3").users(users3).notificationConfigs(notificationConfigs).build(),
                 UserGroup.builder().identifier("UG4").users(users4).notificationConfigs(notificationConfigs).build())))
        .when(userGroupService)
        .list(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null, getPageRequest(pageRequest));

    doReturn(users).when(ngUserService).getUserMetadata(anyList());

    Call<RestResponse<SecretManagerConfigDTO>> request = mock(Call.class);
    doReturn(request)
        .when(accessControlAdminClient)
        .getAggregatedFilteredRoleAssignments(
            eq(ACCOUNT_IDENTIFIER), eq(ORG_IDENTIFIER), eq(PROJECT_IDENTIFIER), any());
    doReturn(Response.success(ResponseDTO.newResponse(RoleAssignmentAggregateResponseDTO.builder()
                                                          .roles(new ArrayList<>())
                                                          .resourceGroups(new ArrayList<>())
                                                          .roleAssignments(new ArrayList<>())
                                                          .build())))
        .when(request)
        .execute();

    PageResponse<UserGroupAggregateDTO> response = aggregateUserGroupService.listAggregateUserGroups(
        pageRequest, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null, 2);

    assertThat(response.getContent()).hasSize(4);
    assertThat(response.getContent().get(0).getUsers().size()).isEqualTo(2);
    assertThat(
        response.getContent().get(0).getUsers().stream().map(UserMetadataDTO::getUuid).collect(Collectors.toList()))
        .containsExactly("u7", "u6");
    assertThat(response.getContent().get(1).getUsers().size()).isEqualTo(2);
    assertThat(
        response.getContent().get(1).getUsers().stream().map(UserMetadataDTO::getUuid).collect(Collectors.toList()))
        .containsExactly("u8", "u7");
    assertThat(response.getContent().get(2).getUsers().size()).isEqualTo(1);
    assertThat(
        response.getContent().get(2).getUsers().stream().map(UserMetadataDTO::getUuid).collect(Collectors.toList()))
        .containsExactly("u2");
    assertThat(response.getContent().get(3).getUsers().size()).isEqualTo(0);
  }

  private static UserMetadataDTO getUserMetadata(String user) {
    return UserMetadataDTO.builder().name(user).email(user).uuid(user).build();
  }
}
