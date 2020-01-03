package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;

public class SearchEntityIndexStateTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldBulkSyncTest() {
    SearchEntityIndexState searchEntityIndexState =
        new SearchEntityIndexState(ApplicationSearchEntity.class.getCanonicalName(), "0.05", "indexName", false);

    boolean shouldBulkSync = searchEntityIndexState.shouldBulkSync();
    assertThat(shouldBulkSync).isTrue();

    searchEntityIndexState =
        new SearchEntityIndexState(ApplicationSearchEntity.class.getCanonicalName(), "0.1", "indexName", false);
    shouldBulkSync = searchEntityIndexState.shouldBulkSync();
    assertThat(shouldBulkSync).isFalse();

    searchEntityIndexState =
        new SearchEntityIndexState(ApplicationSearchEntity.class.getCanonicalName(), "0.1", "indexName", true);
    shouldBulkSync = searchEntityIndexState.shouldBulkSync();
    assertThat(shouldBulkSync).isTrue();
  }
}
