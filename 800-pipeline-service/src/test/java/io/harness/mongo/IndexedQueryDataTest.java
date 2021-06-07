package io.harness.mongo;

import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.packages.HarnessPackages;
import io.harness.persistence.IndexedQueryData;
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
@OwnedBy(HarnessTeam.PIPELINE)
public class IndexedQueryDataTest extends PipelineServiceTestBase {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testConfirmAllIndexedQueries() throws Exception {
    Set<Class<? extends IndexedQueryData>> indexedQueries = new HashSet<>();
    Reflections reflections =
        new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS, "io.serializer");
    indexedQueries.addAll(reflections.getSubTypesOf(IndexedQueryData.class));
    List<String> indexedQueriesCanonicalForm = new ArrayList<>();
    for (Class<? extends IndexedQueryData> indexedQuery : indexedQueries) {
      indexedQueriesCanonicalForm.add(indexedQuery.newInstance().getCanonicalForm());
    }
    indexedQueriesCanonicalForm.sort(String::compareTo);

    List<String> expectedIndexesRaw;
    try (InputStream in = getClass().getResourceAsStream("/mongo/indexedQueries.txt")) {
      expectedIndexesRaw = IOUtils.readLines(in, "UTF-8");
    }
    List<String> expectedIndexes = new ArrayList<>();
    StringBuilder currentIndex = new StringBuilder();
    for (String expectedIndexRaw : expectedIndexesRaw) {
      if (Character.isWhitespace(expectedIndexRaw.charAt(0))) {
        currentIndex.append(expectedIndexRaw.toString().trim());
      } else {
        if (EmptyPredicate.isNotEmpty(currentIndex.toString())) {
          expectedIndexes.add(currentIndex.toString());
        }
        currentIndex = new StringBuilder();
        currentIndex.append(expectedIndexRaw);
      }
    }
    if (EmptyPredicate.isNotEmpty(currentIndex.toString())) {
      expectedIndexes.add(currentIndex.toString());
    }
    expectedIndexes.sort(String::compareTo);
    assertThat(indexedQueriesCanonicalForm).isEqualTo(expectedIndexes);
  }
}
