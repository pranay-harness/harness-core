package software.wings.utils;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.Attributes;
import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.states.EmailState;
import software.wings.utils.JsonUtilsTest.Base.BaseType;

import java.util.List;

/**
 * @author Rishi.
 */
public class JsonUtilsTest {
  private static final String json =
      "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"NigelRees\",\"title\":\"SayingsoftheCentury\","
      + "\"price\":8.95},{\"category\":\"fiction\",\"author\":\"EvelynWaugh\",\"title\":\"SwordofHonour\",\"price\":12.99},{\"category\":\"fiction\","
      + "\"author\":\"HermanMelville\",\"title\":\"MobyDick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\""
      + ",\"author\":\"J.R.R.Tolkien\",\"title\":\"TheLordoftheRings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}]"
      + ",\"bicycle\":{\"color\":\"red\",\"price\":19.95}},\"expensive\":10}";
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  public void shouldGetAuthors() {
    List<String> authors = JsonUtils.jsonPath(json, "$.store.book[*].author");
    logger.debug("authors: {}", authors);
    assertThat(authors).isNotNull();
    assertThat(authors.size()).isEqualTo(4);
  }

  @Test
  public void shouldGetTitleAndCheapBooks() {
    DocumentContext ctx = JsonUtils.parseJson(json);
    List<String> titles = JsonUtils.jsonPath(ctx, "$.store.book[*].title");
    logger.debug("authors: {}", titles);
    assertThat(titles).isNotNull();
    assertThat(titles.size()).isEqualTo(4);

    List<Object> cheapBooks = JsonUtils.jsonPath(ctx, "$.store.book[?(@.price < 10)]");
    logger.debug("cheapBooks: {}", cheapBooks);
    assertThat(cheapBooks).isNotNull();
    assertThat(cheapBooks.size()).isEqualTo(2);
  }

  @Test
  public void shouldReturnCorrectObjectInCaseOfInheritence() {
    BaseA baseA = new BaseA();
    String jsona = JsonUtils.asJson(baseA);

    assertThatJson(jsona).isEqualTo(
        "{\"baseType\":\"A\",\"baseType\":\"A\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseA\"}");

    BaseB baseB = new BaseB();
    String jsonb = JsonUtils.asJson(baseB);

    assertThatJson(jsonb).isEqualTo(
        "{\"baseType\":\"B\",\"baseType\":\"B\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseB\"}");

    assertThat(JsonUtils.asObject(jsona, Base.class))
        .isInstanceOf(BaseA.class)
        .extracting(Base::getBaseType)
        .containsExactly(BaseType.A);
    assertThat(JsonUtils.asObject(jsonb, Base.class))
        .isInstanceOf(BaseB.class)
        .extracting(Base::getBaseType)
        .containsExactly(BaseType.B);
  }

  @Test
  public void shouldGenerateJsonSchema() {
    System.out.println(JsonUtils.jsonSchema(EmailState.class));
    // assertThatJson(JsonUtils.jsonSchema(BaseA.class)).isEqualTo("{\n" + "  \"type\" : \"object\",\n" + "
    // \"properties\" : {\n" + "    \"baseType\" : {\n"
    //    + "      \"enum\" : [ \"A\", \"B\", \"C\" ]\n" + "    },\n" + "    \"name\" : {\n" + "      \"type\" :
    //    \"string\"\n" + "    }\n" + "  },\n"
    //    + "  \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\n" + "  \"title\" : \"BaseA\",\n" + "
    //    \"required\" : [ \"name\" ]\n" + "}");
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "baseType")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = BaseA.class, name = "A")
    , @JsonSubTypes.Type(value = BaseB.class, name = "B"), @JsonSubTypes.Type(value = BaseC.class, name = "C")
  })
  public static class Base {
    private BaseType baseType;

    public BaseType getBaseType() {
      return baseType;
    }

    public void setBaseType(BaseType baseType) {
      this.baseType = baseType;
    }

    public enum BaseType { A, B, C }
  }

  @Attributes(title = "BaseA")
  public static class BaseA extends Base {
    @Attributes(required = true) private String name = BaseA.class.getName();

    public BaseA() {
      super();
      setBaseType(BaseType.A);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class BaseB extends Base {
    private String name = BaseB.class.getName();

    public BaseB() {
      super();
      setBaseType(BaseType.B);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class BaseC extends Base {
    private String name = BaseC.class.getName();

    public BaseC() {
      super();
      setBaseType(BaseType.C);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
