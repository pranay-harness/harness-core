package io.harness.query.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.experimental.UtilityClass;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class QueryShapeDetector {
  ConcurrentMap<String, QueryHashInfo> queryHashCache = new ConcurrentHashMap<>();

  public QueryHashInfo getQueryHash(String collectionName, Document queryDoc) {
    String queryHash = calculateQueryHash(collectionName, queryDoc);
    return queryHashCache.computeIfAbsent(queryHash, hash -> new QueryHashInfo(collectionName, queryDoc, hash));
  }

  private String calculateQueryHash(String collectionName, Document queryDoc) {
    return "";
  }
}
