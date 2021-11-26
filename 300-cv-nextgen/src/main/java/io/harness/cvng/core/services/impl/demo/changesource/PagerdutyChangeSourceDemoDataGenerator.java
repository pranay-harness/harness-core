package io.harness.cvng.core.services.impl.demo.changesource;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.beans.change.PagerDutyEventMetaData;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;
import io.harness.cvng.core.services.api.demo.ChangeSourceDemoDataGenerator;
import io.harness.cvng.core.utils.DateTimeUtils;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class PagerdutyChangeSourceDemoDataGenerator implements ChangeSourceDemoDataGenerator<PagerDutyChangeSource> {
  @Inject private Clock clock;
  @Override
  public List<ChangeEventDTO> generate(PagerDutyChangeSource changeSource) {
    Instant time = DateTimeUtils.roundDownTo1MinBoundary(clock.instant());
    return Arrays.asList(ChangeEventDTO.builder()
                             .accountId(changeSource.getAccountId())
                             .changeSourceIdentifier(changeSource.getIdentifier())
                             .projectIdentifier(changeSource.getProjectIdentifier())
                             .orgIdentifier(changeSource.getOrgIdentifier())
                             .serviceIdentifier(changeSource.getServiceIdentifier())
                             .envIdentifier(changeSource.getEnvIdentifier())
                             .eventTime(time.toEpochMilli())
                             .type(ChangeSourceType.PAGER_DUTY)
                             .metadata(PagerDutyEventMetaData.builder().build()) // TODO
                             .build());
  }
}
