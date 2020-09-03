package io.harness.resource;

import com.google.common.io.Resources;

import io.harness.exception.LoadResourceException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.Charset;

@UtilityClass
public class Loader {
  public static String load(String resource) {
    try {
      return Resources.toString(Resources.getResource(resource), Charset.defaultCharset());
    } catch (IOException e) {
      throw new LoadResourceException(resource, e);
    }
  }
}
