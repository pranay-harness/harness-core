/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migration.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigration;
import io.harness.migration.NGMigrationTestBase;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.beans.NGMigrationConfiguration;
import io.harness.migration.entities.NGSchema;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(DX)
public class NGMigrationServiceImplTest extends NGMigrationTestBase {
  @Mock Injector injector;
  @Mock ExecutorService executorService;
  @Mock PersistentLocker persistentLocker;
  @Mock TimeLimiter timeLimiter;
  @Mock private AcquiredLock acquiredLock;
  @Inject MongoTemplate mongoTemplate;
  NGMigrationServiceImpl ngMigrationService;

  @Before
  public void setup() {
    initMocks(this);
    ngMigrationService =
        new NGMigrationServiceImpl(persistentLocker, executorService, timeLimiter, injector, mongoTemplate);
  }

  @Test
  @Owner(developers = OwnerRule.MEENAKSHI)
  @Category(UnitTests.class)
  public void testRunMigrationWhenSchemaIsNull() {
    when(injector.getInstance(TestMigrationProviderClass.class)).thenReturn(new TestMigrationProviderClass());
    when(injector.getInstance(TestMigrationDetailsClass.class)).thenReturn(new TestMigrationDetailsClass());
    when(injector.getInstance(TestNGMigrationClass.class)).thenReturn(new TestNGMigrationClass());
    AcquiredLock acquiredLock = mock(AcquiredLock.class);
    when(persistentLocker.waitToAcquireLock(NGSchema.class, "ngschemaCORE", ofMinutes(25), ofMinutes(27)))
        .thenReturn(acquiredLock);
    NGMigrationConfiguration config = NGMigrationConfiguration.builder()
                                          .microservice(Microservice.CORE)
                                          .migrationProviderList(new ArrayList<Class<? extends MigrationProvider>>() {
                                            { add(TestMigrationProviderClass.class); }
                                          })
                                          .build();
    ngMigrationService.runMigrations(config);
    NGSchema ngSchema = mongoTemplate.findOne(new Query(), NGSchemaTestClass.class);
    assertThat(ngSchema.getMigrationDetails().get(MigrationType.MongoMigration)).isEqualTo(1);
  }

  @Test
  @Owner(developers = OwnerRule.MEENAKSHI)
  @Category(UnitTests.class)
  public void testDoMigration() throws Exception {
    when(injector.getInstance(TestNGMigrationClass.class)).thenReturn(new TestNGMigrationClass());
    when(injector.getInstance(TestMigrationProviderClass.class)).thenReturn(new TestMigrationProviderClass());
    NGSchema ngSchema = NGSchemaTestClass.builder()
                            .name("ngschema")
                            .migrationDetails(new HashMap<MigrationType, Integer>() {
                              { put(MigrationType.MongoMigration, 1); }
                            })
                            .build();

    mongoTemplate.save(ngSchema, "ngschema");
    Map<Integer, Class<? extends NGMigration>> migrations = new HashMap<Integer, Class<? extends NGMigration>>() {
      {
        put(2, TestNGMigrationClass.class);
      }

      ;
    };
    ngMigrationService.doMigration(
        false, 1, 2, migrations, MigrationType.MongoMigration, NGSchemaTestClass.class, "ngschema");
    NGSchema schema = mongoTemplate.findOne(new Query(), NGSchemaTestClass.class);
    assertThat(schema.getMigrationDetails().get(MigrationType.MongoMigration)).isEqualTo(2);
  }

  @Test
  @Owner(developers = OwnerRule.MEENAKSHI)
  @Category(UnitTests.class)
  public void testDoMigrationWhenFieldIsNotPresent() throws Exception {
    when(injector.getInstance(TestNGMigrationClass.class)).thenReturn(new TestNGMigrationClass());
    NGSchema ngSchema = NGSchemaTestClass.builder()
                            .name("ngschema")
                            .migrationDetails(new HashMap<MigrationType, Integer>() {
                              { put(MigrationType.MongoMigration, 1); }
                            })
                            .build();

    mongoTemplate.save(ngSchema, "ngschema");
    Map<Integer, Class<? extends NGMigration>> migrations = new HashMap<Integer, Class<? extends NGMigration>>() {
      {
        put(1, TestNGMigrationClass.class);
      }

      ;
    };
    ngMigrationService.doMigration(
        false, 0, 1, migrations, MigrationType.TimeScaleMigration, NGSchemaTestClass.class, "ngschema");
    NGSchema ngchema2 = mongoTemplate.findOne(new Query(), NGSchemaTestClass.class);

    assertThat(ngchema2.getMigrationDetails().get(MigrationType.TimeScaleMigration)).isEqualTo(1);
  }

  public static class TestMigrationProviderClass implements MigrationProvider {
    @Override
    public String getServiceName() {
      return "ngcore";
    }

    @Override
    public Class<? extends NGSchema> getSchemaClass() {
      return NGSchemaTestClass.class;
    }

    @Override
    public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
      return new ArrayList<Class<? extends MigrationDetails>>() {
        { add(TestMigrationDetailsClass.class); }
      };
    }
  }

  public static class TestMigrationDetailsClass implements MigrationDetails {
    @Override
    public MigrationType getMigrationTypeName() {
      return MigrationType.MongoMigration;
    }

    @Override
    public boolean isBackground() {
      return false;
    }

    @Override
    public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
      return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
          .add(Pair.of(1, TestNGMigrationClass.class))
          .build();
    }
  }

  public static class TestNGMigrationClass implements NGMigration {
    @Override
    public void migrate() {
      // do nothing
    }
  }
}
