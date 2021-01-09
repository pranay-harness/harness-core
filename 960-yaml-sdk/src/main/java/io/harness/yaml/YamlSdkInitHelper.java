package io.harness.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.YamlSchemaHelper;
import io.harness.yaml.schema.YamlSchemaRoot;
import io.harness.yaml.snippets.dto.YamlSnippetsDTO;
import io.harness.yaml.snippets.helper.YamlSnippetHelper;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.google.inject.Injector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class YamlSdkInitHelper {
  public static void initialize(Injector injector) {
    YamlSnippetHelper yamlSnippetHelper = injector.getInstance(YamlSnippetHelper.class);
    YamlSchemaValidator yamlSchemaValidator = injector.getInstance(YamlSchemaValidator.class);
    YamlSchemaHelper yamlSchemaHelper = injector.getInstance(YamlSchemaHelper.class);
    final Set<Class<?>> classes = YamlSchemaUtils.getClasses(YamlSchemaRoot.class);
    if (isEmpty(classes)) {
      return;
    }
    initializeSnippets(yamlSnippetHelper, classes);
    initializeValidatorWithSchema(yamlSchemaValidator, classes);
    initializeSchemas(yamlSchemaHelper, classes);
  }

  /**
   * Initialises snippet which caches and maintains maps for fast retrieval of snippets and snippets metadata {@link
   * YamlSnippetsDTO}.
   *
   * @param yamlSnippetHelper
   * @throws InvalidRequestException when snippets couldn't be initialized.
   */
  private static void initializeSnippets(YamlSnippetHelper yamlSnippetHelper, Set<Class<?>> classes) {
    try {
      classes.forEach(clazz -> {
        try {
          final String snippetIndexPathForEntityType = YamlSchemaUtils.getSnippetIndexPathForEntityType(
              clazz, YamlSdkConfiguration.snippetBasePath, YamlSdkConfiguration.snippetIndexFile);
          String snippetMetaData =
              IOUtils.resourceToString(snippetIndexPathForEntityType, StandardCharsets.UTF_8, clazz.getClassLoader());
          yamlSnippetHelper.preComputeTagsAndNameMap(snippetMetaData);
        } catch (IOException e) {
          log.info("No Yaml Snippets found for {}", clazz.getCanonicalName());
        }
      });

    } catch (Exception e) {
      throw new InvalidRequestException("Cannot initialize snippets", e);
    }
  }

  /**
   * Initialises a static map which will help in fast validation against a schema.
   *
   * @param yamlSchemaValidator
   */
  private static void initializeValidatorWithSchema(YamlSchemaValidator yamlSchemaValidator, Set<Class<?>> classes) {
    yamlSchemaValidator.populateSchemaInStaticMap(YamlSdkConfiguration.schemaBasePath, classes);
  }

  /**
   * Initialises a static map which will help in fast retrieval of schemas.
   *
   * @param yamlSchemaHelper
   * @param classes
   */
  private static void initializeSchemas(YamlSchemaHelper yamlSchemaHelper, Set<Class<?>> classes) {
    yamlSchemaHelper.initializeSchemaMaps(YamlSdkConfiguration.schemaBasePath, classes);
  }
}
