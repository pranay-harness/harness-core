/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.utils;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class URLDecoderUtility {
  public String getDecodedString(String encodedString) {
    String decodedString = null;
    if (isNotBlank(encodedString)) {
      try {
        decodedString = java.net.URLDecoder.decode(encodedString, StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        log.info("Encountered exception while decoding {}", encodedString);
      }
    }
    return decodedString;
  }
}
