package io.harness.yaml;

import io.harness.exception.InvalidRequestException;
import io.harness.govern.ProviderModule;
import io.harness.yaml.schema.YamlSchemaHelper;
import io.harness.yaml.snippets.dto.YamlSnippetsDTO;
import io.harness.yaml.snippets.helper.YamlSnippetHelper;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class YamlSdkModule {
  YamlSnippetHelper yamlSnippetHelper;
  YamlSchemaValidator yamlSchemaValidator;
  YamlSchemaHelper yamlSchemaHelper;
  private static YamlSdkModule defaultInstance;

  public static YamlSdkModule getDefaultInstance() {
    return defaultInstance;
  }

  /**
   * @param config Configuration {@link YamlSdkConfiguration} for initialization of SDK.
   */
  public static void initializeDefaultInstance(YamlSdkConfiguration config) {
    if (defaultInstance == null) {
      defaultInstance = new YamlSdkModule(config);
      defaultInstance.initialize();
    }
  }

  private final YamlSdkConfiguration config;

  private YamlSdkModule(YamlSdkConfiguration config) {
    this.config = config;
  }

  private void initialize() {
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      public YamlSdkConfiguration config() {
        return config;
      }
    });
    Injector injector = Guice.createInjector(modules);
    yamlSnippetHelper = injector.getInstance(YamlSnippetHelper.class);
    yamlSchemaValidator = injector.getInstance(YamlSchemaValidator.class);
    yamlSchemaHelper = injector.getInstance(YamlSchemaHelper.class);
    initializeSnippets();
    initializeValidatorWithSchema();
    initializeSchemas();
  }

  /**
   * Initialises snippet which caches and maintains maps for fast retrieval of snippets and snippets metadata {@link
   * YamlSnippetsDTO}.
   *
   * @throws InvalidRequestException when snippets couldn't be initialized.
   */
  private void initializeSnippets() {
    try {
      final InputStream inputStream = config.getSnippetIndex();
      if (inputStream == null) {
        log.info("No Yaml Snippets found");
        return;
      }
      String snippetMetaData = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
      yamlSnippetHelper.preComputeTagsAndNameMap(snippetMetaData);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot initialize snippets", e);
    }
  }

  /**
   * Initialises a static map which will help in fast validation against a schema.
   */
  private void initializeValidatorWithSchema() {
    yamlSchemaValidator.populateSchemaInStaticMap(config.getSchemaBasePath());
  }

  /**
   * Initialises a static map which will help in fast retrieval of schemas.
   */
  private void initializeSchemas() {
    yamlSchemaHelper.initializeSchemaMaps(config.getSchemaBasePath());
  }
}
