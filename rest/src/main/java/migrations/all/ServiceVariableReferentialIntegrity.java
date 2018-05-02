package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;

import java.util.List;

/**
 * Created by brett on 5/1/18.
 */
public class ServiceVariableReferentialIntegrity implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(ServiceVariableReferentialIntegrity.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Checking service variables for invalid parent references");
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }

    UpdateOperations<ServiceVariable> updateOperations =
        wingsPersistence.createUpdateOperations(ServiceVariable.class).unset("parentServiceVariableId");

    logger.info("Checking {} applications", apps.size());
    for (Application app : apps) {
      List<ServiceVariable> refVariables = wingsPersistence.createQuery(ServiceVariable.class)
                                               .filter(APP_ID_KEY, app.getUuid())
                                               .field("parentServiceVariableId")
                                               .exists()
                                               .asList();
      logger.info("Checking {} variables in {}", refVariables.size(), app.getName());
      for (ServiceVariable var : refVariables) {
        String parentId = var.getParentServiceVariableId();
        ServiceVariable parent = wingsPersistence.createQuery(ServiceVariable.class)
                                     .filter(APP_ID_KEY, app.getUuid())
                                     .filter(ID_KEY, parentId)
                                     .get();
        if (parent == null) {
          logger.info("Clearing invalid parent reference in {}", var.getName());
          wingsPersistence.update(var, updateOperations);
        }
      }
    }
  }
}
