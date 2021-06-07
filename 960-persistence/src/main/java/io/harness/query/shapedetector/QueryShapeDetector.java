package io.harness.query.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.query.shapedetector.QueryHashInfo.QueryHashKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@UtilityClass
public class QueryShapeDetector {
  ConcurrentMap<QueryHashKey, QueryHashInfo> queryHashCache = new ConcurrentHashMap<>();

  public QueryHashInfo getQueryHash(String collectionName, Document queryDoc) {
    String queryHash = calculateQueryHash(collectionName, queryDoc);
    QueryHashKey queryHashKey = QueryHashKey.builder().queryHash(queryHash).collectionName(collectionName).build();
    return queryHashCache.computeIfAbsent(
        queryHashKey, hashKey -> new QueryHashInfo(collectionName, queryDoc, hashKey.getQueryHash()));
  }

  public String calculateQueryHash(String collectionName, Document queryDoc) {
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonString = queryDoc.toJson();
    try {
      JsonNode jsonNode = objectMapper.readTree(objectMapper.getFactory().createParser(jsonString));
      return String.valueOf(jsonNode.hashCode());
    } catch (IOException e) {
      log.error("Unable to parse the query json  - ", e);
      throw new InvalidRequestException("Unable to parse the query json");
    }
  }
}
