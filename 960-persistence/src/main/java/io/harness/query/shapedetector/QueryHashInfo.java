package io.harness.query.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Value;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
public class QueryHashInfo {
  String collectionName;
  Document queryDoc;
  String queryHash;
}
