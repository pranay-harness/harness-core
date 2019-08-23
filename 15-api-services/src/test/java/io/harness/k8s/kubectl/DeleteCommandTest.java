package io.harness.k8s.kubectl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeleteCommandTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    DeleteCommand deleteCommand = client.delete().resources("Deployment/nginx ConfigMap/config");

    assertThat(deleteCommand.command()).isEqualTo("kubectl delete Deployment/nginx ConfigMap/config");
  }
}
