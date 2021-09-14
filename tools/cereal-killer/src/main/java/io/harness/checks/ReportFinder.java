/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ReportFinder {
  private final String baseDir;

  public ReportFinder(String baseDir) {
    this.baseDir = baseDir;
  }

  public List<String> findSurefireReports() throws IOException {
    List<String> surefireReports = new ArrayList<>();
    PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/target/surefire-reports/TEST-*.xml");
    Files.walkFileTree(Paths.get(baseDir), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        if (pathMatcher.matches(path)) {
          surefireReports.add(path.toString());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path path, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    });
    return surefireReports;
  }
}
