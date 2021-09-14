/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.dl;

import static io.harness.rule.OwnerRule.UNKNOWN;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import software.wings.WingsBaseTest;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by peeyushaggarwal on 4/25/16.
 */
public class PageResponseTest extends WingsBaseTest {
  /**
   * Should return page response as an object.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldReturnPageResponseAsAnObject() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(Lists.newArrayList());
    pageResponse.setTotal(100l);
    assertThatJson(JsonUtils.asJson(pageResponse))
        .isEqualTo("{\"start\":0,\"pageSize\":" + PageRequest.DEFAULT_UNLIMITED + ",\"filters\":[],"
            + "\"orders\":[],\"fieldsIncluded\":[],\"fieldsExcluded\":[],\"response\":[],"
            + "\"total\":100,\"empty\":true,\"currentPage\":1,\"or\":false}");
  }
}
