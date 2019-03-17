package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeleteCommandTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    DeleteCommand deleteCommand = client.delete().resources("Deployment/nginx ConfigMap/config");

    assertEquals("kubectl delete Deployment/nginx ConfigMap/config", deleteCommand.command());
  }
}
