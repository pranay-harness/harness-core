package io.harness.ccm.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.ccm.service.intf.CEYamlService;
import io.harness.exception.UnexpectedException;

import com.google.common.collect.ImmutableMap;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

public class CEYamlServiceImpl implements CEYamlService {
  private static final Configuration templateConfiguration = new Configuration(VERSION_2_3_23);
  private static final String COST_OPTIMISATION = "cost-optimisation-crd";
  private static final String YAML = ".yaml";
  private static final String YAML_FTL = ".yaml.ftl";

  static {
    templateConfiguration.setTemplateLoader(new ClassTemplateLoader(CEYamlServiceImpl.class, "/yamltemplates"));
  }

  @Override
  public File downloadCostOptimisationYaml(String accountId, String connectorIdentifier, String apiKey,
      String harnessHost, String serverName) throws IOException {
    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("accountId", accountId)
                                                    .put("connectorIdentifier", connectorIdentifier)
                                                    .put("envoyHarnessHostname", serverName)
                                                    .put("harnessHostname", harnessHost)
                                                    .put("APIToken", apiKey)
                                                    .build();

    File yaml = File.createTempFile(COST_OPTIMISATION, YAML);

    saveProcessedTemplate(scriptParams, yaml, COST_OPTIMISATION + YAML_FTL);
    return new File(yaml.getAbsolutePath());
  }

  private void saveProcessedTemplate(Map<String, String> scriptParams, File start, String template) throws IOException {
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(start), UTF_8)) {
      templateConfiguration.getTemplate(template).process(scriptParams, fileWriter);
    } catch (TemplateException ex) {
      throw new UnexpectedException("This templates are included in the jar, they should be safe to process", ex);
    }
  }
}
