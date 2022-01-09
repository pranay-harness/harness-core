/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.migrations.all.CleanUpDirectK8sInfraMappingEncryptedFieldsMigration.ENCRYPTED_FIELDS;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CleanUpDirectK8sInfraMappingEncryptedFieldsMigrationTest extends WingsBaseTest {
  private static final int GENERATED_ENTITIES_COUNT = 10;
  @Inject WingsPersistence wingsPersistence;

  @Inject CleanUpDirectK8sInfraMappingEncryptedFieldsMigration migration;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunMigration() {
    prepareEntities();
    BasicDBList encryptedFieldExists = new BasicDBList();
    ENCRYPTED_FIELDS.stream()
        .map(field -> new BasicDBObject(field, new BasicDBObject("$exists", true)))
        .forEach(encryptedFieldExists::add);
    DBObject anyEncryptedFieldsExists = new BasicDBObject("$or", encryptedFieldExists);

    assertThat(
        wingsPersistence.getCollection(DirectKubernetesInfrastructureMapping.class).count(anyEncryptedFieldsExists))
        .isEqualTo(GENERATED_ENTITIES_COUNT);

    migration.migrate();
    assertThat(
        wingsPersistence.getCollection(DirectKubernetesInfrastructureMapping.class).count(anyEncryptedFieldsExists))
        .isZero();
  }

  private void prepareEntities() {
    Function<Integer, Set<String>> getEncryptedFields = step -> {
      int amount = step % ENCRYPTED_FIELDS.size() + 1;
      return IntStream.range(0, amount).mapToObj(ENCRYPTED_FIELDS::get).collect(Collectors.toSet());
    };

    IntStream.range(0, GENERATED_ENTITIES_COUNT).mapToObj(getEncryptedFields::apply).forEach(encryptedFields -> {
      String savedEntityKey = wingsPersistence.save(
          DirectKubernetesInfrastructureMapping.builder().accountId("accountId").appId("appId").envId("envId").build());

      BasicDBObject encryptedFieldsUpdate = new BasicDBObject();
      encryptedFields.forEach(field -> encryptedFieldsUpdate.append(field, "encryptedValue"));
      BasicDBObject updateObject = new BasicDBObject("$set", encryptedFieldsUpdate);
      wingsPersistence.getCollection(DirectKubernetesInfrastructureMapping.class)
          .update(new BasicDBObject("_id", savedEntityKey), updateObject);
    });
  }
}
