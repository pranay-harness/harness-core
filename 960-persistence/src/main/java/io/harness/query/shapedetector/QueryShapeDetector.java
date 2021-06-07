package io.harness.query.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.query.shapedetector.QueryHashInfo.QueryHashKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
@Slf4j
@UtilityClass
public class QueryShapeDetector {
  private static final Object DEFAULT_VALUE = 1;
  private static final Set<String> ARRAY_TRIM_OPERATORS =
      Sets.newHashSet("$eq", "$in", "$nin", "$all", "$distinct", "$mod");

  ConcurrentMap<QueryHashKey, QueryHashInfo> queryHashCache = new ConcurrentHashMap<>();

  public String getQueryHash(String collectionName, Document queryDoc) {
    QueryHashKey queryHashKey = calculateQueryHashKey(collectionName, queryDoc);
    QueryHashInfo queryHashInfo =
        queryHashCache.computeIfAbsent(queryHashKey, hashKey -> new QueryHashInfo(queryHashKey, queryDoc));
    return String.valueOf(queryHashInfo.getQueryHashKey().hashCode());
  }

  public QueryHashKey calculateQueryHashKey(String collectionName, Document queryDoc) {
    String queryHash = calculateQueryDocHash(queryDoc);
    return QueryHashKey.builder().collectionName(collectionName).queryHash(queryHash).build();
  }

  private String calculateQueryDocHash(Document queryDoc) {
    Document normalizedQueryDoc = normalizeMap(queryDoc);
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonString = normalizedQueryDoc.toJson();
    try {
      JsonNode jsonNode = objectMapper.readTree(objectMapper.getFactory().createParser(jsonString));
      return String.valueOf(jsonNode.hashCode());
    } catch (IOException e) {
      log.error("Unable to parse the query json", e);
      throw new InvalidRequestException("Unable to parse the query json");
    }
  }

  @VisibleForTesting
  Object normalizeObject(Object object) {
    if (object == null) {
      return null;
    }
    if (object instanceof Map) {
      return normalizeMap((Map<String, Object>) object);
    }
    if (object instanceof List) {
      return normalizeList((List<Object>) object);
    }
    return DEFAULT_VALUE;
  }

  private Document normalizeMap(Map<String, Object> doc) {
    Document copy = new Document();
    if (EmptyPredicate.isEmpty(doc)) {
      return copy;
    }

    List<ImmutablePair<String, Object>> normalizedEntries = new ArrayList<>();
    // Recursively normalize
    for (Map.Entry<String, Object> entry : doc.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof List && needToTrimList(key)) {
        value = Collections.singletonList(DEFAULT_VALUE);
      } else {
        value = normalizeObject(value);
      }
      normalizedEntries.add(ImmutablePair.of(key, value));
    }

    // Sort the entries
    normalizedEntries.sort(Comparator.comparing(ImmutablePair::getLeft));
    normalizedEntries.forEach(e -> copy.put(e.getLeft(), e.getRight()));
    return copy;
  }

  private List<Object> normalizeList(List<Object> list) {
    if (EmptyPredicate.isEmpty(list)) {
      return Collections.emptyList();
    }
    return list.stream().map(QueryShapeDetector::normalizeObject).collect(Collectors.toList());
  }

  private boolean needToTrimList(String key) {
    return !key.startsWith("$") || ARRAY_TRIM_OPERATORS.contains(key);
  }
}
