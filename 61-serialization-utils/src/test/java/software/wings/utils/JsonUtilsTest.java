package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.jayway.jsonpath.DocumentContext;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.JsonUtilsTest.Base.BaseType;

import java.util.List;

/**
 * The Class JsonUtilsTest.
 *
 * @author Rishi.
 */
public class JsonUtilsTest {
  private static final String json =
      "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"NigelRees\",\"title\":\"SayingsoftheCentury\","
      + "\"price\":8.95},{\"category\":\"fiction\",\"author\":\"EvelynWaugh\",\"title\":\"SwordofHonour\",\"price\":12.99},{\"category\":\"fiction\","
      + "\"author\":\"HermanMelville\",\"title\":\"MobyDick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\""
      + ",\"author\":\"J.R.R.Tolkien\",\"title\":\"TheLordoftheRings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}]"
      + ",\"bicycle\":{\"color\":\"red\",\"price\":19.95}},\"expensive\":10}";
  private static final Logger logger = LoggerFactory.getLogger(JsonUtilsTest.class);

  /**
   * Should get authors.
   */
  @Test
  public void shouldGetAuthors() {
    List<String> authors = JsonUtils.jsonPath(json, "$.store.book[*].author");
    logger.debug("authors: {}", authors);
    assertThat(authors).isNotNull().hasSize(4);
  }

  /**
   * Should get title and cheap books.
   */
  @Test
  public void shouldGetTitleAndCheapBooks() {
    DocumentContext ctx = JsonUtils.parseJson(json);
    List<String> titles = JsonUtils.jsonPath(ctx, "$.store.book[*].title");
    logger.debug("authors: {}", titles);
    assertThat(titles).isNotNull().hasSize(4);

    List<Object> cheapBooks = JsonUtils.jsonPath(ctx, "$.store.book[?(@.price < 10)]");
    logger.debug("cheapBooks: {}", cheapBooks);
    assertThat(cheapBooks).isNotNull().hasSize(2);
  }

  /**
   * Should return correct object in case of inheritence.
   */
  @Test
  public void shouldReturnCorrectObjectInCaseOfInheritence() {
    BaseA baseA = new BaseA();
    String jsona = JsonUtils.asJson(baseA);

    JsonFluentAssert.assertThatJson(jsona).isEqualTo(
        "{\"baseType\":\"A\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseA\"}");

    BaseB baseB = new BaseB();
    String jsonb = JsonUtils.asJson(baseB);

    JsonFluentAssert.assertThatJson(jsonb).isEqualTo(
        "{\"baseType\":\"B\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseB\"}");

    assertThat(
        JsonUtils.asObject("{\"baseType\":\"A\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseA\"}", Base.class))
        .isInstanceOf(BaseA.class)
        .extracting(Base::getBaseType)
        .containsExactly(BaseType.A);
    assertThat(
        JsonUtils.asObject("{\"baseType\":\"B\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseB\"}", Base.class))
        .isInstanceOf(BaseB.class)
        .extracting(Base::getBaseType)
        .containsExactly(BaseType.B);
  }

  @Test
  public void shouldReturnCorrectObjectInCaseOfInheritenceWithoutInterface() {
    TypeA typeA = new TypeA();
    typeA.setX("A");
    String jsona = JsonUtils.asJson(typeA);

    JsonFluentAssert.assertThatJson(jsona).isEqualTo("{\"eventType\":\"A\", \"x\": \"A\"}");

    TypeB typeB = new TypeB();
    typeB.setX("B");
    String jsonb = JsonUtils.asJson(typeB);

    JsonFluentAssert.assertThatJson(jsonb).isEqualTo("{\"eventType\":\"B\", \"x\": \"B\"}");

    assertThat(JsonUtils.asObject("{\"eventType\":\"A\", \"x\": \"A\"}", TypeA.class))
        .isInstanceOf(TypeA.class)
        .extracting(TypeA::getX)
        .containsExactly("A");
    assertThat(JsonUtils.asObject("{\"eventType\":\"B\", \"x\": \"B\"}", TypeA.class))
        .isInstanceOf(TypeB.class)
        .extracting(TypeA::getX)
        .containsExactly("B");
  }

