/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.security.UserGroup;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class UserGroupServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private UserGroupService userGroupService;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testGetUserGroupSummary() {
    UserGroup userGroup = UserGroup.builder().uuid(generateUuid()).name("name").build();

    UserGroup userGroupSummary = userGroupService.getUserGroupSummary(userGroup);
    assertThat(userGroupSummary).isNotNull();
    assertThat(userGroupSummary.getName()).isEqualTo(userGroup.getName());
    assertThat(userGroupSummary.getUuid()).isEqualTo(userGroup.getUuid());
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testGetUserGroupSummaryNull() {
    UserGroup userGroup = null;
    UserGroup userGroupSummary = userGroupService.getUserGroupSummary(userGroup);
    assertThat(userGroupSummary).isNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testGetListUserGroupSummaryNull() {
    List<UserGroup> userGroupList = new ArrayList<>();
    List<UserGroup> userGroupSummaryList = userGroupService.getUserGroupSummary(userGroupList);
    assertThat(userGroupSummaryList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testGetListUserGroupSummary() {
    UserGroup userGroup = UserGroup.builder().uuid(generateUuid()).name("name").build();
    List<UserGroup> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup);
    List<UserGroup> userGroupSummaryList = userGroupService.getUserGroupSummary(userGroupList);
    assertThat(userGroupSummaryList).isNotNull();
    assertThat(userGroupSummaryList.get(0).getUuid()).isEqualTo(userGroup.getUuid());
    assertThat(userGroupSummaryList.get(0).getName()).isEqualTo(userGroup.getName());
  }
}
