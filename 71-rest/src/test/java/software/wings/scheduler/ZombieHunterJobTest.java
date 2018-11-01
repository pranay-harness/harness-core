package software.wings.scheduler;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.time.Duration;
import java.time.OffsetDateTime;

public class ZombieHunterJobTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(ZombieHunterJobTest.class);

  @Mock @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject @InjectMocks private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  @Inject @InjectMocks ZombieHunterJob job;

  @Test
  public void huntingExpeditionSchedule() {
    for (int i = 0; i < ZombieHunterJob.zombieTypes.size(); i++) {
      final OffsetDateTime huntingExpedition = ZombieHunterJob.nextHuntingExpedition(i);
      assertThat(huntingExpedition).isAfter(ZombieHunterJob.today);
      assertThat(Duration.between(huntingExpedition, ZombieHunterJob.today)).isLessThan(ZombieHunterJob.cycle);
    }

    for (int i = 1; i < ZombieHunterJob.zombieTypes.size(); i++) {
      final OffsetDateTime huntingExpedition0 = ZombieHunterJob.nextHuntingExpedition(i - 1);
      final OffsetDateTime huntingExpedition1 = ZombieHunterJob.nextHuntingExpedition(i);
      assertThat(huntingExpedition0).isNotEqualTo(huntingExpedition1);
    }
  }

  @Test
  public void exploratoryExpedition() {
    Account account = anAccount().withAppId(GLOBAL_APP_ID).withUuid("exists").build();
    wingsPersistence.save(account);

    Application existApplication = anApplication().withName("dummy1").withAccountId(account.getUuid()).build();
    wingsPersistence.save(existApplication);

    Application application1 = anApplication().withName("dummy1").withAccountId("deleted").build();
    wingsPersistence.save(application1);
    Application application2 = anApplication().withName("dummy2").withAccountId("deleted").build();
    wingsPersistence.save(application2);
    assertThat(
        job.huntingExpedition(new ZombieHunterJob.ZombieType("applications", "accountId", asList("accounts"), null)))
        .isEqualTo(2);

    assertThat(wingsPersistence.delete(application1)).isTrue();
    assertThat(wingsPersistence.delete(application2)).isTrue();
  }

  @Test
  public void huntingExpedition() {
    Account account = anAccount().withAppId(GLOBAL_APP_ID).withUuid("exists").build();
    wingsPersistence.save(account);

    Application existApplication = anApplication().withName("dummy1").withAccountId(account.getUuid()).build();
    wingsPersistence.save(existApplication);

    Application application1 = anApplication().withName("dummy1").withAccountId("deleted").build();
    wingsPersistence.save(application1);
    Application application2 = anApplication().withName("dummy2").withAccountId("deleted").build();
    wingsPersistence.save(application2);
    assertThat(job.huntingExpedition(
                   new ZombieHunterJob.ZombieType("applications", "accountId", asList("accounts"), appService)))
        .isEqualTo(2);

    assertThat(wingsPersistence.delete(existApplication)).isTrue();
    assertThat(wingsPersistence.delete(application1)).isFalse();
    assertThat(wingsPersistence.delete(application2)).isFalse();
  }

  @Test
  public void huntingForOwnersFromMultipleCollections() {
    Account account = anAccount().withAppId(GLOBAL_APP_ID).withUuid("exists").build();
    wingsPersistence.save(account);

    Application existApplication = anApplication().withName("dummy1").withAccountId(account.getUuid()).build();
    wingsPersistence.save(existApplication);

    Application application1 = anApplication().withName("dummy1").withAccountId("deleted").build();
    wingsPersistence.save(application1);
    Application application2 = anApplication().withName("dummy2").withAccountId("deleted").build();
    wingsPersistence.save(application2);
    assertThat(job.huntingExpedition(new ZombieHunterJob.ZombieType(
                   "applications", "accountId", asList("environments", "accounts"), appService)))
        .isEqualTo(2);

    assertThat(wingsPersistence.delete(existApplication)).isTrue();
    assertThat(wingsPersistence.delete(application1)).isFalse();
    assertThat(wingsPersistence.delete(application2)).isFalse();
  }
}
