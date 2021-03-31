package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class JiraIssueUtils {
  public List<String> splitByComma(String value) {
    List<String> values = new ArrayList<>();
    if (isBlank(value)) {
      return values;
    }

    for (String s : Splitter.on(JiraConstantsNG.COMMA_SPLIT_PATTERN).trimResults().split(value)) {
      if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
        String str = s.substring(1, s.length() - 1).trim();
        values.add(str.replaceAll("\"\"", "\""));
      } else {
        values.add(s);
      }
    }
    return values;
  }
}
