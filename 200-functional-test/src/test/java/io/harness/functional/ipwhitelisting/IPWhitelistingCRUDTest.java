/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.ipwhitelisting;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;

import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class IPWhitelistingCRUDTest extends AbstractFunctionalTest {
  @Test()
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void testIPWhitelistingCRUD() {
    final String IP_WHITELIST_VAL = "0.0.0.0";
    log.info("Starting the IPWhitelisting CRUD test");
    log.info("Check if the IP to be whitelisted already exists");

    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(getAccount().getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    log.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(getAccount().getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipAdded).isNotNull();
    log.info("Verifying the added values");
    assertThat(ipToWhiteList.getAccountId()).isEqualTo(ipAdded.getAccountId());
    assertThat(ipToWhiteList.getFilter()).isEqualTo(ipAdded.getFilter());
    assertThat(ipToWhiteList.getDescription()).isEqualTo(ipAdded.getDescription());
    assertThat(ipToWhiteList.getStatus()).isEqualTo(ipAdded.getStatus());
    String uuid = ipAdded.getUuid();
    log.info("Verifying if getting the whitelisted IP works");
    ipAdded = IPWhitelistingRestUtils.getWhitelistedIP(getAccount().getUuid(), bearerToken, ipAdded.getUuid());
    assertThat(ipToWhiteList.getAccountId()).isEqualTo(ipAdded.getAccountId());
    assertThat(ipToWhiteList.getFilter()).isEqualTo(ipAdded.getFilter());
    assertThat(ipToWhiteList.getDescription()).isEqualTo(ipAdded.getDescription());
    assertThat(ipToWhiteList.getStatus()).isEqualTo(ipAdded.getStatus());
    assertThat(uuid).isEqualTo(ipAdded.getUuid());
    log.info("Updating and verifying the whitelisted IP");
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
    log.info("IPWhitelisting test completed");
    log.info("Deleting the test created through entries");
    assertThat(
        IPWhitelistingRestUtils.deleteWhitelistedIP(getAccount().getUuid(), bearerToken, updatedWhitelist.getUuid()))
        .isTrue();
    log.info("Deletion successful");
  }
}
