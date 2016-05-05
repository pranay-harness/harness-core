package software.wings.beans;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import com.google.common.collect.Lists;

import org.junit.Test;
import software.wings.common.JsonUtils;

/**
 * Created by peeyushaggarwal on 4/25/16.
 */
public class PageResponseTest {
  @Test
  public void shouldReturnPageResponseAsAnObject() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(Lists.newArrayList());
    pageResponse.setTotal(100);
    assertThatJson(JsonUtils.asJson(pageResponse))
        .isEqualTo("{\"offset\":null,\"start\":0,\"limit\":null,\"pageSize\":50,\"filters\":[],"
            + "\"orders\":[],\"fieldsIncluded\":[],\"fieldsExcluded\":[],\"response\":[],"
            + "\"total\":100,\"empty\":true,\"currentPage\":1,\"or\":false}");
  }
}
