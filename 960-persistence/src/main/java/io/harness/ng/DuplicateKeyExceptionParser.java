/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng;

import com.google.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
@Singleton
public class DuplicateKeyExceptionParser {
  public static JSONObject getDuplicateKey(String exceptionMessage) {
    try {
      Pattern pattern = Pattern.compile("dup key: \\{.*?\\}");
      Matcher matcher = pattern.matcher(exceptionMessage);
      if (matcher.find()) {
        String matchedUniqueKey = matcher.group(0);
        String[] removingDupKeyFromString = matchedUniqueKey.split("dup key: ");
        String jsonString = removingDupKeyFromString[1];
        return new JSONObject(jsonString);
      }
    } catch (Exception ex) {
      log.info("Encountered exception while reading the duplicate key", ex);
    }
    return null;
  }
}
