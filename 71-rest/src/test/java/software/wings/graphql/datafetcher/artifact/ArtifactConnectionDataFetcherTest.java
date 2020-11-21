package software.wings.graphql.datafetcher.artifact;

import static io.harness.rule.OwnerRule.MILAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLArtifactConnection;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.Query;

public class ArtifactConnectionDataFetcherTest {
  private DataFetcherUtils utils = Mockito.mock(DataFetcherUtils.class);

  @InjectMocks private ArtifactConnectionDataFetcher dataFetcher;

  @Before
  public void setUp() {
    dataFetcher = Mockito.spy(new ArtifactConnectionDataFetcher());
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void testDataFetcher() {
    QLPageInfo page = QLPageInfo.builder().offset(0).hasMore(true).limit(10).total(20).build();
    Query query = Mockito.mock(Query.class);

    Mockito.doReturn(query)
        .when(dataFetcher)
        .populateFilters(
            Matchers.any(WingsPersistence.class), Matchers.anyList(), Matchers.any(Class.class), Matchers.anyBoolean());
    when(utils.populate(Matchers.any(QLPageQueryParameters.class), Matchers.any(Query.class),
             Matchers.any(DataFetcherUtils.Controller.class)))
        .thenReturn(page);

    QLArtifactConnection connection = dataFetcher.fetchConnection(
        Matchers.anyList(), Matchers.any(QLPageQueryParameters.class), (List<QLNoOpSortCriteria>) Matchers.isNull());

    Mockito.verify(dataFetcher, times(1))
        .populateFilters(
            Matchers.any(WingsPersistence.class), Matchers.anyList(), Matchers.any(Class.class), Matchers.anyBoolean());
    Mockito.verify(utils, times(1))
        .populate(Matchers.any(QLPageQueryParameters.class), Matchers.any(Query.class),
            Matchers.any(DataFetcherUtils.Controller.class));
    assertThat(page.getLimit().intValue()).isEqualTo(10);
    assertThat(page.getTotal().intValue()).isEqualTo(20);
    assertThat(page.getOffset().intValue()).isEqualTo(0);
    assertThat(page.getHasMore()).isTrue();
  }
}
