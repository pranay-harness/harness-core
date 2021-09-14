/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class FilePathUtils {
  public static boolean isFilePartOfFolder(String folderPath, String filePath) {
    if (filePath == null || folderPath == null) {
      throw new InvalidRequestException(
          String.format("filePath : %s or folderPath : %s cannot be null", filePath, folderPath));
    }

    return trimPath(filePath).startsWith(trimPath(folderPath));
  }

  public static List<String> getAllFilesWithinFolder(String folderPath, List<String> filePaths) {
    if (filePaths == null || folderPath == null) {
      throw new InvalidRequestException(
          String.format("filePath list : %s or folderPath : %s cannot be null", filePaths, folderPath));
    }
    List<String> validFilePaths = new ArrayList<>();
    filePaths.forEach(filePath -> {
      if (isFilePartOfFolder(folderPath, filePath)) {
        validFilePaths.add(filePath);
      }
    });
    return validFilePaths;
  }

  public static String removeTrailingChars(String path, String chars) {
    path = path.replaceAll(chars + "+$", "");
    return path;
  }

  // ---------------------------------- PRIVATE METHODS ----------------------------

  // Remove starting and ending backslashes
  private String trimPath(String path) {
    path = path.replaceAll("^/+", "");
    path = path.replaceAll("/+$", "");
    return path;
  }
}
