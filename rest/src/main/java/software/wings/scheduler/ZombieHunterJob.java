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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
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
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.Exterminator;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public class ZombieHunterJob implements Job {
  protected static Logger logger = LoggerFactory.getLogger(ZombieHunterJob.class);

  public static final String GROUP = "ZOMBIE_HUNTER_GROUP";
  public static final String INDEX_KEY = "index";

  public static final Random random = new Random();
  public static final String[] KILL_METHOD = {
      "shot in the head", "stabbed in the head", "impelled in the head", "decapitated", "skull crushed", "axed"};
  public static final String[] SQUAD_MEMBER = {"Rick", "Daryl", "Michonne", "Glenn", "Maggie", "Carl"};

  public static final long OUTBREAK_DATE = 1288515600000L;
  public static Calendar today = Calendar.getInstance();

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;

  @Inject private AppService appService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @AllArgsConstructor
  @Value
  static class ZombieType {
    private String collection;
    private String ownerFieldName;
    private String ownerCollection;
    private Exterminator exterminator;
  }

  public static List<ZombieType> zombieTypes = asList(new ZombieType("applications", "accountId", "accounts", null),
      new ZombieType("artifactStream", "serviceId", "services", null),
      new ZombieType("infrastructureMapping", "serviceId", "services", null),
      new ZombieType("serviceTemplates", "serviceId", "services", null),
      new ZombieType("serviceVariables", "entityId", "services", null));

  public static Duration interval = Duration.ofDays(1);
  public static Duration cycle = interval.multipliedBy(zombieTypes.size());

  public static long nextHuntingExpedition(int index) {
    final long hunting_cycles = (today.getTimeInMillis() - OUTBREAK_DATE) / cycle.toMillis();
    final long current_hunting_cycle = OUTBREAK_DATE + hunting_cycles * cycle.toMillis();
    long hunting_squad = current_hunting_cycle + interval.toMillis() * index;
    if (hunting_squad < today.getTimeInMillis()) {
      hunting_squad += cycle.toMillis();
    }

    return hunting_squad;
  }

  public static Trigger defaultTrigger(int index) {
    final TriggerBuilder<SimpleTrigger> builder =
        TriggerBuilder.newTrigger()
            .startAt(new Date(nextHuntingExpedition(index)))
            .withIdentity("" + index, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(zombieTypes.size() * 24));

    return builder.build();
  }

  public static void scheduleJobs(QuartzScheduler jobScheduler) {
    // Scheduling a job for every zombie type on an `interval` distance from each other.
    // The algo preserves the schedule from execution to execution to avoid restarting
    // the cycle after every deploy. This will be broken only if new zombie types are
    // added, but this is not critical.
    for (int i = 0; i < zombieTypes.size(); ++i) {
      String id = "" + i;
      // If somehow this job was scheduled from before make sure the trigger is correct.
      jobScheduler.deleteJob(id, GROUP);

      JobDetail details =
          JobBuilder.newJob(ZombieHunterJob.class).withIdentity(id, GROUP).usingJobData(INDEX_KEY, i).build();

      org.quartz.Trigger trigger = defaultTrigger(i);
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
    try (AcquiredLock lock = persistentLocker.acquireLock(ZombieHunterJob.class, "expedition", Duration.ofMinutes(2))) {
      final ZombieType zombieType = zombieTypes.get(index);

      huntingExpedition(zombieType);

    } catch (WingsException exception) {
      exception.logProcessedMessages(logger);
    } catch (Exception exception) {
      logger.error("Error seen in the ZombieHunterJob  execute call", exception);
    }
  }

  int huntingExpedition(ZombieType zombieType) {
    final DBCollection owner = wingsPersistence.getCollection(zombieType.ownerCollection);

    LRUMap map = new LRUMap(5000);

    BasicDBObject select = new BasicDBObject();
    select.put(ID_KEY, 1);
    select.put(APP_ID_KEY, 1);
    select.put(zombieType.ownerFieldName, 1);

    int count = 0;
    final DBCursor dbCursor = wingsPersistence.getCollection(zombieType.collection).find(null, select);
    while (dbCursor.hasNext()) {
      final DBObject object = dbCursor.next();
      final Object ownerId = object.get(zombieType.ownerFieldName);

      DBObject ownerObject = null;
      if (map.containsKey(ownerId)) {
        ownerObject = (DBObject) map.get(ownerId);
      } else {
        ownerObject = owner.findOne(new BasicDBObject("_id", ownerId));
        map.put(ownerId, ownerObject);
      }

      if (ownerObject == null) {
        ++count;
        final Object entityId = object.get(ID_KEY);
        if (zombieType.exterminator != null) {
          final Object appId = object.get(APP_ID_KEY);
          zombieType.exterminator.delete(appId.toString(), entityId.toString());

          logger.warn(format("Zombie %s from %s.%s=%s was discovered. It was %s by %s.", entityId,
              zombieType.collection, zombieType.ownerFieldName, ownerId,
              KILL_METHOD[random.nextInt(KILL_METHOD.length)], SQUAD_MEMBER[random.nextInt(SQUAD_MEMBER.length)]));
        } else {
          logger.warn(format("Zombie %s from %s.%s=%s will be spreading the deadly virus.", entityId,
              zombieType.collection, zombieType.ownerFieldName, ownerId));
        }
      }
    }

    return count;
  }
}
