package io.harness.functional.ipwhitelisting;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;

@Slf4j
public class IPWhitelistingCRUDTest extends AbstractFunctionalTest {
  @Test()
  @Owner(developers = NATARAJA)
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
    assertThat(ipToWhiteList.getAccountId()).isEqualTo(ipAdded.getAccountId());
    assertThat(ipToWhiteList.getFilter()).isEqualTo(ipAdded.getFilter());
    assertThat(ipToWhiteList.getDescription()).isEqualTo(ipAdded.getDescription());
    assertThat(ipToWhiteList.getStatus()).isEqualTo(ipAdded.getStatus());
    String uuid = ipAdded.getUuid();
    logger.info("Verifying if getting the whitelisted IP works");
    ipAdded = IPWhitelistingRestUtils.getWhitelistedIP(getAccount().getUuid(), bearerToken, ipAdded.getUuid());
    assertThat(ipToWhiteList.getAccountId()).isEqualTo(ipAdded.getAccountId());
    assertThat(ipToWhiteList.getFilter()).isEqualTo(ipAdded.getFilter());
    assertThat(ipToWhiteList.getDescription()).isEqualTo(ipAdded.getDescription());
    assertThat(ipToWhiteList.getStatus()).isEqualTo(ipAdded.getStatus());
    assertThat(uuid).isEqualTo(ipAdded.getUuid());
    logger.info("Updating and verifying the whitelisted IP");
    ipToWhiteList.setUuid(ipAdded.getUuid());
    ipToWhiteList.setFilter("127.0.0.1");
    ipToWhiteList.setDescription("Modified description");
    Whitelist updatedWhitelist =
        IPWhitelistingRestUtils.updateWhiteListing(getAccount().getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipToWhiteList.getAccountId()).isEqualTo(updatedWhitelist.getAccountId());
    assertThat(ipToWhiteList.getFilter()).isEqualTo(updatedWhitelist.getFilter());
    assertThat(ipToWhiteList.getDescription()).isEqualTo(updatedWhitelist.getDescription());
    assertThat(ipToWhiteList.getStatus()).isEqualTo(updatedWhitelist.getStatus());
    assertThat(ipToWhiteList.getUuid()).isEqualTo(updatedWhitelist.getUuid());
    logger.info("IPWhitelisting test completed");
    logger.info("Deleting the test created through entries");
    assertThat(
        IPWhitelistingRestUtils.deleteWhitelistedIP(getAccount().getUuid(), bearerToken, updatedWhitelist.getUuid()))
        .isTrue();
    logger.info("Deletion successful");
  }
}
