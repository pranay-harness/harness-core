package software.wings.yaml.handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import software.wings.WingsBaseTest;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;

import java.io.IOException;

/**
 * @author rktummala on 1/9/18
 */
public class BaseYamlHandlerTest extends WingsBaseTest {
  protected String getYamlContent(BaseYaml yaml) {
    Yaml yamlFormatter = new Yaml(YamlHelper.getRepresenter(), YamlHelper.getDumperOptions());
    String dump = yamlFormatter.dump(yaml);
    return YamlHelper.cleanupYaml(dump);
  }

  protected BaseYaml getYaml(String yamlString, Class<? extends BaseYaml> yamlClass, boolean ignoreUnknownFields)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    if (ignoreUnknownFields) {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    return mapper.readValue(yamlString, yamlClass);
  }
}
