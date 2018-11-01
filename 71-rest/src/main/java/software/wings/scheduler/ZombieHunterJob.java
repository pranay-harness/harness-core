package software.wings.scheduler;

/*
mmmmmmmyoddddddddddddddddddddddddddddddddddddddddd-mdddddddd
mmmmmmms+mdddddddddddddddddddddddddddddddddddddddd-ddddddddd
mmmmmmm+omdddddddddddddddddddddddddddddddddddddddd-ydddddddd
mmmmmmm/+mdddddddddddddddddddddddddddddddddddddddd.smddddddd
mmmmhdm+.mdddddddddddddddddddddddddddddddddddddddh ddsmddddd
mmmmh+mh hmddmddddddddddddddddddddddddddddddddddd/.md:mddddd
mmmmm:dm--dmdhmmddddddddddddddddddddddddddddhhmds sm+smddddd
mmmmms:ms :dm/dmdddddddddddddddddddddddddddmsymy`.my-mmmmmdd
mmmmmm:+m+`:doomddddomdddddddddmddddmdomdddd-dy`-dh-ymmmmmdm
mmmmmmd//s+ /d-dmmdd/dddddmmddddmddmdssmmdmo+y``o/:ymmmmmmmm
mmmmmmmdho+:...-shdmo+mdyo/-....-/shd-dmhs/`.-:+shdmmmmmmmmm
mmmmmmmmmmmmdyo:-.-oo.s-`          `:`+-.-/shdmmmmmmmmmmmmmm
mmmmmmmmmmmy+.:ddhs/`   `         `    sddmmmmdyymmmmmmmmmmm
mmmmmmmmmmm/   -shmd-   .`        -    /mmdhy+. `hmmmmmmmmmm
mmmmmmmmmmmdyo/-.-:+- `  ..     `-`  . `s:-------ymmmmmmmmmm
mmmmmmmmmmmmmmmmdhy:` -.  `-`  .-    -``/shhddddmmmmmmmmmmmm
mmmmmmmmmmmmmmmmmmm.. ..` ``- `.`  .`.`./mmmmmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmmmmm/-.ohho:.````-/ydy/`-smmmmmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmmmmmh-smmmmmd+`.ydmmmmd::dmmmmmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmmmmm+``-/oyys.`.:yys+:.  /dmmmmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmddhhs:``:o``` shd/ ```s/.`/.:ohdmmmmmmmmmmmmmm
mmmmmmmmmmmmmm:.../hhhmy`   +yy-   `hddhh/  .smmmmmmmmmmmmmm
mmmmmdhhmmmmmm:.odmmmmy-.--....`.-...ommmms-ommmmmmmddmmmmmm
mmmmmy .ydhhmmmmmmmmmmds::--.o/.`:-:+hmmmmmmmmmmhoo/-/mmmmmm
mmmmmd  oy`.smhodmmdmmmmmmdhyddyhmmmmmmmmdhds+hs` y/ :mmmmmm
mmmmmh  /s  sm- -d/-ymh+hmdsdmdhohhy+/sdy-`/:+mh  o- +mmmmmm
mmmmmh  ++  om/ .N. om- .d+ /m: :- `s  mh  ysdmy  s/ .mmmmmm
mmmmmh-`oh  ym: .d` sm:  :o om/.y/ .d++dy  /.ymh  h+`+mmmmmm
mmmmmmmydh-`om+ -N. ym: o.  +mmdm: `mmmmd  yssds`:hdhmmmmmmm
mmmmmmmmmmmhdmd/.+.`hm: dm. ommmm/ .mmmmy``/:/dmdmmmmmmmmmmm
mmmmmmmmmmmmmmmmmshdNm+-hm/`ommmd+`/dmmmdhhmmmmmmmmmmmmmmmmm
*/

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.ReadPref;
import io.harness.scheduler.PersistentScheduler;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.collections.map.LRUMap;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.Exterminator;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public class ZombieHunterJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ZombieHunterJob.class);

  public static final String GROUP = "ZOMBIE_HUNTER_GROUP";
  public static final String INDEX_KEY = "index";
  public static final Duration TIMEOUT = Duration.ofMinutes(2);

  public static final Random random = new SecureRandom();
  private static final String[] KILL_METHOD = {
      "shot in the head", "stabbed in the head", "impelled in the head", "decapitated", "skull crushed", "axed"};
  private static final String[] SQUAD_MEMBER = {"Rick", "Daryl", "Michonne", "Glenn", "Maggie", "Carl"};

  private static final OffsetDateTime OUTBREAK_DAY =
      OffsetDateTime.of(LocalDate.of(2010, 10, 31).atStartOfDay(), ZoneOffset.UTC);
  protected static final OffsetDateTime today = OffsetDateTime.now();

  private static final int CACHE_MAX_SIZE = 5000;
  private LRUMap cache = new LRUMap(CACHE_MAX_SIZE);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;

  @Inject private AppService appService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @AllArgsConstructor
  @Value
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

  @SuppressFBWarnings("MS_MUTABLE_COLLECTION_PKGPROTECT")
  protected static final List<ZombieType> zombieTypes =
      asList(new ZombieType("applications", "accountId", asList("accounts"), null),
          new ZombieType("artifactStream", ArtifactStream.SERVICE_ID_KEY, asList(SERVICES), null),
          new ZombieType("artifactStream", ArtifactStream.SETTING_ID_KEY, asList(SETTING_ATTRIBUTES), null),
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
    // Scheduling a job for every zombie type on an `interval` distance from each other.
    // The algorithm preserves the schedule from execution to execution to avoid restarting
    // the cycle after every deploy. This will be broken only if new zombie types are
    // added, but this is not critical.
    for (int i = 0; i < zombieTypes.size(); ++i) {
      String id = "" + i;
      // If somehow this job was scheduled from before make sure the trigger is correct.
      jobScheduler.deleteJob(id, GROUP);

      JobDetail details =
          JobBuilder.newJob(ZombieHunterJob.class).withIdentity(id, GROUP).usingJobData(INDEX_KEY, i).build();

      Trigger trigger = defaultTrigger(i);
      jobScheduler.scheduleJob(details, trigger);
    }
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
    int index = map.getInt(INDEX_KEY);

    // Check if a zombie type was exterminated.
    if (index >= zombieTypes.size()) {
      jobScheduler.deleteJob("" + index, GROUP);
      return;
    }

    executorService.submit(() -> executeAsync(index));
  }

  public void executeAsync(int index) {
    try (AcquiredLock lock = persistentLocker.acquireLock(ZombieHunterJob.class, "expedition", TIMEOUT)) {
      final ZombieType zombieType = zombieTypes.get(index);

      huntingExpedition(zombieType);

    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Error seen in the ZombieHunterJob  execute call", exception);
    }
  }

  int huntingExpedition(ZombieType zombieType) {
    List<DBCollection> owners = zombieType.ownerCollections.stream()
                                    .map(owner -> wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, owner))
                                    .collect(toList());

    BasicDBObject select = new BasicDBObject();
    select.put(ID_KEY, 1);
    select.put(APP_ID_KEY, 1);
    select.put(zombieType.ownerFieldName, 1);

    BasicDBObject selectOwner = new BasicDBObject();
    selectOwner.put(ID_KEY, 1);

    int count = 0;
    final DBCursor dbCursor =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, zombieType.collection).find(null, select);
    while (dbCursor.hasNext()) {
      final DBObject object = dbCursor.next();
      final Object ownerId = object.get(zombieType.ownerFieldName);

      if (getDbObject(owners, selectOwner, ownerId) != null) {
        continue;
      }

      ++count;
      final Object entityId = object.get(ID_KEY);
      if (zombieType.exterminator == null) {
        if (logger.isWarnEnabled()) {
          logger.warn(format("Zombie %s from %s.%s=%s will be spreading the deadly virus.", entityId,
              zombieType.collection, zombieType.ownerFieldName, ownerId));
        }
        continue;
      }

      final Object appId = object.get(APP_ID_KEY);
      zombieType.exterminator.delete(appId.toString(), entityId.toString());

      if (logger.isWarnEnabled()) {
        logger.warn(format("Zombie %s from %s.%s=%s was discovered. It was %s by %s.", entityId, zombieType.collection,
            zombieType.ownerFieldName, ownerId, KILL_METHOD[random.nextInt(KILL_METHOD.length)],
            SQUAD_MEMBER[random.nextInt(SQUAD_MEMBER.length)]));
      }
    }

    return count;
  }

  private DBObject getDbObject(List<DBCollection> owners, BasicDBObject selectOwner, Object ownerId) {
    if (cache.containsKey(ownerId)) {
      return (DBObject) cache.get(ownerId);
    }

    DBObject ownerObject = null;
    final BasicDBObject queryOwner = new BasicDBObject("_id", ownerId);
    for (DBCollection owner : owners) {
      if ((ownerObject = owner.findOne(queryOwner, selectOwner)) != null) {
        break;
      }
    }
    cache.put(ownerId, ownerObject);

    return ownerObject;
  }
}
