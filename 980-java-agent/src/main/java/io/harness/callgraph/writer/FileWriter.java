package io.harness.callgraph.writer;

import io.harness.callgraph.util.config.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class FileWriter {
  private static final int BUFFER_SIZE = 8192;

  /**
   * File writeTo.
   */
  BufferedWriter writer;

  FileWriter(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
    String outDir;
    outDir = Config.getInst() == null ? "/tmp/" : Config.getInst().outDir();
    File target = new File(outDir + fileName);
    target.getParentFile().mkdirs();
    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target, true), "UTF-8"), BUFFER_SIZE);
  }

  public void close() throws IOException {
    writer.flush();
    writer.close();
  }

  void append(String text) throws IOException {
    writer.write(text);
    writer.flush(); // TODO do we need this? its less performant
  }
}
