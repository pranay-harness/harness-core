/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class SignVerifier {
  private static final String HARNESS_SIGNATURE_FILE_NAME = "META-INF/HARNESSJ.SF";

  public boolean verify(JarFile jar) throws IOException {
    // Since jarverifier is not available in JRE we will have to manually trigger the verification by reading
    // some portion of each of the jar entries.
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      byte[] buffer = new byte[2048];
      try (InputStream is = jar.getInputStream(entry)) {
        while ((is.read(buffer, 0, buffer.length)) != -1) {
          // We just read. This will throw a SecurityException
          // if a signature/digest check fails.
          log.trace("Reading jar entries to trigger signing check...");
        }
      } catch (SecurityException se) {
        log.error("Jar signing verification failed", se);
        return false;
      }
    }
    return true;
  }

  // Because the in build jvm jar verification allows for non-signed or properly signed jars, to make sure that
  // the jar is always signed, we making an extra check to find a resource that we adding as part of the signing
  // process.
  public boolean meticulouslyVerify(JarFile jar) throws IOException {
    // Check if jar is signed properly (This will pass if jar was not signed at all)
    if (!SignVerifier.verify(jar)) {
      return false;
    }

    // Additional check to make sure that jar file was signed by Harness
    return jar.getJarEntry(HARNESS_SIGNATURE_FILE_NAME) != null;
  }
}
