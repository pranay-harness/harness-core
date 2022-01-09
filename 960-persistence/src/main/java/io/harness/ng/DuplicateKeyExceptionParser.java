/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
