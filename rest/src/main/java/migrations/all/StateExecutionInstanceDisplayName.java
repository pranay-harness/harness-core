package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.sm.StateExecutionInstance;

public class StateExecutionInstanceDisplayName implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(StateExecutionInstanceDisplayName.class);

  @Inject private AppService appService;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    PageResponse<Application> pageResponse = appService.list(aPageRequest().build());
    if (isEmpty(pageResponse.getResponse())) {
      logger.info("Application list is empty.. skipping migration");
    }
    for (Application app : pageResponse.getResponse()) {
      logger.info("Starting migration for Application - id: {}, name: {}", app.getUuid(), app.getName());

      MorphiaIterator<StateExecutionInstance, StateExecutionInstance> iterator =
          wingsPersistence.createQuery(StateExecutionInstance.class)
              .filter("appId", app.getUuid())
              .project("uuid", true)
              .project("stateName", true)
              .project("displayName", true)
              .project("executionUuid", true)
              .project("appId", true)
              .fetch();

      while (iterator.hasNext()) {
        StateExecutionInstance stateExecutionInstance = iterator.next();
        if (stateExecutionInstance.getDisplayName() != null || stateExecutionInstance.getStateName() == null) {
          continue;
        }
        wingsPersistence.update(wingsPersistence.createQuery(StateExecutionInstance.class)
                                    .filter(ID_KEY, stateExecutionInstance.getUuid())
                                    .filter("appId", stateExecutionInstance.getAppId())
                                    .filter("executionUuid", stateExecutionInstance.getExecutionUuid()),
            wingsPersistence.createUpdateOperations(StateExecutionInstance.class)
                .set("displayName", stateExecutionInstance.getStateName()));
      }

      logger.info("Done with migration for Application - id: {}, name: {}", app.getUuid(), app.getName());
    }
  }
}
