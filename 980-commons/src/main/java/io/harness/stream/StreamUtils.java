/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.stream;

import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StreamUtils {
  public static long getInputStreamSize(InputStream inputStream) throws IOException {
    long size = 0;
    int chunk;
    byte[] buffer = new byte[1024];
    while ((chunk = inputStream.read(buffer)) != -1) {
      size += chunk;
      if (size > Integer.MAX_VALUE) {
        return -1;
      }
    }
    return size;
  }
}
