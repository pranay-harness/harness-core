package software.wings.integration.dl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.ELEMENT_MATCH;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.GE;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.annotations.Entity;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;

import java.util.List;

public class PageRequestTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Value
  @Builder
  private static class DummyItem {
    int i;
    String s;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  @Entity(value = "!!!testDummies", noClassnameStored = true)
  @HarnessEntity(exportable = false)
  public static class Dummy extends Base {
    private List<DummyItem> dummies;
    private String name;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldRequestElemMatch() {
    {
      final Dummy dummy = new Dummy();
      dummy.dummies = asList(DummyItem.builder().i(1).s("foo").build(), DummyItem.builder().i(2).s("foo").build());
      wingsPersistence.save(dummy);
    }

    {
      final Dummy dummy = new Dummy();
      dummy.dummies = asList(DummyItem.builder().i(2).s("foo").build(), DummyItem.builder().i(1).s("bar").build());
      wingsPersistence.save(dummy);
    }

    {
      final PageRequest<Dummy> itemPageRequest = aPageRequest().addFilter("i", EQ, 1).addFilter("s", EQ, "foo").build();
      final PageRequest<Dummy> pageRequest =
          aPageRequest().addFilter("dummies", ELEMENT_MATCH, itemPageRequest).build();
      PageResponse<Dummy> response = wingsPersistence.query(Dummy.class, pageRequest, excludeAuthority);
      assertThat(response.size()).isEqualTo(1);
    }

    {
      final PageRequest<Dummy> itemPageRequest = aPageRequest().addFilter("i", GE, 1).addFilter("s", EQ, "foo").build();
      final PageRequest<Dummy> pageRequest =
          aPageRequest().addFilter("dummies", ELEMENT_MATCH, itemPageRequest).build();
      PageResponse<Dummy> response = wingsPersistence.query(Dummy.class, pageRequest, excludeAuthority);
      assertThat(response.size()).isEqualTo(2);
    }
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldRequestNestedElemMatchQueries() {
    Dummy dummy1 =
        Dummy.builder()
            .dummies(asList(DummyItem.builder().i(1).s("i11").build(), DummyItem.builder().i(2).s("i12").build()))
            .name("dummy1")
            .build();
    Dummy dummy2 =
        Dummy.builder()
            .dummies(asList(DummyItem.builder().i(1).s("i21").build(), DummyItem.builder().i(2).s("i22").build()))
            .name("dummy2")
            .build();
    Dummy dummy3 =
        Dummy.builder()
            .dummies(asList(DummyItem.builder().i(1).s("i31").build(), DummyItem.builder().i(2).s("i32").build()))
            .name("dummy3")
            .build();
    List<String> strings = wingsPersistence.save(asList(dummy1, dummy2, dummy3));

    SearchFilter searchFilterForDummy1 =
        SearchFilter.builder()
            .fieldName("dummies")
            .op(Operator.ELEMENT_MATCH)
            .fieldValues(new Object[] {
                PageRequestBuilder.aPageRequest()
                    .addFilter(SearchFilter.builder().fieldName("i").op(IN).fieldValues(new Integer[] {1}).build())
                    .addFilter(SearchFilter.builder().fieldName("s").op(IN).fieldValues(new String[] {"i11"}).build())
                    .build()})
            .build();
    SearchFilter searchFilterForDummy3 =
        SearchFilter.builder().fieldName("name").op(IN).fieldValues(new String[] {"dummy3"}).build();

    PageRequest<Dummy> pageRequestForDummy1And3 =
        aPageRequest()
            .addFilter(SearchFilter.builder()
                           .fieldName("query")
                           .op(Operator.OR)
                           .fieldValues(new Object[] {searchFilterForDummy1, searchFilterForDummy3})
                           .build())
            .build();

    PageResponse<Dummy> pageResponse = wingsPersistence.query(Dummy.class, pageRequestForDummy1And3, excludeAuthority);
    assertThat(pageResponse.getResponse()).containsExactlyInAnyOrder(dummy1, dummy3);
  }
}
