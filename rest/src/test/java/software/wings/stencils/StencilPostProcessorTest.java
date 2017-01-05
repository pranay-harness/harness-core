package software.wings.stencils;

import static freemarker.template.utility.Collections12.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.utils.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/28/16.
 */
public class StencilPostProcessorTest extends WingsBaseTest {
  @Mock private Injector injector;

  @InjectMocks @Inject private StencilPostProcessor stencilPostProcessor;

  /**
   * Sets up mocks.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUpMocks() throws Exception {
    when(injector.getInstance(TestDataProvider.class)).thenReturn(new TestDataProvider());
  }

  /**
   * Should expand stencil on post process.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldExpandStencilOnPostProcess() throws Exception {
    List<Stencil> processedStencils =
        stencilPostProcessor.postProcess(singletonList(new StencilType(ExpandStencilObject.class)), APP_ID);

    assertThat(processedStencils)
        .hasSize(2)
        .extracting(Stencil::getName, Stencil::getJsonSchema, Stencil::getType)
        .contains(
            tuple("Value1",
                JsonUtils.readTree(
                    "{\"type\":\"object\",\"properties\":{\"expand\":{\"type\":\"string\",\"enum\":[\"Name1\",\"Name2\"],\"enumNames\":[\"Value1\",\"Value2\"],"
                    + "\"default\":\"Name1\"}}}"),
                "TYPE"),
            tuple("Value2",
                JsonUtils.readTree(
                    "{\"type\":\"object\",\"properties\":{\"expand\":{\"type\":\"string\",\"enum\":[\"Name1\",\"Name2\"],\"enumNames\":[\"Value1\",\"Value2\"],"
                    + "\"default\":\"Name2\"}}}"),
                "TYPE"));
  }

  /**
   * Should not expand for stencil enum on post process.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldNotExpandForStencilEnumOnPostProcess() throws Exception {
    List<Stencil> processedStencils =
        stencilPostProcessor.postProcess(singletonList(new StencilType(EnumStencilObject.class)), APP_ID);

    assertThat(processedStencils)
        .hasSize(1)
        .extracting(Stencil::getName, Stencil::getJsonSchema, Stencil::getType)
        .contains(tuple("NAME",
            JsonUtils.readTree(
                "{\"type\":\"object\",\"properties\":{\"enumField\":{\"type\":\"string\",\"enum\":[\"Name1\",\"Name2\"],\"enumNames\":[\"Value1\",\"Value2\"]}}}"),
            "TYPE"));
  }

  /**
   * Should not expand for stencil enum on post process.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSetDefaultValueForTheField() throws Exception {
    List<Stencil> processedStencils =
        stencilPostProcessor.postProcess(singletonList(new StencilType(DefaultStencilObject.class)), APP_ID);

    assertThat(processedStencils)
        .hasSize(1)
        .extracting(Stencil::getName, Stencil::getJsonSchema, Stencil::getType)
        .contains(tuple("NAME",
            JsonUtils.readTree(
                "{\"type\":\"object\",\"properties\":{\"enumField\":{\"type\":\"string\",\"default\":\"hello\"}}}"),
            "TYPE"));
  }

  /**
   * Should set default value for the accessor method.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSetDefaultValueForTheAccessorMethod() throws Exception {
    List<Stencil> processedStencils =
        stencilPostProcessor.postProcess(singletonList(new StencilType(DefaultMethodStencilObject.class)), APP_ID);

    assertThat(processedStencils)
        .hasSize(1)
        .extracting(Stencil::getName, Stencil::getJsonSchema, Stencil::getType)
        .contains(tuple("NAME",
            JsonUtils.readTree(
                "{\"type\":\"object\",\"properties\":{\"enumField\":{\"type\":\"string\",\"default\":\"hello\"}}}"),
            "TYPE"));
  }

  /**
   * The interface Stencil object.
   */
  public interface StencilObject {}

