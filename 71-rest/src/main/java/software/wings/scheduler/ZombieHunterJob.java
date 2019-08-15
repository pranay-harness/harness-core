package software.wings.scheduler;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.Exterminator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Deprecated
@Slf4j
// TODO: this job is no longer needed, delete after 2019/09/30
public class ZombieHunterJob implements Job {
  public static final String GROUP = "ZOMBIE_HUNTER_GROUP";
  public static final String INDEX_KEY = "index";
  public static final Duration TIMEOUT = Duration.ofMinutes(2);

  private static final OffsetDateTime OUTBREAK_DAY =
      OffsetDateTime.of(LocalDate.of(2010, 10, 31).atStartOfDay(), ZoneOffset.UTC);
  protected static final OffsetDateTime today = OffsetDateTime.now();

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Value
  @AllArgsConstructor
  static class ZombieType {
    private String collection;
    private String ownerFieldName;
    // The owner can be in any of these collections
    private List<String> ownerCollections;
    private Exterminator exterminator;
  }

  public static final String ENVIRONMENTS = "environments";
  public static final String SERVICES = "services";
  public static final String SETTING_ATTRIBUTES = "settingAttributes";

  protected static final ImmutableList<ZombieType> zombieTypes =
      ImmutableList.of(new ZombieType("applications", "accountId", asList("accounts"), null),
          new ZombieType("infrastructureMapping", InfrastructureMapping.SERVICE_ID_KEY, asList(SERVICES), null),
          new ZombieType("infrastructureMapping", InfrastructureMapping.ENV_ID_KEY, asList(ENVIRONMENTS), null),
          new ZombieType("serviceTemplates", ServiceTemplate.SERVICE_ID_KEY, asList(SERVICES), null),
          new ZombieType("serviceVariables", "entityId", asList(SERVICES, "serviceTemplates", "environments"), null));

  public static final Duration interval = Duration.ofDays(1);
  public static final Duration cycle = interval.multipliedBy(zombieTypes.size());

  public static OffsetDateTime nextHuntingExpedition(int index) {
    final long huntingCycles = Duration.between(OUTBREAK_DAY, today).getSeconds() / cycle.getSeconds();
    OffsetDateTime currentHuntingCycle = OUTBREAK_DAY.plus(huntingCycles * cycle.getSeconds(), ChronoUnit.SECONDS);
    OffsetDateTime date = currentHuntingCycle.plus(interval.multipliedBy(index)).plus(8, ChronoUnit.HOURS);
    if (date.isBefore(today)) {
      date = date.plus(cycle);
    }

    return date;
  }

  public static Trigger defaultTrigger(int index) {
    final TriggerBuilder<SimpleTrigger> builder =
        TriggerBuilder.newTrigger()
            .startAt(Date.from(nextHuntingExpedition(index).toInstant()))
            .withIdentity("" + index, GROUP)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(zombieTypes.size() * 24).repeatForever());

    return builder.build();
  }

  public static void scheduleJobs(PersistentScheduler jobScheduler) {
    for (int i = 0; i < zombieTypes.size(); ++i) {
      String id = "" + i;
      jobScheduler.deleteJob(id, GROUP);
    }
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {}
}
