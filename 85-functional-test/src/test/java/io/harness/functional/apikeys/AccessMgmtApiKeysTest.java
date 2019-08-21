package io.harness.functional.apikeys;

import static io.harness.rule.OwnerRule.SWAMY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonObject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.security.UserGroup;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AccessMgmtApiKeysTest extends AbstractFunctionalTest {
  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void apiKeysCRUD() {
    logger.info("Creating a userGroup");
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String userGroupname = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", userGroupname);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertNotNull(userGroup);
    logger.info("Constructing APIKeys");
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(getAccount().getUuid());
    apiKeyEntry.setName(name);
    logger.info("Creating APIKeys");
    ApiKeyEntry postCreationEntry = ApiKeysRestUtils.createApiKey(apiKeyEntry.getAccountId(), bearerToken, apiKeyEntry);
    logger.info("Validating created APIKeys");
    assertNotNull(postCreationEntry);
    assertNotNull(postCreationEntry.getUuid());
    assertThat(postCreationEntry.getName().equals(name)).isTrue();
    assertNotNull(postCreationEntry.getUserGroupIds());
    assertThat(postCreationEntry.getUserGroupIds().size() == 1).isTrue();
    assertThat(postCreationEntry.getUserGroupIds().get(0).equals(userGroup.getUuid())).isTrue();
    logger.info("Updating APIKeys");
    String changedName = "APIKey_Changed - " + System.currentTimeMillis();
    postCreationEntry.setName(changedName);
    postCreationEntry.setUserGroups(null);
    postCreationEntry = ApiKeysRestUtils.updateApiKey(apiKeyEntry.getAccountId(), bearerToken, postCreationEntry);
    logger.info("Validating updated APIKeys");
    assertNotNull(postCreationEntry);
    assertNotNull(postCreationEntry.getUuid());
    assertThat(postCreationEntry.getName().equals(changedName)).isTrue();
    assertNotNull(postCreationEntry.getUserGroupIds());
    assertThat(postCreationEntry.getUserGroupIds().size() == 1).isTrue();
    assertThat(postCreationEntry.getUserGroupIds().get(0).equals(userGroup.getUuid())).isTrue();
    ApiKeyEntry retrievedAPIKey =
        ApiKeysRestUtils.getApiKey(getAccount().getUuid(), bearerToken, postCreationEntry.getUuid());
    assertNotNull(retrievedAPIKey);
    assertNotNull(retrievedAPIKey.getUuid());
    assertThat(postCreationEntry.getUuid().equals(retrievedAPIKey.getUuid())).isTrue();
    assertThat(ApiKeysRestUtils.deleteApiKey(getAccount().getUuid(), bearerToken, retrievedAPIKey.getUuid())
        == HttpStatus.SC_OK)
        .isTrue();
    logger.info("APIKey CRUD test completed");
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid())).isTrue();
    logger.info("Deleted Usergroup");
  }
}