  /**
   * The type Test data provider.
   */
  @Singleton
  public static class TestDataProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return ImmutableMap.of("Name1", "Value1", "Name2", "Value2");
    }
  }

  /**
   * The type Expand stencil object.
   */
  public static class ExpandStencilObject implements StencilObject {
    @EnumData(enumDataProvider = TestDataProvider.class)
    @Expand(dataProvider = TestDataProvider.class)
    private String expand;

    /**
     * Getter for property 'expand'.
     *
     * @return Value for property 'expand'.
     */
    public String getExpand() {
      return expand;
    }

    /**
     * Setter for property 'expand'.
     *
     * @param expand Value to set for property 'expand'.
     */
    public void setExpand(String expand) {
      this.expand = expand;
    }
  }

  /**
   * The type Enum stencil object.
   */
  public static class EnumStencilObject implements StencilObject {
    @EnumData(enumDataProvider = TestDataProvider.class) private String enumField;

    /**
     * Getter for property 'enumField'.
     *
     * @return Value for property 'enumField'.
     */
    public String getEnumField() {
      return enumField;
    }

    /**
     * Setter for property 'enumField'.
     *
     * @param enumField Value to set for property 'enumField'.
     */
    public void setEnumField(String enumField) {
      this.enumField = enumField;
    }
  }

  /**
   * The type Enum stencil object.
   */
  public static class DefaultStencilObject implements StencilObject {
    @DefaultValue("hello") private String enumField;

    /**
     * Getter for property 'enumField'.
     *
     * @return Value for property 'enumField'.
     */
    public String getEnumField() {
      return enumField;
    }

    /**
     * Setter for property 'enumField'.
     *
     * @param enumField Value to set for property 'enumField'.
     */
    public void setEnumField(String enumField) {
      this.enumField = enumField;
    }
  }

  /**
   * The type Enum stencil object.
   */
  public static class DefaultMethodStencilObject implements StencilObject {
    private String enumField;

    /**
     * Getter for property 'enumField'.
     *
     * @return Value for property 'enumField'.
     */
    @DefaultValue("hello")
    public String getEnumField() {
      return enumField;
    }

    /**
     * Setter for property 'enumField'.
     *
     * @param enumField Value to set for property 'enumField'.
     */
    public void setEnumField(String enumField) {
      this.enumField = enumField;
    }
  }

  /**
   * The type Stencil type.
   */
  public static class StencilType implements Stencil<StencilObject> {
    private Class<? extends StencilObject> clazz;

    /**
     * Instantiates a new Stencil type.
     *
     * @param clazz the clazz
     */
    public StencilType(Class<? extends StencilObject> clazz) {
      this.clazz = clazz;
    }

    @Override
    public String getType() {
      return "TYPE";
    }

    @Override
    public Class<?> getTypeClass() {
      return clazz;
    }

    @Override
    public JsonNode getJsonSchema() {
      return JsonUtils.jsonSchema(clazz);
    }

    @Override
    public Object getUiSchema() {
      return new HashMap<String, String>();
    }

    @Override
    public String getName() {
      return "NAME";
    }

    @Override
    public OverridingStencil getOverridingStencil() {
      return new OverridingStencilType(this);
    }

    @Override
    public ExpandStencilObject newInstance(String id) {
      return on(clazz).create().get();
    }

    @Override
    public StencilCategory getStencilCategory() {
      return StencilCategory.OTHERS;
    }

    @Override
    public Integer getDisplayOrder() {
      return DEFAULT_DISPLAY_ORDER;
    }
  }

  /**
   * The type Overriding stencil type.
   */
  public static class OverridingStencilType implements Stencil<StencilObject>, OverridingStencil<StencilObject> {
    private final StencilType stencilType;
    private Optional<String> overridingName = Optional.empty();
    private Optional<JsonNode> overridingJsonSchema = Optional.empty();

    /**
     * Instantiates a new Overriding stencil type.
     *
     * @param stencilType the stencil type
     */
    public OverridingStencilType(StencilType stencilType) {
      this.stencilType = stencilType;
    }

    @Override
    public String getType() {
      return stencilType.getType();
    }

    @Override
    public Class<?> getTypeClass() {
      return stencilType.getTypeClass();
    }

    @Override
    public JsonNode getJsonSchema() {
      return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy() : stencilType.getJsonSchema();
    }

    @Override
    public Object getUiSchema() {
      return stencilType.getUiSchema();
    }

    @Override
    public String getName() {
      return overridingName.orElse(stencilType.getName());
    }

    @Override
    public OverridingStencil getOverridingStencil() {
      return stencilType.getOverridingStencil();
    }

    @Override
    public ExpandStencilObject newInstance(String id) {
      return stencilType.newInstance(id);
    }

    @Override
    public JsonNode getOverridingJsonSchema() {
      return overridingJsonSchema.orElse(null);
    }

    @Override
    public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
      this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
    }

    @Override
    public String getOverridingName() {
      return overridingName.orElse(null);
    }

    @Override
    public void setOverridingName(String overridingName) {
      this.overridingName = Optional.ofNullable(overridingName);
    }

    @Override
    public StencilCategory getStencilCategory() {
      return stencilType == null ? null : stencilType.getStencilCategory();
    }

    @Override
    public Integer getDisplayOrder() {
      return stencilType == null ? DEFAULT_DISPLAY_ORDER : stencilType.getDisplayOrder();
    }
  }
}
