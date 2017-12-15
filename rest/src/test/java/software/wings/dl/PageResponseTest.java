package software.wings.dl;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import com.google.common.collect.Lists;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.common.Constants;
import software.wings.utils.JsonUtils;

import javax.inject.Inject;

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
    pageResponse.setTotal(100);
    assertThatJson(jsonUtils.asJson(pageResponse))
        .isEqualTo("{\"start\":0,\"pageSize\":" + PageRequest.DEFAULT_UNLIMITED + ",\"filters\":[],"
            + "\"orders\":[],\"fieldsIncluded\":[],\"fieldsExcluded\":[],\"response\":[],"
            + "\"total\":100,\"empty\":true,\"currentPage\":1,\"or\":false}");
  }
}
