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

  @Before
  public void setUpMocks() throws Exception {
    when(injector.getInstance(TestDataProvider.class)).thenReturn(new TestDataProvider());
  }

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

  public interface StencilObject {}

  public static class TestDataProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return ImmutableMap.of("Name1", "Value1", "Name2", "Value2");
    }
  }

  public static class ExpandStencilObject implements StencilObject {
    @EnumData(expandIntoMultipleEntries = true, enumDataProvider = TestDataProvider.class) private String expand;

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

  public static class StencilType implements Stencil<StencilObject> {
    private Class<? extends StencilObject> clazz;

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
  }

  public static class OverridingStencilType implements Stencil<StencilObject>, OverridingStencil<StencilObject> {
    private final StencilType stencilType;
    private Optional<String> overridingName = Optional.empty();
    private Optional<JsonNode> overridingJsonSchema = Optional.empty();

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
    public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
      this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
    }

    @Override
    public JsonNode getOverridingJsonSchema() {
      return overridingJsonSchema.orElse(null);
    }

    @Override
    public String getOverridingName() {
      return overridingName.orElse(null);
    }

    @Override
    public void setOverridingName(String overridingName) {
      this.overridingName = Optional.ofNullable(overridingName);
    }
  }
}
