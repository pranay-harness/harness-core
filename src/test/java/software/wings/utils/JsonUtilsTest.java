/**
 *
 */
package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseUnitTest;

import java.util.List;

import javax.inject.Inject;

/**
 * @author Rishi
 *
 */
public class JsonUtilsTest extends WingsBaseUnitTest {
  @Inject private JsonUtils jsonUtils;

  private static final String json =
      "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"NigelRees\",\"title\":\"SayingsoftheCentury\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"EvelynWaugh\",\"title\":\"SwordofHonour\",\"price\":12.99},{\"category\":\"fiction\",\"author\":\"HermanMelville\",\"title\":\"MobyDick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J.R.R.Tolkien\",\"title\":\"TheLordoftheRings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}],\"bicycle\":{\"color\":\"red\",\"price\":19.95}},\"expensive\":10}";

  @Test
  public void shouldGetAuthors() {
    List<String> authors = jsonUtils.jsonPath(json, "$.store.book[*].author");
    logger.debug("authors: {}", authors);
    assertThat(authors).isNotNull();
    assertThat(authors.size()).isEqualTo(4);
  }

  @Test
  public void shouldGetTitleAndCheapBooks() {
    DocumentContext ctx = jsonUtils.parseJson(json);
    List<String> titles = jsonUtils.jsonPath(ctx, "$.store.book[*].title");
    logger.debug("authors: {}", titles);
    assertThat(titles).isNotNull();
    assertThat(titles.size()).isEqualTo(4);

    List<Object> cheapBooks = jsonUtils.jsonPath(ctx, "$.store.book[?(@.price < 10)]");
    logger.debug("cheapBooks: {}", cheapBooks);
    assertThat(cheapBooks).isNotNull();
    assertThat(cheapBooks.size()).isEqualTo(2);
  }
  private final Logger logger = LoggerFactory.getLogger(getClass());
}
