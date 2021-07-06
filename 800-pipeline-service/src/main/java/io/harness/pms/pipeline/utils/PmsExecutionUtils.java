package io.harness.pms.pipeline.utils;

import io.harness.serializer.JsonUtils;

import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@UtilityClass
@Slf4j
public class PmsExecutionUtils {
  /**
   * This method is used for backward compatibility
   * @param stepParameters can be of type {@link Document} (current)
   *                      and {@link java.util.LinkedHashMap} (before recaster)
   * @return document representation of step parameters
   */
  public Document extractToDocument(Object stepParameters) {
    if (stepParameters == null) {
      return Document.parse("{}");
    }
    if (stepParameters instanceof Document) {
      return (Document) stepParameters;
    } else if (stepParameters instanceof Map) {
      return Document.parse(JsonUtils.asJson(stepParameters));
    } else {
      throw new IllegalStateException(String.format("Unable to parse stepParameters %s", stepParameters.getClass()));
    }
  }
}
