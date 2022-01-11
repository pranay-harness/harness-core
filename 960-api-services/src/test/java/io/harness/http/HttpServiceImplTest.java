/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.http;

import static io.harness.rule.OwnerRule.AGORODETKI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.ApiServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
