package io.harness.e2e.dailysanity.platform.paid;

import static io.harness.rule.OwnerRule.SWAMY;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonObject;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
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
public class AccessMgmtApiKeysTest extends AbstractE2ETest {
  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(E2ETests.class)
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
    assertTrue(postCreationEntry.getName().equals(name));
    assertNotNull(postCreationEntry.getUserGroupIds());
    assertTrue(postCreationEntry.getUserGroupIds().size() == 1);
    assertTrue(postCreationEntry.getUserGroupIds().get(0).equals(userGroup.getUuid()));
    logger.info("Updating APIKeys");
    String changedName = "APIKey_Changed - " + System.currentTimeMillis();
    postCreationEntry.setName(changedName);
    postCreationEntry.setUserGroups(null);
    postCreationEntry = ApiKeysRestUtils.updateApiKey(apiKeyEntry.getAccountId(), bearerToken, postCreationEntry);
    logger.info("Validating updated APIKeys");
    assertNotNull(postCreationEntry);
    assertNotNull(postCreationEntry.getUuid());
    assertTrue(postCreationEntry.getName().equals(changedName));
    assertNotNull(postCreationEntry.getUserGroupIds());
    assertTrue(postCreationEntry.getUserGroupIds().size() == 1);
    assertTrue(postCreationEntry.getUserGroupIds().get(0).equals(userGroup.getUuid()));
    ApiKeyEntry retrievedAPIKey =
        ApiKeysRestUtils.getApiKey(getAccount().getUuid(), bearerToken, postCreationEntry.getUuid());
    assertNotNull(retrievedAPIKey);
    assertNotNull(retrievedAPIKey.getUuid());
    assertTrue(postCreationEntry.getUuid().equals(retrievedAPIKey.getUuid()));
    assertTrue(ApiKeysRestUtils.deleteApiKey(getAccount().getUuid(), bearerToken, retrievedAPIKey.getUuid())
        == HttpStatus.SC_OK);
    logger.info("APIKey CRUD test completed");
    assertTrue(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid()));
    logger.info("Deleted Usergroup");
  }
}
