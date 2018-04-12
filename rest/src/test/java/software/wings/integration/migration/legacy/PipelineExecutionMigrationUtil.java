package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Test;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.PipelineExecution;
import software.wings.beans.SearchFilter;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

/**
 * Created by sgurubelli on 9/18/17.
 */
@Integration
public class PipelineExecutionMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(PipelineExecutionMigrationUtil.class);

  @Inject private WingsPersistence wingsPersistence;

  /***
   * Updates pipeline id of Pipeline Execution
   */
  @Test
  public void updatePipelineExecution() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      logger.info("No applications found");
      return;
    }
    pageResponse.getResponse().forEach(application -> {
      logger.info("Updating " + application);
      PageRequest<PipelineExecution> pipelineRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("appId", SearchFilter.Operator.EQ, application.getAppId())
              .build();
      PageResponse<PipelineExecution> pipelineExecutions =
          wingsPersistence.query(PipelineExecution.class, pipelineRequest);
      if (pipelineExecutions == null) {
        logger.info("No pipeline executions found for Application" + application);
      }
      pipelineExecutions.forEach(pipelineExecution -> {
        UpdateOperations<PipelineExecution> ops = wingsPersistence.createUpdateOperations(PipelineExecution.class);
        logger.info("Updating pipeline execution  = " + pipelineExecution);
        setUnset(ops, "pipeline._id", generateUuid() + "_embedded");
        wingsPersistence.update(wingsPersistence.createQuery(PipelineExecution.class)
                                    .filter("appId", application.getAppId())
                                    .filter(ID_KEY, pipelineExecution.getUuid()),
            ops);
      });
    });
  }
}
