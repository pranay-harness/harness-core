package io.harness.commandlibrary.server.beans.archive;

import java.io.InputStream;
import java.nio.charset.Charset;

public interface ArchiveContent {
  String getPath();

  String string(Charset encoding);

  InputStream byteStream();
}
