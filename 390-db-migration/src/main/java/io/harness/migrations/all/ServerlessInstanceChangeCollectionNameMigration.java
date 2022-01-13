/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.DBCollection;

public class ServerlessInstanceChangeCollectionNameMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection sereverlessInstanceCollectionWithHyphen =
        wingsPersistence.getCollection(DEFAULT_STORE, "serverless-instance");

    boolean doesDataBaseContainServerlessInstance =
        sereverlessInstanceCollectionWithHyphen.getDB().getCollectionNames().contains("serverless-instance");

    if (doesDataBaseContainServerlessInstance) {
      sereverlessInstanceCollectionWithHyphen.rename("serverlessInstance", true);
    }
  }
}
