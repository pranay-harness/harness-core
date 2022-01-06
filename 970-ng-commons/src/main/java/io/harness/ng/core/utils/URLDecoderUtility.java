/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
