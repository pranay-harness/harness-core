package software.wings.integration;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.List;

/**
 * Migration script to change the instance lastWorkflowExecutionName from Workflow: XXXX to XXXXX
 * @author rktummala on 09/12/17.
 */
@Integration
@Ignore
public class InstanceWorkflowNameMigration extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void createAwsCloudProvidersFromEcrArtifactServers() {
    System.out.println("Creating new AWS cloud providers based on the ECR Artifact Servers");
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    List<Instance> instanceList = query.field("lastWorkflowExecutionName")
                                      .startsWith("Workflow: ")
                                      .project("lastWorkflowExecutionName", true)
                                      .asList();
    for (Instance instance : instanceList) {
      wingsPersistence.updateField(Instance.class, instance.getUuid(), "lastWorkflowExecutionName",
          instance.getLastWorkflowExecutionName().substring(10));
    }
    System.out.println("Changing instance workflow names completed");
  }
}
