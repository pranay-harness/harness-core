/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.common;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.StressTests;
import io.harness.rule.Owner;
import io.harness.threading.Concurrent;

import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Integration
@Slf4j
public class MongoDBTest extends WingsBaseTest {
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
          log.info("" + i);
          data = data + i;
          entity.setData(data);
          wingsPersistence.save(entity);
          final MongoEntity mongoEntity = wingsPersistence.get(MongoEntity.class, entity.getUuid());
          assertThat(mongoEntity.getData()).isEqualTo(data);
        }
      } catch (RuntimeException e) {
        log.error("", e);
      }
    });
  }
}
