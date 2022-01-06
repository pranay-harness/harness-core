/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.resources.UserGroupResource;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class UserGroupResourceTest extends WingsBaseTest {
  private UserGroupService userGroupService = mock(UserGroupService.class);
  @Inject @InjectMocks private UserGroupResource userGroupResource;

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void shouldSearchUsers() {
    when(userGroupService.list(anyString(), any(), anyBoolean())).thenReturn(aPageResponse().build());
    userGroupResource.list(aPageRequest().build(), UUIDGenerator.generateUuid(), "xyz", false);
    verify(userGroupService).list(anyString(), any(), anyBoolean());
  }
}
