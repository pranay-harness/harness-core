/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.TemplateAuthHandler;
import software.wings.service.impl.security.auth.TemplateRBACListFilter;
import software.wings.service.impl.template.TemplateBaseTestHelper;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class TemplateHelperTest extends TemplateBaseTestHelper {
  @Inject TemplateHelper templateHelper;
  @Inject TemplateAuthHandler templateAuthHandler;
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_mappedEntity() {
    final List<TemplateType> supportedTemplateTypes = Arrays.asList(TemplateType.HTTP, TemplateType.SHELL_SCRIPT,
        TemplateType.ARTIFACT_SOURCE, TemplateType.SSH, TemplateType.PCF_PLUGIN, TemplateType.CUSTOM_DEPLOYMENT_TYPE);
    for (TemplateType supportedTemplateType : supportedTemplateTypes) {
      assertThat(TemplateHelper.mappedEntities(supportedTemplateType)).isNotNull();
    }

    Arrays.stream(TemplateType.values())
        .filter(templateType -> !supportedTemplateTypes.contains(templateType))
        .forEach(templateType
            -> assertThatExceptionOfType(InvalidArgumentsException.class)
                   .isThrownBy(() -> TemplateHelper.mappedEntities(templateType)));
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_getFolderDetails() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    TemplateFolder templateFolder1 = templateFolderService.save(TemplateFolder.builder()
                                                                    .name(TEMPLATE_FOLDER_NAME)
                                                                    .description(TEMPLATE_FOLDER_DEC)
                                                                    .parentId(parentFolder.getParentId())
                                                                    .appId("appId2")
                                                                    .accountId(GLOBAL_ACCOUNT_ID)
                                                                    .build(),
        templateGallery.getUuid());
    final List<TemplateFolder> folderDetails = templateHelper.getFolderDetails(GLOBAL_ACCOUNT_ID,
        new HashSet<>(Arrays.asList(templateFolder1.getUuid())), Arrays.asList(templateFolder1.getAppId()));
    assertThat(folderDetails.get(0).getUuid()).isEqualTo(templateFolder1.getUuid());
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
                                .templatePermissions(new HashMap<PermissionAttribute.Action, Set<String>>() {
                                  {
                                    put(PermissionAttribute.Action.CREATE,
                                        new HashSet<>(Arrays.asList("template1", "template2")));
                                    put(PermissionAttribute.Action.UPDATE,
                                        new HashSet<>(Arrays.asList("template1", "template3")));
                                    put(PermissionAttribute.Action.DELETE,
                                        new HashSet<>(Arrays.asList("template2", "template3")));
                                    put(PermissionAttribute.Action.READ, new HashSet<>(Arrays.asList("template5")));
                                  }
                                })
                                .build());
                      }
                    })
                    .build())
            .build());
    UserThreadLocal.set(user);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldAllowTemplateFolderDeletion() {
    setUserRequestContext();
    assertThat(templateHelper.shouldAllowTemplateFolderDeletion("appId1", new HashSet<>(Arrays.asList("template3"))))
        .isEqualTo(true);
    assertThat(templateHelper.shouldAllowTemplateFolderDeletion(
                   "appId1", new HashSet<>(Arrays.asList("template4, template5"))))
        .isEqualTo(false);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_createSearchFilterWithTemplatePermissions() {
    setUserRequestContext();
    final TemplateRBACListFilter filter =
        templateAuthHandler.buildTemplateListRBACFilter(Collections.singletonList("appId1"));

    assertThat(filter.empty()).isEqualTo(false);
    assertThat(filter.getAppIds()).hasSize(1);
    assertThat(filter.getTemplateIds()).hasSize(1);

    final TemplateRBACListFilter filter2 =
        templateAuthHandler.buildTemplateListRBACFilter(Collections.singletonList("appId2"));

    assertThat(filter2.empty()).isEqualTo(true);
    assertThat(filter2.getAppIds()).hasSize(0);
    assertThat(filter2.getTemplateIds()).hasSize(0);
  }
}
