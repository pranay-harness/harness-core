/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.apikeys;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;

import software.wings.beans.ApiKeyEntry;
import software.wings.beans.security.UserGroup;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class AccessMgmtApiKeysTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void apiKeysCRUD() {
    log.info("Creating a userGroup");
    log.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String userGroupname = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", userGroupname);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertThat(userGroup).isNotNull();
    log.info("Constructing APIKeys");
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(getAccount().getUuid());
    apiKeyEntry.setName(name);
    log.info("Creating APIKeys");
    ApiKeyEntry postCreationEntry = ApiKeysRestUtils.createApiKey(apiKeyEntry.getAccountId(), bearerToken, apiKeyEntry);
    log.info("Validating created APIKeys");
    assertThat(postCreationEntry).isNotNull();
    assertThat(postCreationEntry.getUuid()).isNotNull();
    assertThat(postCreationEntry.getName().equals(name)).isTrue();
    assertThat(postCreationEntry.getUserGroupIds()).isNotNull();
    assertThat(postCreationEntry.getUserGroupIds().size() == 1).isTrue();
    assertThat(postCreationEntry.getUserGroupIds().get(0).equals(userGroup.getUuid())).isTrue();
    log.info("Updating APIKeys");
    String changedName = "APIKey_Changed - " + System.currentTimeMillis();
    postCreationEntry.setName(changedName);
    postCreationEntry.setUserGroups(null);
    postCreationEntry = ApiKeysRestUtils.updateApiKey(apiKeyEntry.getAccountId(), bearerToken, postCreationEntry);
    log.info("Validating updated APIKeys");
    assertThat(postCreationEntry).isNotNull();
    assertThat(postCreationEntry.getUuid()).isNotNull();
    assertThat(postCreationEntry.getName().equals(changedName)).isTrue();
    assertThat(postCreationEntry.getUserGroupIds()).isNotNull();
    assertThat(postCreationEntry.getUserGroupIds().size() == 1).isTrue();
    assertThat(postCreationEntry.getUserGroupIds().get(0).equals(userGroup.getUuid())).isTrue();
    ApiKeyEntry retrievedAPIKey =
        ApiKeysRestUtils.getApiKey(getAccount().getUuid(), bearerToken, postCreationEntry.getUuid());
    assertThat(retrievedAPIKey).isNotNull();
    assertThat(retrievedAPIKey.getUuid()).isNotNull();
    assertThat(postCreationEntry.getUuid().equals(retrievedAPIKey.getUuid())).isTrue();
    assertThat(ApiKeysRestUtils.deleteApiKey(getAccount().getUuid(), bearerToken, retrievedAPIKey.getUuid())
        == HttpStatus.SC_OK)
        .isTrue();
    log.info("APIKey CRUD test completed");
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid())).isTrue();
    log.info("Deleted Usergroup");
  }
}
