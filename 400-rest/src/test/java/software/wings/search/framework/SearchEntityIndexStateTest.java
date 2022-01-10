/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;

import com.google.inject.Inject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SearchEntityIndexStateTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void shouldBulkSyncTest() {
    SearchEntityIndexState searchEntityIndexState =
        new SearchEntityIndexState(ApplicationSearchEntity.class.getCanonicalName(), "0.05", "indexName", false);

    boolean shouldBulkSync = searchEntityIndexState.shouldBulkSync();
    assertThat(shouldBulkSync).isTrue();

    searchEntityIndexState =
        new SearchEntityIndexState(ApplicationSearchEntity.class.getCanonicalName(), "0.2", "indexName", false);
    shouldBulkSync = searchEntityIndexState.shouldBulkSync();
    assertThat(shouldBulkSync).isFalse();

    searchEntityIndexState =
        new SearchEntityIndexState(ApplicationSearchEntity.class.getCanonicalName(), "0.2", "indexName", true);
    shouldBulkSync = searchEntityIndexState.shouldBulkSync();
    assertThat(shouldBulkSync).isTrue();
  }
}
