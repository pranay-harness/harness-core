package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.CatalogItem;
import software.wings.beans.CatalogNames;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author Rishi.
 */
public class YamlUtilsTest extends WingsBaseTest {
  @Inject private YamlUtils yamlUtils;

  @Test
  public void shouldReadCatalogs() throws IOException {
    URL url = this.getClass().getResource("/configs/catalogs.yml");
    String yaml = Resources.toString(url, Charsets.UTF_8);

    Map<String, List<CatalogItem>> catalogs =
        yamlUtils.read(yaml, new TypeReference<Map<String, List<CatalogItem>>>() {});

    assertThat(catalogs).isNotNull();

    List<CatalogItem> cardViewSortBy = catalogs.get("CARD_VIEW_SORT_BY");
    assertThat(cardViewSortBy).isNotNull();
    assertThat(cardViewSortBy.size()).isEqualTo(3);
  }
}
