/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.utility;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class AzureLogParser {
  private static final String timePattern = "yyyy-MM-dd'T'HH:mm:ss";
  DateTimeFormatter formatter = DateTimeFormat.forPattern(timePattern).withZoneUTC();

  private static final String timestampRegex = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\\s*";
  private static final Pattern successPattern = Pattern.compile("Deployment successful\\.", Pattern.CASE_INSENSITIVE);
  private static final Pattern containerSuccessPattern =
      Pattern.compile("initialized successfully and is ready to serve requests\\.", Pattern.CASE_INSENSITIVE);
  private static final Pattern welcomeLogPattern =
      Pattern.compile("Welcome, you are now connected to log-streaming service.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern failureContainerLogPattern =
      Pattern.compile("ERROR - Container .* didn't respond to HTTP pings on port:", Pattern.CASE_INSENSITIVE);
  private static final Pattern timestampPattern = Pattern.compile(timestampRegex, Pattern.CASE_INSENSITIVE);

  public boolean checkIsSuccessDeployment(String log, boolean containerDeployment) {
    if (containerDeployment) {
      Matcher matcher = containerSuccessPattern.matcher(log);
      return matcher.find();
    } else {
      Matcher matcher = successPattern.matcher(log);
      return matcher.find();
    }
  }

  public boolean checkIsWelcomeLog(String log) {
    Matcher matcher = welcomeLogPattern.matcher(log);
    return matcher.find();
  }

  public String removeTimestamp(@NotNull String log) {
    return log.replaceAll(timestampRegex, "");
  }

  public boolean shouldLog(String log) {
    return !checkIsWelcomeLog(log) && !log.isEmpty();
  }

  public boolean checkIfFailed(String log, boolean containerDeployment) {
    if (!containerDeployment) {
      return false;
    }
    Matcher matcher = failureContainerLogPattern.matcher(log);
    return matcher.find();
  }

  public Optional<DateTime> parseTime(String log) {
    Matcher timestampMatcher = timestampPattern.matcher(log);
    if (timestampMatcher.find()) {
      String timestamp = timestampMatcher.group();
      timestamp = timestamp.trim();
      return Optional.of(formatter.parseDateTime(timestamp));
    }

    return Optional.empty();
  }
}
