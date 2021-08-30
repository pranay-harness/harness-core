package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.ABHIJITH;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.ChangeEventDashboardDTO;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.types.ChangeCategory;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ChangeEventServiceImplTest extends CvNextGenTestBase {
  @Inject ChangeEventService changeEventService;

  @Inject ChangeSourceService changeSourceService;

  @Inject HPersistence hPersistence;

  BuilderFactory builderFactory;

  @Before
  public void before() {
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void register_insert() {
    changeSourceService.create(builderFactory.getContext().getServiceEnvironmentParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();

    changeEventService.register(builderFactory.getContext().getAccountId(), changeEventDTO);

    ChangeEvent changeEventFromDb = hPersistence.createQuery(ChangeEvent.class).get();
    Assertions.assertThat(changeEventFromDb).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void register_update() {
    changeSourceService.create(builderFactory.getContext().getServiceEnvironmentParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));

    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    changeEventService.register(builderFactory.getContext().getAccountId(), changeEventDTO);
    Long eventTime = 123L;
    ChangeEventDTO changeEventDTO2 = builderFactory.getHarnessCDChangeEventDTOBuilder().eventTime(eventTime).build();
    changeEventService.register(builderFactory.getContext().getAccountId(), changeEventDTO2);

    Long count = hPersistence.createQuery(ChangeEvent.class).count();
    Assertions.assertThat(hPersistence.createQuery(ChangeEvent.class).count()).isEqualTo(1);
    ChangeEvent changeEventFromDb = hPersistence.createQuery(ChangeEvent.class).get();
    Assertions.assertThat(changeEventFromDb.getEventTime()).isEqualTo(eventTime);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void register_noChangeSource() {
    changeSourceService.create(builderFactory.getContext().getServiceEnvironmentParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();

    changeEventService.register(builderFactory.getContext().getAccountId(), changeEventDTO);

    ChangeEvent changeEventFromDb = hPersistence.createQuery(ChangeEvent.class).get();
    Assertions.assertThat(changeEventFromDb).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void get_withoutCategory() {
    ChangeEvent harnessCDChangeEvent = builderFactory.getHarnessCDChangeEventBuilder().build();
    hPersistence.save(harnessCDChangeEvent);

    List<ChangeEventDTO> changeEventDTOS =
        changeEventService.get(builderFactory.getContext().getServiceEnvironmentParams(),
            Instant.EPOCH.getEpochSecond() - 10000, Instant.EPOCH.getEpochSecond() + 10000, null);
    Assertions.assertThat(changeEventDTOS.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void get_withCategory() {
    ChangeEvent harnessCDChangeEvent = builderFactory.getHarnessCDChangeEventBuilder().build();
    hPersistence.save(harnessCDChangeEvent);

    List<ChangeEventDTO> changeEventDTOS = changeEventService.get(
        builderFactory.getContext().getServiceEnvironmentParams(), Instant.EPOCH.getEpochSecond() - 10000,
        Instant.EPOCH.getEpochSecond() + 10000, Arrays.asList(ChangeCategory.DEPLOYMENT));
    Assertions.assertThat(changeEventDTOS.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void getDashboard() {
    ChangeEvent harnessCDChangeEvent = builderFactory.getHarnessCDChangeEventBuilder().build();
    hPersistence.save(harnessCDChangeEvent);
    harnessCDChangeEvent =
        builderFactory.getHarnessCDChangeEventBuilder().eventTime(Instant.EPOCH.getEpochSecond() - 15000).build();
    hPersistence.save(harnessCDChangeEvent);

    ChangeEventDashboardDTO changeEventDashboardDTO =
        changeEventService.getChangeEventDashboard(builderFactory.getContext().getServiceEnvironmentParams(),
            Instant.EPOCH.getEpochSecond() - 10000, Instant.EPOCH.getEpochSecond() + 10000);
    Assertions.assertThat(changeEventDashboardDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount())
        .isEqualTo(1);
    Assertions
        .assertThat(
            changeEventDashboardDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
  }
}