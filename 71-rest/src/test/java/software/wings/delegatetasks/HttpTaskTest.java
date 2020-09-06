package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.AGORODETKI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameters;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class HttpTaskTest {
  private String method;

  public HttpTaskTest(String method) {
    this.method = method;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"GET"}, {"POST"}, {"PUT"}, {"PATCH"}, {"DELETE"}, {"HEAD"}});
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnMethodSpecificUriRequest() {
    HttpTask task =
        new HttpTask(DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), o -> {}, () -> true);
    HttpUriRequest request = task.getMethodSpecificHttpRequest(method, "url", "body");
    assertThat(request.getMethod()).isEqualTo(method);
  }
}
