package io.harness.multiline;

import io.harness.resource.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public interface MultilineStringMixin {
  String OPEN_ML_COMMENT = "/*";
  String CLOSE_ML_COMMENT = "*/";
  String TEST_DIR = "/src/test/java/";

  // This method is accepts a multiline comment as format pattern, followed with zero or more objects
  // Never use in production code. It depends on accessing the original java file and it useful only
  // for test purposes, while running the test as part of the build process.
  default String $ML(Object... objects) {
    StackTraceElement testClassElement = new RuntimeException().getStackTrace()[1];
    String classPath = testClassElement.getClassName().replace('.', '/') + ".java";
    String absolutePath = Project.moduleDirectory(getClass()) + TEST_DIR + classPath;

    StringBuilder sb = new StringBuilder();
    try (InputStream in = new FileInputStream(new File(absolutePath));
         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line = null;
      int skip = testClassElement.getLineNumber();
      while ((line = reader.readLine()) != null) {
        if (--skip <= 0) {
          sb.append(line).append('\n');
          if (line.indexOf("*/") >= 0) {
            break;
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("", e);
    }

    String commentArea = sb.toString();

    final String substring =
        commentArea.substring(commentArea.indexOf(OPEN_ML_COMMENT) + 2, commentArea.indexOf(CLOSE_ML_COMMENT));
    return String.format(substring, objects);
  }
}