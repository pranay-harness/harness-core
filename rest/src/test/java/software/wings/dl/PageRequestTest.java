package software.wings.dl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.SearchFilter.Operator.ELEMENT_MATCH;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SearchFilter;

import javax.ws.rs.core.MultivaluedHashMap;

public class PageRequestTest extends WingsBaseTest {
  @Test
  public void testElemMatchPageRequest() {
    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    map.put("search[0][field]", asList("foo"));
    map.put("search[0][op]", asList(ELEMENT_MATCH.name()));
    map.put("search[0][value][0][field]", asList("bar"));
    map.put("search[0][value][0][op]", asList(EQ.name()));
    map.put("search[0][value][0][value]", asList("something"));

    final PageRequest pageRequest = aPageRequest().build();

    pageRequest.populateFilters(map, null, null);

    assertThat(pageRequest.getFilters().size()).isEqualTo(1);
    assertThat(((SearchFilter) pageRequest.getFilters().get(0)).getFieldValues()[0]).isInstanceOf(PageRequest.class);
  }

  @Test
  public void testMissingIndexPageRequest() {
    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    map.put("search[1][field]", asList("foo"));
    map.put("search[1][op]", asList(EQ.name()));
    map.put("search[1][value]", asList("bar"));

    final PageRequest pageRequest = aPageRequest().build();

    assertThatThrownBy(() -> pageRequest.populateFilters(map, null, null)).isInstanceOf(InvalidRequestException.class);
  }
}
