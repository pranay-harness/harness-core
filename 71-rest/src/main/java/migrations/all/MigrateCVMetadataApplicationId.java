package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SortOrder.OrderType;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;

import java.util.List;

@Slf4j
public class MigrateCVMetadataApplicationId implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    PageRequest<ContinuousVerificationExecutionMetaData> cvMetadataRequest = PageRequestBuilder.aPageRequest()
                                                                                 .withLimit("999")
                                                                                 .withOffset("0")
                                                                                 .addOrder("createdAt", OrderType.DESC)
                                                                                 .build();
    PageResponse<ContinuousVerificationExecutionMetaData> cvMetadataResponse =
        wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, cvMetadataRequest);
    int previousOffset = 0;
    while (!cvMetadataResponse.isEmpty()) {
      List<ContinuousVerificationExecutionMetaData> cvList = cvMetadataResponse.getResponse();
      for (ContinuousVerificationExecutionMetaData cvMetadata : cvList) {
        if (isEmpty(cvMetadata.getAppId())) {
          cvMetadata.setAppId(cvMetadata.getApplicationId());
          UpdateOperations<ContinuousVerificationExecutionMetaData> op =
              wingsPersistence.createUpdateOperations(ContinuousVerificationExecutionMetaData.class);
          setUnset(op, "appId", cvMetadata.getApplicationId());
          wingsPersistence.update(wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
                                      .filter("stateExecutionId", cvMetadata.getStateExecutionId()),
              op);
        }
      }
      logger.info("Updated appId for {} CVExecutionMetadata records", cvList.size());
      previousOffset += cvList.size();
      cvMetadataRequest.setOffset(String.valueOf(previousOffset));
      cvMetadataResponse = wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, cvMetadataRequest);
    }
  }
}
