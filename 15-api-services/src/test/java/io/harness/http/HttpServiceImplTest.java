package io.harness.http;

import com.google.inject.Inject;
import io.harness.ApiServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;

import static io.harness.rule.OwnerRule.AGORODETKI;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpServiceImplTest extends ApiServiceTestBase {
  @Inject HttpServiceImpl httpService;

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnMethodSpecificUriRequest() {
    List<String> methodsList = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");
    for (String method : methodsList) {
      HttpUriRequest request = httpService.getMethodSpecificHttpRequest(method, "url", "body");
      assertThat(request.getMethod()).isEqualTo(method);
    }
  }
}
