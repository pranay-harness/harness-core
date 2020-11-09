package io.harness.stream;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;

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
