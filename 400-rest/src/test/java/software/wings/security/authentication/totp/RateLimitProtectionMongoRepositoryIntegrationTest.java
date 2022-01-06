/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import static io.harness.rule.OwnerRule.BOGDAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.User;

import com.google.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RateLimitProtectionMongoRepositoryIntegrationTest extends WingsBaseTest {
  @Inject private HPersistence persistence;
  private RateLimitProtectionRepository repo;
  private User user;

  @Before
  public void setUp() {
    repo = new RateLimitProtectionMongoRepository(persistence);
    user = User.Builder.anUser().uuid("user1-uuid").build();
    persistence.save(user);
  }

  @After
  public void tearDown() {
    persistence.delete(user);
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldCreateRateLimitProtectionDataIfNotExists() {
    // preconditions
    assertThat(user.getRateLimitProtection()).isNull();

    // when
    repo.createRateLimitProtectionDataIfNotExists(user.getUuid());
    RateLimitProtection persistedRateLimitProtection =
        persistence.get(User.class, user.getUuid()).getRateLimitProtection();

    // then
    assertThat(persistedRateLimitProtection).isNotNull();
    RateLimitProtection expectedResult = RateLimitProtection.builder().incorrectAttemptTimestamps(null).build();
    assertThat(persistedRateLimitProtection).isEqualTo(expectedResult);
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldRemoveAccessTimesOutsideWindow() {
    // given
    repo.createRateLimitProtectionDataIfNotExists(user.getUuid());
    repo.addIncorrectAttempt(user.getUuid(), 1L);
    repo.addIncorrectAttempt(user.getUuid(), 2L);
    repo.addIncorrectAttempt(user.getUuid(), 3L);

    // when
    RateLimitProtection protection = repo.pruneIncorrectAttemptTimes(user.getUuid(), 2L);

    // then
    assertThat(protection.getIncorrectAttemptTimestamps()).containsOnly(2L, 3L);
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldGetAndUpdateLastEmailSentToUserAt() {
    // given
    repo.createRateLimitProtectionDataIfNotExists(user.getUuid());

    // when
    long lastSentAt = repo.getAndUpdateLastEmailSentToUserAt(user.getUuid(), 1L);
    assertThat(lastSentAt).isEqualTo(0);

    lastSentAt = repo.getAndUpdateLastEmailSentToUserAt(user.getUuid(), 2L);
    assertThat(lastSentAt).isEqualTo(1);
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldGetAndUpdateLastEmailSentToSecOpsAt() {
    //
    repo.createRateLimitProtectionDataIfNotExists(user.getUuid());

    long lastSentAt = repo.getAndUpdateLastEmailSentToSecOpsAt(user.getUuid(), 1L);
    assertThat(lastSentAt).isEqualTo(0);

    lastSentAt = repo.getAndUpdateLastEmailSentToSecOpsAt(user.getUuid(), 2L);
    assertThat(lastSentAt).isEqualTo(1);
  }
}
