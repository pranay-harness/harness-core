/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.packages.HarnessPackages;
import io.harness.query.PersistentQuery;
import io.harness.rule.Owner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

@Slf4j
public class QueriesTest extends DelegateServiceTestBase {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testConfirmAllIndexedQueries() throws Exception {
    Set<Class<? extends PersistentQuery>> indexedQueries = new HashSet<>();
    Reflections reflections = new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS);
    indexedQueries.addAll(reflections.getSubTypesOf(PersistentQuery.class));
    List<String> indexedQueriesCanonicalForm = new ArrayList<>();
    for (Class<? extends PersistentQuery> indexedQuery : indexedQueries) {
      indexedQueriesCanonicalForm.addAll(indexedQuery.newInstance().queryCanonicalForms());
    }
    indexedQueriesCanonicalForm.sort(String::compareTo);
    List<String> expectedIndexesRaw;
    try (InputStream in = getClass().getResourceAsStream("/mongo/queries.txt")) {
      expectedIndexesRaw = IOUtils.readLines(in, "UTF-8");
    }
    List<String> expectedQueries = new ArrayList<>();
    StringBuilder currentQuery = new StringBuilder();
    for (String expectedIndexRaw : expectedIndexesRaw) {
      if (Character.isWhitespace(expectedIndexRaw.charAt(0))) {
        currentQuery.append('\n').append(expectedIndexRaw);
      } else {
        if (EmptyPredicate.isNotEmpty(currentQuery.toString())) {
          expectedQueries.add(currentQuery.toString());
        }
        currentQuery = new StringBuilder();
        currentQuery.append(expectedIndexRaw);
      }
    }
    if (EmptyPredicate.isNotEmpty(currentQuery.toString())) {
      expectedQueries.add(currentQuery.toString());
    }
    expectedQueries.sort(String::compareTo);
    assertThat(indexedQueriesCanonicalForm)
        .as("\n" + String.join("\n", indexedQueriesCanonicalForm) + "\n")
        .isEqualTo(expectedQueries);
  }
}
