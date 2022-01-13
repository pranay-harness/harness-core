/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(PL)
@TargetModule(HarnessModule.UNDEFINED)
@Slf4j
public class TemplateAuthHandlerTest extends WingsBaseTest {
  @InjectMocks @Inject private TemplateAuthHandler templateAuthHandler;

  private void assertDoesNotThrow(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception ex) {
      log.error("ERROR: ", ex);
      Assert.fail();
    }
  }

  private void setUserRequestContext() {
    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
    user.setUserRequestContext(
        UserRequestContext.builder()
            .accountId(ACCOUNT_ID)
            .userPermissionInfo(
                UserPermissionInfo.builder()
                    .appPermissionMapInternal(new HashMap<String, AppPermissionSummary>() {
                      {
                        put("appId1",
                            AppPermissionSummary.builder()
                                .canCreateTemplate(true)
                                .templatePermissions(new HashMap<Action, Set<String>>() {
                                  {
                                    put(Action.CREATE, new HashSet<>(Arrays.asList("template1", "template2")));
                                    put(Action.UPDATE, new HashSet<>(Arrays.asList("template1", "template3")));
                                    put(Action.DELETE, new HashSet<>(Arrays.asList("template2", "template3")));
                                    put(Action.READ, new HashSet<>(Arrays.asList("template5")));
                                  }
                                })
                                .build());
                      }
                    })
                    .build())
            .build());
    UserThreadLocal.set(user);
  }

  private void setUpUserWithAccountPermissions() {
    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
    user.setUserRequestContext(
        UserRequestContext.builder()
            .accountId(ACCOUNT_ID)
            .userPermissionInfo(UserPermissionInfo.builder()
                                    .accountPermissionSummary(AccountPermissionSummary.builder()
                                                                  .permissions(new HashSet<>(Arrays.asList(
                                                                      PermissionType.TEMPLATE_MANAGEMENT)))
                                                                  .build())
                                    .build())
            .build());
    UserThreadLocal.set(user);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldAllowTemplateCreationOnAppHavingCreatePermission() {
    setUserRequestContext();
    assertDoesNotThrow(() -> templateAuthHandler.authorizeCreate("appId1"));
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldNotAllowTemplateCreationOnAppHavingNoPermission() {
    setUserRequestContext();
    assertThatThrownBy(() -> templateAuthHandler.authorizeCreate("appId2"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User doesn't have rights to create template in app appId2");
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldNotAllowTemplateCreationOnAppHavingNoTemplateCreatePermission() {
    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
    user.setUserRequestContext(
        UserRequestContext.builder()
            .accountId(ACCOUNT_ID)
            .userPermissionInfo(
                UserPermissionInfo.builder()
                    .appPermissionMapInternal(new HashMap<String, AppPermissionSummary>() {
                      {
                        put("appId1",
                            AppPermissionSummary.builder()
                                .templatePermissions(new HashMap<Action, Set<String>>() {
                                  {
                                    put(Action.UPDATE, new HashSet<>(Arrays.asList("template1", "template3")));
                                    put(Action.DELETE, new HashSet<>(Arrays.asList("template2", "template3")));
                                  }
                                })
                                .build());
                      }
                    })
                    .build())
            .build());
    UserThreadLocal.set(user);
    assertThatThrownBy(() -> templateAuthHandler.authorizeCreate("appId1"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User doesn't have rights to create template in app appId1");
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldBeAbleToReadAccountTemplates() {
    setUpUserWithAccountPermissions();
    assertDoesNotThrow(() -> templateAuthHandler.authorizeRead(GLOBAL_APP_ID, "templateId"));
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldAllowAccountLevelTemplateCreation() {
    setUpUserWithAccountPermissions();
    assertDoesNotThrow(() -> templateAuthHandler.authorizeCreate(GLOBAL_APP_ID));
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldNotAllowAccountLevelTemplateCreation() {
    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
    user.setUserRequestContext(
        UserRequestContext.builder()
            .accountId(ACCOUNT_ID)
            .userPermissionInfo(UserPermissionInfo.builder()
                                    .accountPermissionSummary(AccountPermissionSummary.builder().build())
                                    .build())
            .build());
    UserThreadLocal.set(user);
    assertThatThrownBy(() -> templateAuthHandler.authorizeCreate(GLOBAL_APP_ID))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User doesn't have rights to create template at account level");
  }
}
