/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static java.util.Arrays.stream;

import java.io.BufferedInputStream;

/**
 * Created by peeyushaggarwal on 9/6/16.
 */
public class FileTypeDetector {
  /**
   * Detect type file type.
   *
   * @param bufferedInputStream the buffered input stream
   * @return the file type
   */
  public static FileType detectType(BufferedInputStream bufferedInputStream) {
    return stream(FileType.values()).filter(fileType -> fileType.test(bufferedInputStream)).findFirst().get();
  }
}
