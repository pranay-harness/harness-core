package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

public class ZombieHunterJobTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(ZombieHunterJobTest.class);

  @Mock @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Inject @InjectMocks private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  @Inject @InjectMocks ZombieHunterJob job;

  @Test
  public void huntingExpeditionSchedule() {
    for (int i = 0; i < ZombieHunterJob.zombieTypes.size(); i++) {
      final long huntingExpedition = ZombieHunterJob.nextHuntingExpedition(i);
      assertThat(huntingExpedition).isGreaterThan(ZombieHunterJob.today.getTimeInMillis());
      assertThat(huntingExpedition - ZombieHunterJob.today.getTimeInMillis())
          .isLessThan(ZombieHunterJob.cycle.toMillis());
    }
  }

  @Test
  public void exploratoryExpedition() {
    Application application1 = anApplication().withName("dummy1").withAccountId("deleted").build();
    wingsPersistence.save(application1);
    Application application2 = anApplication().withName("dummy2").withAccountId("deleted").build();
    wingsPersistence.save(application2);
    assertThat(job.huntingExpedition(new ZombieHunterJob.ZombieType("applications", "accountId", "accounts", null)))
        .isEqualTo(2);

    assertThat(wingsPersistence.delete(application1)).isTrue();
    assertThat(wingsPersistence.delete(application2)).isTrue();
  }

  @Test
  public void huntingExpedition() {
    Application application1 = anApplication().withName("dummy1").withAccountId("deleted").build();
    wingsPersistence.save(application1);
    Application application2 = anApplication().withName("dummy2").withAccountId("deleted").build();
    wingsPersistence.save(application2);
    assertThat(
        job.huntingExpedition(new ZombieHunterJob.ZombieType("applications", "accountId", "accounts", appService)))
        .isEqualTo(2);

    assertThat(wingsPersistence.delete(application1)).isFalse();
    assertThat(wingsPersistence.delete(application2)).isFalse();
  }
}
