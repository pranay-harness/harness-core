package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.parseLatestRevisionNumberFromRolloutHistory;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class UtilsTest {
  @Test
  public void latestRevisionTest() {
    String rolloutHistory = "deployments \"demo1-nginx-deployment\"\n"
        + "REVISION  CHANGE-CAUSE\n"
        + "2         kubectl.exe apply --kubeconfig=.kubeconfig --filename=manifests.yaml --record=true --output=yaml\n"
        + "3         kubectl edit deploy/demo1-nginx-deployment\n"
        + "4         kubectl edit deploy/demo1-nginx-deployment\n"
        + "\n";

    assertEquals("4", parseLatestRevisionNumberFromRolloutHistory(rolloutHistory));

    rolloutHistory = "daemonsets \"datadog-agent\"\n"
        + "REVISION  CHANGE-CAUSE\n"
        + "2         <none>\n"
        + "3         <none>\n"
        + "\n";

    assertEquals("3", parseLatestRevisionNumberFromRolloutHistory(rolloutHistory));
  }

  @Test
  public void encloseWithQuotesIfNeededTest() {
    assertEquals("kubectl", encloseWithQuotesIfNeeded("kubectl"));
    assertEquals("kubectl", encloseWithQuotesIfNeeded("kubectl "));
    assertEquals("config", encloseWithQuotesIfNeeded("config"));
    assertEquals("\"C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe\"",
        encloseWithQuotesIfNeeded("C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe"));
  }
}
