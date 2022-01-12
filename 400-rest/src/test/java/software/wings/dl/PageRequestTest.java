/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.dl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.ELEMENT_MATCH;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PageRequestTest extends WingsBaseTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testMissingIndexPageRequest() {
    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    map.put("search[1][field]", asList("foo"));
    map.put("search[1][op]", asList(EQ.name()));
    map.put("search[1][value]", asList("bar"));

    final PageRequest pageRequest = aPageRequest().build();

    assertThatThrownBy(() -> pageRequest.populateFilters(map, null, null)).isInstanceOf(InvalidRequestException.class);
  }
}
