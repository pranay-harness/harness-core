package software.wings.integration.common;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.StressTests;
import io.harness.rule.Owner;
import io.harness.threading.Concurrent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.annotations.Entity;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

@Integration
@Slf4j
public class MongoDBTest extends WingsBaseTest {
  @Entity(value = "!!!testMongo", noClassnameStored = true)
  public static class MongoEntity extends Base {
    @Getter @Setter private String data;
  }

  @Inject WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  // this test was used to validate mongo setting for dirty reads
  // there is no need it to be run again and again
  public void dirtyReads() {
    Concurrent.test(10, t -> {
      try {
        final MongoEntity entity = new MongoEntity();
        String data = "";
        for (int i = 0; i < 1000; i++) {
          logger.info("" + i);
          data = data + i;
          entity.setData(data);
          wingsPersistence.save(entity);
          final MongoEntity mongoEntity = wingsPersistence.get(MongoEntity.class, entity.getUuid());
          assertThat(mongoEntity.getData()).isEqualTo(data);
        }
      } catch (RuntimeException e) {
        logger.error("", e);
      }
    });
  }
}
