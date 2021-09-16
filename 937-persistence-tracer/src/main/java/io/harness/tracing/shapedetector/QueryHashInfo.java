package io.harness.tracing.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class QueryHashInfo {
  QueryHashKey queryHashKey;
  Document queryDoc;
  Document sortDoc;
}
