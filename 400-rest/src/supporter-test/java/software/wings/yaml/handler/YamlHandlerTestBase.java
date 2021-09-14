/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.handler;

import io.harness.yaml.BaseYaml;
import io.harness.yaml.YamlUtils;

import software.wings.WingsBaseTest;
import software.wings.yaml.YamlHelper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import java.io.IOException;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * @author rktummala on 1/9/18
 */
public class YamlHandlerTestBase extends WingsBaseTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  protected String getYamlContent(BaseYaml yaml) {
    Yaml yamlFormatter = new Yaml(YamlHelper.getRepresenter(), YamlUtils.getDumperOptions());
    String dump = yamlFormatter.dump(yaml);
    return YamlUtils.cleanupYaml(dump);
  }

  protected BaseYaml getYaml(String yamlString, Class<? extends BaseYaml> yamlClass) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    return mapper.readValue(yamlString, yamlClass);
  }

  /*
  If this test breaks, make sure you have added the new attribute in the yaml class as well. After that
  we also need to update the toYaml and fromYaml method of the corresponding Yaml Handler.
   */
  public int attributeDiff(Class bean, Class yaml) {
    int attributesInBean = bean.getDeclaredFields().length;
    int attributesInYaml = yaml.getDeclaredFields().length;
    return attributesInBean - attributesInYaml;
  }
}
