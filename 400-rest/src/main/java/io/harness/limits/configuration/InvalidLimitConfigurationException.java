package io.harness.limits.configuration;

import static java.lang.System.lineSeparator;

import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class InvalidLimitConfigurationException extends RuntimeException {
  private ObjectMapper mapper = new ObjectMapper();
  private ConfiguredLimit configuredLimit;
  private String reason;

  public InvalidLimitConfigurationException(ConfiguredLimit configuredLimit, String reason) {
    this.configuredLimit = configuredLimit;
    this.reason = reason;
  }

  @Override
  public String getMessage() {
    String title = "Invalid limit configured in database: ";
    String invalidLimit;
    try {
      invalidLimit = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuredLimit);
    } catch (JsonProcessingException e) {
      invalidLimit = configuredLimit.toString();
      log.error("Could not convert configured limit to JSON: {}", invalidLimit);
    }

    String reasonLine = "REASON: " + reason;
    String exampleTitle = "Valid configuration EXAMPLES: (id should be generated by mongo)";
    String examples = examples();

    return StringUtils.join(
        new String[] {title, invalidLimit, reasonLine, exampleTitle, examples}, lineSeparator() + lineSeparator());
  }

  private String examples() {
    try {
      StringBuilder stringBuilder = new StringBuilder();
      ConfiguredLimit configuredLimit =
          new ConfiguredLimit<>("some-account-id", new StaticLimit(10), ActionType.CREATE_APPLICATION);

      stringBuilder.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuredLimit))
          .append(',')
          .append(lineSeparator());

      configuredLimit =
          new ConfiguredLimit<>("some-account-id", new RateLimit(100, 24, TimeUnit.HOURS), ActionType.DEPLOY);
      stringBuilder.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuredLimit));
      return stringBuilder.toString();
    } catch (JsonProcessingException e) {
      return "Error while trying to get examples. Suggestion: Use LimitConfigurationService to configure a limit.";
    }
  }
}
