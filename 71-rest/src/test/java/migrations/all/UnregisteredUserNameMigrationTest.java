package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import io.harness.category.element.UnitTests;
import io.harness.persistence.ReadPref;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;

import java.util.stream.LongStream;

public class UnregisteredUserNameMigrationTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Inject UnregisteredUserNameMigration unregisteredUserNameMigration;

  @Before
  public void setUp() {}

  private void prepareTest(long count, String name) {
    LongStream.range(0, 10)
        .mapToObj(i -> Builder.anUser().withName(name).withEmail("testEmail" + i + "@test.com").build())
        .forEach(user -> wingsPersistence.save(user));
  }

  @Test
  @Category(UnitTests.class)
  public void testMigrationOfAllUnregisteredUsers() {
    long initialCount = 10;
    prepareTest(initialCount, Constants.NOT_REGISTERED);
    Assertions
        .assertThat(wingsPersistence.getCollection(User.class, ReadPref.NORMAL)
                        .count(new BasicDBObject("name", Constants.NOT_REGISTERED)))
        .isEqualTo(initialCount);
    unregisteredUserNameMigration.migrate();
    Assertions
        .assertThat(wingsPersistence.getCollection(User.class, ReadPref.NORMAL)
                        .count(new BasicDBObject("name", Constants.NOT_REGISTERED)))
        .isEqualTo(0);
  }

  @Test
  @Category(UnitTests.class)
  public void testNoMigrationOfRegisteredUsers() {
    long initialCount = 10;
    prepareTest(initialCount, "TestUser");
    Assertions
        .assertThat(
            wingsPersistence.getCollection(User.class, ReadPref.NORMAL).count(new BasicDBObject("name", "TestUser")))
        .isEqualTo(10);
    unregisteredUserNameMigration.migrate();
    Assertions
        .assertThat(
            wingsPersistence.getCollection(User.class, ReadPref.NORMAL).count(new BasicDBObject("name", "TestUser")))
        .isEqualTo(10);
  }

  @Test
  @Category(UnitTests.class)
  public void testMigrationOfSomeRegisteredUsers() {
    long initialCount = 10;
    prepareTest(initialCount, "TestUser");
    prepareTest(initialCount, Constants.NOT_REGISTERED);
    Assertions
        .assertThat(
            wingsPersistence.getCollection(User.class, ReadPref.NORMAL).count(new BasicDBObject("name", "TestUser")))
        .isEqualTo(10);
    Assertions
        .assertThat(wingsPersistence.getCollection(User.class, ReadPref.NORMAL)
                        .count(new BasicDBObject("name", Constants.NOT_REGISTERED)))
        .isEqualTo(initialCount);
    unregisteredUserNameMigration.migrate();
    Assertions
        .assertThat(
            wingsPersistence.getCollection(User.class, ReadPref.NORMAL).count(new BasicDBObject("name", "TestUser")))
        .isEqualTo(10);
    Assertions
        .assertThat(wingsPersistence.getCollection(User.class, ReadPref.NORMAL)
                        .count(new BasicDBObject("name", Constants.NOT_REGISTERED)))
        .isEqualTo(0);
  }
}
