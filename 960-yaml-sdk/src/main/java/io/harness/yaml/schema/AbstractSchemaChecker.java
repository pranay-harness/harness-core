package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.packages.HarnessPackages.IO_HARNESS;
import static io.harness.packages.HarnessPackages.SOFTWARE_WINGS;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.reflection.CodeUtils;
import io.harness.validation.OneOfField;
import io.harness.validation.OneOfFields;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

public interface AbstractSchemaChecker {
  default void schemaTests(Logger logger) {
    Reflections reflections = new Reflections(IO_HARNESS, SOFTWARE_WINGS);
    ensureSchemaUpdated(reflections, logger);
    ensureOneOfHasCorrectValues(reflections, logger);
  }

  default void ensureSchemaUpdated(Reflections reflections, Logger log) {
    final Set<Class<?>> schemaRoots = reflections.getTypesAnnotatedWith(YamlSchemaRoot.class, true);
    if (isEmpty(schemaRoots)) {
      return;
    }
    for (Class<?> schemaRoot : schemaRoots) {
      log.info("Running schema check for {}", schemaRoot.getCanonicalName());
      final EntityType entityType = schemaRoot.getDeclaredAnnotation(YamlSchemaRoot.class).value();
      final String schemaBasePath = YamlSdkConfiguration.schemaBasePath;
      final String schemaPathForEntityType = YamlSchemaUtils.getSchemaPathForEntityType(entityType, schemaBasePath);
      String moduleBasePath = getModulePath(schemaRoot);
      executeCommand("git diff --exit-code -- " + moduleBasePath + schemaPathForEntityType);
    }
  }

  default String getModulePath(Class<?> schemaRoot) {
    String moduleBasePath;
    List<String> locationList = Arrays.asList(Preconditions.checkNotNull(CodeUtils.location(schemaRoot)).split("/"));
    // Bazel do not have target folder, so tha path is directly coming from .m2.
    // Instead of target we are using 0.0.1-SNAPSHOT to handle bazel build modules.
    if (locationList.contains("0.0.1-SNAPSHOT")) {
      moduleBasePath = locationList.get(locationList.indexOf("0.0.1-SNAPSHOT") - 1) + "/src/main/resources/";
    } else {
      moduleBasePath = locationList.get(locationList.indexOf("target") - 1) + "/src/main/resources/";
    }
    return moduleBasePath;
  }

  default String executeCommand(String command) {
    final String[] returnString = new String[1];
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(30, TimeUnit.SECONDS)
                                            .command("/bin/sh", "-c", command)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String s) {
                                                returnString[0] = s;
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      if (processResult.getExitValue() != 0) {
        throw new InvalidRequestException(String.format("Command Execution failed for %s", command));
      }

    } catch (InterruptedException | TimeoutException | IOException ex) {
      throw new InvalidRequestException("Command Execution failed");
    }
    return Arrays.toString(returnString);
  }

  default void ensureOneOfHasCorrectValues(Reflections reflections, Logger log) {
    final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(OneOfField.class, true);
    for (Class<?> clazz : typesAnnotatedWith) {
      final OneOfField annotation = clazz.getAnnotation(OneOfField.class);
      validateForOneOfFields(clazz, annotation, log);
    }
    final Set<Class<?>> typesAnnotated = reflections.getTypesAnnotatedWith(OneOfFields.class, true);
    for (Class<?> clazz : typesAnnotated) {
      final OneOfFields annotation = clazz.getAnnotation(OneOfFields.class);
      final OneOfField[] value = annotation.value();
      for (OneOfField oneOfField : value) {
        validateForOneOfFields(clazz, oneOfField, log);
      }
    }
  }

  default void validateForOneOfFields(Class<?> clazz, OneOfField annotation, Logger log) {
    final String[] fields = annotation.fields();
    final Field[] declaredFieldsInClass = clazz.getDeclaredFields();
    final Set<String> decFieldSwaggerName =
        Arrays.stream(declaredFieldsInClass).map(YamlSchemaUtils::getFieldName).collect(Collectors.toSet());
    for (String field : fields) {
      if (!decFieldSwaggerName.contains(field)) {
        throw new InvalidRequestException(String.format("Field %s has incorrect Name", field));
      }
      log.info("One of field passed for field {}", field);
    }
  }
}
