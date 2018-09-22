package software.wings.dl;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.utils.JsonUtils;

/**
 * Created by peeyushaggarwal on 4/25/16.
 */
public class PageResponseTest extends WingsBaseTest {
  @Inject private JsonUtils jsonUtils;

  /**
   * Should return page response as an object.
   */
  @Test
  public void shouldReturnPageResponseAsAnObject() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(Lists.newArrayList());
    pageResponse.setTotal(100l);
    assertThatJson(jsonUtils.asJson(pageResponse))
        .isEqualTo("{\"start\":0,\"pageSize\":" + PageRequest.DEFAULT_UNLIMITED + ",\"filters\":[],"
            + "\"orders\":[],\"fieldsIncluded\":[],\"fieldsExcluded\":[],\"response\":[],"
            + "\"total\":100,\"empty\":true,\"currentPage\":1,\"or\":false}");
  }
}
