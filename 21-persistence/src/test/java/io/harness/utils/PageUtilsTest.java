package io.harness.utils;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetPageRequest_When_NoSortDirectionProvided() {
    List<String> sortList = new ArrayList<>();
    sortList.add("name");
    Pageable pageable = PageUtils.getPageRequest(1, sortList.size(), sortList);
    assertThat(pageable).isNotNull();
    assertThat(pageable.getPageSize()).isEqualTo(sortList.size());
    assertThat(pageable.getSort()).isNotNull();
    assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
    assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetPageRequest_When_SortDirectionProvided() {
    List<String> sortList = new ArrayList<>();
    sortList.add("name,desc");
    Pageable pageable = PageUtils.getPageRequest(1, sortList.size(), sortList);
    assertThat(pageable).isNotNull();
    assertThat(pageable.getPageSize()).isEqualTo(sortList.size());
    assertThat(pageable.getSort()).isNotNull();
    assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
    assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test(expected = IllegalArgumentException.class)
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetPageRequest_When_InvalidSortDirectionProvided() {
    List<String> sortList = new ArrayList<>();
    sortList.add("name,des");
    PageUtils.getPageRequest(1, sortList.size(), sortList);
  }
}
