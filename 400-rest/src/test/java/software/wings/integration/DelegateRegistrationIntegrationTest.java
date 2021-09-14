/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.integration;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.PUNEET;

import static org.awaitility.Awaitility.await;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;

import software.wings.beans.DelegateConnection;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by anubhaw on 6/8/17.
 */
@Slf4j
public class DelegateRegistrationIntegrationTest extends IntegrationTestBase {
  @Test
  @Owner(developers = ANUBHAW)
  @Repeat(times = 5, successes = 1)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void shouldWaitForADelegateToRegister() {
    // TODO: fix this method
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void shouldWaitForADelegateConnectionsToAppear() {
    await().with().pollInterval(Duration.ONE_SECOND).timeout(5, TimeUnit.MINUTES).until(() -> {
      List<DelegateConnection> delegateConnections =
          wingsPersistence.createQuery(DelegateConnection.class, excludeAuthority).asList();
      boolean connected = !delegateConnections.isEmpty();
      log.info("Got {} delegate connections.", delegateConnections.size());
      return connected;
    }, CoreMatchers.is(true));
  }
}
