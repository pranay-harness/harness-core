package io.harness.iterator;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PersistentCronIterable extends PersistentIrregularIterable {
  boolean skipMissed();

  CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

  default boolean expandNextIterations(String cronExpression, List<Long> times) {
    // times is output list, it must not be empty
    if (times.size() > 2) {
      return false;
    }

    final Cron cron = parser.parse(cronExpression);
    ExecutionTime executionTime = ExecutionTime.forCron(cron);

    if (times.isEmpty()) {
      times.add(ZonedDateTime.now().toInstant().toEpochMilli());
    }

    ZonedDateTime time = Instant.ofEpochMilli(times.get(times.size() - 1)).atZone(ZoneOffset.UTC);

    for (int i = 0; i < 10; i++) {
      final Optional<ZonedDateTime> nextTime = executionTime.nextExecution(time);
      if (!nextTime.isPresent()) {
        break;
      }

      time = nextTime.get();
      if (skipMissed() && time.isBefore(ZonedDateTime.now())) {
        continue;
      }

      times.add(time.toInstant().toEpochMilli());
    }

    return true;
  }
}
