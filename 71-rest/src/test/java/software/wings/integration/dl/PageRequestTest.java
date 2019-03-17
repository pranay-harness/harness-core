package software.wings.integration.dl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.ELEMENT_MATCH;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.GE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import lombok.Builder;
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

  @Entity(value = "!!!testDummies", noClassnameStored = true)
  public static class Dummy extends Base {
    public List<DummyItem> dummies;
  }

  @Test
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
}