  @Test
  public void shouldUseClassNameWhenUsingMapperForCloning() {
    BaseA baseA = new BaseA();
    String jsona = JsonUtils.asJson(new Object[] {baseA}, JsonUtils.mapperForCloning);

    JsonFluentAssert.assertThatJson(jsona).isEqualTo(
        "[[\"software.wings.utils.JsonUtilsTest$BaseA\",{\"baseType\":\"A\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseA\"}]]");

    BaseB baseB = new BaseB();
    String jsonb = JsonUtils.asJson(new Object[] {baseB}, JsonUtils.mapperForCloning);

    JsonFluentAssert.assertThatJson(jsonb).isEqualTo(
        "[[\"software.wings.utils.JsonUtilsTest$BaseB\",{\"baseType\":\"B\",\"name\":\"software.wings.utils.JsonUtilsTest$BaseB\"}]]");

    assertThat(JsonUtils.asObject(jsona, new TypeReference<Object[]>() {}, JsonUtils.mapperForCloning))
        .hasSize(1)
        .hasOnlyElementsOfType(BaseA.class)
        .extracting(o -> ((Base) o).getBaseType())
        .containsExactly(BaseType.A);

    assertThat(JsonUtils.asObject(jsonb, new TypeReference<Object[]>() {}, JsonUtils.mapperForCloning))
        .hasSize(1)
        .hasOnlyElementsOfType(BaseB.class)
        .extracting(o -> ((Base) o).getBaseType())
        .containsExactly(BaseType.B);
  }

  /**
   * Should generate json schema.
   */
  @Test
  public void shouldGenerateJsonSchema() {
    JsonFluentAssert.assertThatJson(JsonUtils.jsonSchema(BaseA.class))
        .isEqualTo(
            "{\"type\":\"object\",\"properties\":{\"baseType\":{\"enum\":[\"A\",\"B\",\"C\"],\"type\":\"string\"},\"name\":{\"type\":\"string\"}},"
            + "\"title\":\"BaseA\",\"required\":[\"name\"]}");
  }

  /**
   * The Class Base.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "baseType", include = As.EXISTING_PROPERTY)
  public static class Base {
    private BaseType baseType;

    @SchemaIgnore private String x;

    /**
     * Gets base type.
     *
     * @return the base type
     */
    public BaseType getBaseType() {
      return baseType;
    }

    /**
     * Sets base type.
     *
     * @param baseType the base type
     */
    public void setBaseType(BaseType baseType) {
      this.baseType = baseType;
    }

    /**
     * Gets x.
     *
     * @return the x
     */
    @SchemaIgnore
    public String getX() {
      return x;
    }

    /**
     * Sets x.
     *
     * @param x the x
     */
    @SchemaIgnore
    public void setX(String x) {
      this.x = x;
    }

    /**
     * The Enum BaseType.
     */
    public enum BaseType {
      /**
       * A base type.
       */
      A,
      /**
       * B base type.
       */
      B,
      /**
       * C base type.
       */
      C
    }
  }

  /**
   * The Class BaseA.
   */
  @JsonTypeName("A")
  @Attributes(title = "BaseA")
  public static class BaseA extends Base {
    @Attributes(required = true) private String name = BaseA.class.getName();

    /**
     * Instantiates a new base a.
     */
    public BaseA() {
      setBaseType(BaseType.A);
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }
  }

  /**
   * The Class BaseB.
   */
  @JsonTypeName("B")
  public static class BaseB extends Base {
    private String name = BaseB.class.getName();

    /**
     * Instantiates a new base b.
     */
    public BaseB() {
      setBaseType(BaseType.B);
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }
  }

  /**
   * The Class BaseC.
   */
  @JsonTypeName("C")
  public static class BaseC extends Base {
    private String name = BaseC.class.getName();

    /**
     * Instantiates a new base c.
     */
    public BaseC() {
      setBaseType(BaseType.C);
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", include = As.PROPERTY)
  @JsonTypeName("A")
  public static class TypeA {
    private String x;

    /**
     * Getter for property 'x'.
     *
     * @return Value for property 'x'.
     */
    public String getX() {
      return x;
    }

    /**
     * Setter for property 'x'.
     *
     * @param x Value to set for property 'x'.
     */
    public void setX(String x) {
      this.x = x;
    }
  }

  @JsonTypeName("B")
  public static class TypeB extends TypeA {}
}
