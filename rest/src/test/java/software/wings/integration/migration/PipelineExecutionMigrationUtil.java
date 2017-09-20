package software.wings.integration.migration;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.PipelineExecution;
import software.wings.beans.SearchFilter;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import javax.inject.Inject;

/**
 * Created by sgurubelli on 9/18/17.
 */
@Integration
public class PipelineExecutionMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  /***
   * Updates pipeline id of Pipeline Execution
   */
  @Test
  public void updatePipelineExecution() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    if (pageResponse.isEmpty() || CollectionUtils.isEmpty(pageResponse.getResponse())) {
      System.out.println("No applications found");
      return;
    }
    pageResponse.getResponse().forEach(application -> {
      System.out.println("Updating " + application);
      PageRequest<PipelineExecution> pipelineRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("appId", SearchFilter.Operator.EQ, application.getAppId())
              .build();
      PageResponse<PipelineExecution> pipelineExecutions =
          wingsPersistence.query(PipelineExecution.class, pipelineRequest);
      if (pipelineExecutions == null) {
        System.out.println("No pipeline executions found for Application" + application);
      }
      pipelineExecutions.forEach(pipelineExecution -> {
        UpdateOperations<PipelineExecution> ops = wingsPersistence.createUpdateOperations(PipelineExecution.class);
        System.out.println("Updating pipeline execution  = " + pipelineExecution);
        setUnset(ops, "pipeline._id", UUIDGenerator.getUuid() + "_embedded");
        wingsPersistence.update(wingsPersistence.createQuery(PipelineExecution.class)
                                    .field("appId")
                                    .equal(application.getAppId())
                                    .field(ID_KEY)
                                    .equal(pipelineExecution.getUuid()),
            ops);

      });
    });
  }
}
