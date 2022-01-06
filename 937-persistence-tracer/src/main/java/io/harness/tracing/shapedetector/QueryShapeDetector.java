/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.tracing.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class QueryShapeDetector {
  private static final Object DEFAULT_VALUE = 1;
  private static final Set<String> ARRAY_TRIM_OPERATORS = Sets.newHashSet("$eq", "$in", "$nin", "$all", "$mod");

  ConcurrentMap<QueryHashKey, QueryHashInfo> queryHashCache = new ConcurrentHashMap<>();

  public String getQueryHash(String collectionName, Document queryDoc, Document sortDoc) {
    QueryHashKey queryHashKey = calculateQueryHashKey(collectionName, queryDoc, sortDoc);
    QueryHashInfo queryHashInfo = queryHashCache.computeIfAbsent(queryHashKey,
        hashKey -> QueryHashInfo.builder().queryHashKey(queryHashKey).queryDoc(queryDoc).sortDoc(sortDoc).build());
    return String.valueOf(queryHashInfo.getQueryHashKey().hashCode());
  }

  public QueryHashKey calculateQueryHashKey(String collectionName, Document queryDoc, Document sortDoc) {
    String queryHash = calculateDocHash(normalizeMap(queryDoc, false));
    String sortHash = calculateDocHash(normalizeMap(sortDoc, true));
    return QueryHashKey.builder().collectionName(collectionName).queryHash(queryHash).sortHash(sortHash).build();
  }

  private String calculateDocHash(Document doc) {
    if (doc == null) {
      return "";
    }
    // Document hashCode doesnt take order into account, thus keys order in linkedHashMap doesnt map
    return String.valueOf(doc.hashCode());
  }

  @VisibleForTesting
  Object normalizeObject(Object object, boolean prefixField) {
    if (object == null) {
      // null is not converted to default value as null might not work with usual indices
      return null;
    }
    if (object instanceof Map) {
      return normalizeMap((Map<String, Object>) object, prefixField);
    }
    if (object instanceof List) {
      return normalizeList((List<Object>) object, prefixField);
    }
    return DEFAULT_VALUE;
  }

  private Document normalizeMap(Map<String, Object> doc, boolean prefixField) {
    Document copy = new Document();
    if (EmptyPredicate.isEmpty(doc)) {
      return copy;
    }

    List<ImmutablePair<String, Object>> normalizedEntries = new ArrayList<>();
    // Recursively normalize
    Integer prefixCount = 1;
    for (Map.Entry<String, Object> entry : doc.entrySet()) {
      String key = prefixField ? (prefixCount + entry.getKey()) : entry.getKey();
      Object value = entry.getValue();
      if (!prefixField && value instanceof List && needToTrimList(key)) {
        value = Collections.singletonList(DEFAULT_VALUE);
      } else {
        value = normalizeObject(value, prefixField);
      }
      normalizedEntries.add(ImmutablePair.of(key, value));
      prefixCount++;
    }

    // Sort the entries
    normalizedEntries.sort(Comparator.comparing(ImmutablePair::getLeft));
    normalizedEntries.forEach(e -> copy.put(e.getLeft(), e.getRight()));
    return copy;
  }

  private List<Object> normalizeList(List<Object> list, boolean prefixField) {
    if (EmptyPredicate.isEmpty(list)) {
      return Collections.emptyList();
    }
    return list.stream().map(object -> normalizeObject(object, prefixField)).collect(Collectors.toList());
  }

  private boolean needToTrimList(String key) {
    return key.charAt(0) != '$' || ARRAY_TRIM_OPERATORS.contains(key);
  }
}
