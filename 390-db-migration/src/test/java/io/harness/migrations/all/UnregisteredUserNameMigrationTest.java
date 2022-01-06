/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.migrations.all.UnregisteredUserNameMigration.NOT_REGISTERED;
import static io.harness.rule.OwnerRule.RUSHABH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.util.stream.LongStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class UnregisteredUserNameMigrationTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Inject UnregisteredUserNameMigration unregisteredUserNameMigration;

  @Before
  public void setUp() {}

  private void prepareTest(long count, String name) {
    LongStream.range(0, 10)
        .mapToObj(i -> Builder.anUser().name(name).email("testEmail" + i + "@test.com").build())
        .forEach(user -> wingsPersistence.save(user));
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testMigrationOfAllUnregisteredUsers() {
    long initialCount = 10;
    prepareTest(initialCount, NOT_REGISTERED);
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", NOT_REGISTERED)))
        .isEqualTo(initialCount);
    unregisteredUserNameMigration.migrate();
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", NOT_REGISTERED)))
        .isEqualTo(0);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testNoMigrationOfRegisteredUsers() {
    long initialCount = 10;
    prepareTest(initialCount, "TestUser");
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", "TestUser"))).isEqualTo(10);
    unregisteredUserNameMigration.migrate();
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", "TestUser"))).isEqualTo(10);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testMigrationOfSomeRegisteredUsers() {
    long initialCount = 10;
    prepareTest(initialCount, "TestUser");
    prepareTest(initialCount, NOT_REGISTERED);
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", "TestUser"))).isEqualTo(10);
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", NOT_REGISTERED)))
        .isEqualTo(initialCount);
    unregisteredUserNameMigration.migrate();
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", "TestUser"))).isEqualTo(10);
    assertThat(wingsPersistence.getCollection(User.class).count(new BasicDBObject("name", NOT_REGISTERED)))
        .isEqualTo(0);
  }
}
