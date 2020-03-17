package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetJobCommandTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void command() {
    Kubectl client = Kubectl.client("kubectl", "CONFIG_PATH");
    GetJobCommand getJobCommand = new GetJobCommand(new GetCommand(client), "JOB1", "NAMESPACE");
    getJobCommand.output("jsonpath=.status");

    String command = getJobCommand.command();

    assertThat(command).isEqualTo(
        "kubectl --kubeconfig=CONFIG_PATH get jobs JOB1 --namespace=NAMESPACE --output=jsonpath=.status");
  }
}