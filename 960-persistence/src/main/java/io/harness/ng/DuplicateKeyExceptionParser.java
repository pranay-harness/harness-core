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
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject;
      }
    } catch (Exception ex) {
      log.info("Encountered exception while reading the duplicate key", ex);
    }
    return null;
  }
}
