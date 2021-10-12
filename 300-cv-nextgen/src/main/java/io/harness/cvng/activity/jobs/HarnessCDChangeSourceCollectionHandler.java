package io.harness.cvng.activity.jobs;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.beans.change.HarnessCDCurrentGenEventMetadata;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.entities.changeSource.HarnessCDCurrentGenChangeSource;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rest.RestResponse;

import com.google.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
public class HarnessCDChangeSourceCollectionHandler
    implements MongoPersistenceIterator.Handler<HarnessCDCurrentGenChangeSource> {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private ChangeEventService changeEventService;

  @Override
  public void handle(HarnessCDCurrentGenChangeSource changeSource) {
    try {
      Response<RestResponse<List<HarnessCDCurrentGenEventMetadata>>> eventMetadataListResponse =
          verificationManagerClient
              .getCDCurrentGenChangeEvents(changeSource.getAccountId(), changeSource.getHarnessApplicationId(),
                  changeSource.getHarnessServiceId(), changeSource.getHarnessEnvironmentId(),
                  Instant.now().minus(30, ChronoUnit.MINUTES).toEpochMilli())
              .execute();
      eventMetadataListResponse.body().getResource().forEach(event -> {
        ChangeEventDTO changeEventDTO = ChangeEventDTO.builder()
                                            .accountId(changeSource.getAccountId())
                                            .orgIdentifier(changeSource.getOrgIdentifier())
                                            .projectIdentifier(changeSource.getProjectIdentifier())
                                            .changeSourceIdentifier(changeSource.getIdentifier())
                                            .envIdentifier(changeSource.getEnvIdentifier())
                                            .serviceIdentifier(changeSource.getServiceIdentifier())
                                            .type(ChangeSourceType.HARNESS_CD_CURRENT_GEN)
                                            .eventTime(event.getWorkflowStartTime())
                                            .metadata(event)
                                            .build();
        changeEventService.register(changeEventDTO);
      });
    } catch (IOException e) {
      log.error("Exception while getting CD 1.0 events for change source {}", changeSource.getIdentifier(), e);
    }
  }
}
