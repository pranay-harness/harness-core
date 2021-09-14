/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.beans.archive;

import java.io.InputStream;
import java.nio.charset.Charset;

public interface ArchiveContent {
  String getPath();

  String string(Charset encoding);

  InputStream byteStream();
}
