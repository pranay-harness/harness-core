/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
