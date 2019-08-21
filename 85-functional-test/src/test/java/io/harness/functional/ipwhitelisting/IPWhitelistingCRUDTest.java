package io.harness.functional.ipwhitelisting;

import static io.harness.rule.OwnerRule.SWAMY;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;

@Slf4j
public class IPWhitelistingCRUDTest extends AbstractFunctionalTest {
  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void testIPWhitelistingCRUD() {
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting the IPWhitelisting CRUD test");
    logger.info("Check if the IP to be whitelisted already exists");

    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(getAccount().getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    logger.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(getAccount().getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipAdded).isNotNull();
    logger.info("Verifying the added values");
    assertEquals(ipAdded.getAccountId(), ipToWhiteList.getAccountId());
    assertEquals(ipAdded.getFilter(), ipToWhiteList.getFilter());
    assertEquals(ipAdded.getDescription(), ipToWhiteList.getDescription());
    assertEquals(ipAdded.getStatus(), ipToWhiteList.getStatus());
    String uuid = ipAdded.getUuid();
    logger.info("Verifying if getting the whitelisted IP works");
    ipAdded = IPWhitelistingRestUtils.getWhitelistedIP(getAccount().getUuid(), bearerToken, ipAdded.getUuid());
    assertEquals(ipAdded.getAccountId(), ipToWhiteList.getAccountId());
    assertEquals(ipAdded.getFilter(), ipToWhiteList.getFilter());
    assertEquals(ipAdded.getDescription(), ipToWhiteList.getDescription());
    assertEquals(ipAdded.getStatus(), ipToWhiteList.getStatus());
    assertEquals(ipAdded.getUuid(), uuid);
    logger.info("Updating and verifying the whitelisted IP");
    ipToWhiteList.setUuid(ipAdded.getUuid());
    ipToWhiteList.setFilter("127.0.0.1");
    ipToWhiteList.setDescription("Modified description");
    Whitelist updatedWhitelist =
        IPWhitelistingRestUtils.updateWhiteListing(getAccount().getUuid(), bearerToken, ipToWhiteList);
    assertEquals(updatedWhitelist.getAccountId(), ipToWhiteList.getAccountId());
    assertEquals(updatedWhitelist.getFilter(), ipToWhiteList.getFilter());
    assertEquals(updatedWhitelist.getDescription(), ipToWhiteList.getDescription());
    assertEquals(updatedWhitelist.getStatus(), ipToWhiteList.getStatus());
    assertEquals(updatedWhitelist.getUuid(), ipToWhiteList.getUuid());
    logger.info("IPWhitelisting test completed");
    logger.info("Deleting the test created through entries");
    assertThat(
        IPWhitelistingRestUtils.deleteWhitelistedIP(getAccount().getUuid(), bearerToken, updatedWhitelist.getUuid()))
        .isTrue();
    logger.info("Deletion successful");
  }
}
