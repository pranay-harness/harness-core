package io.harness.data.compression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by rsingh on 8/24/18.
 */
public class CompressionUtils {
  public static byte[] compressString(String toCompress) throws IOException {
    Preconditions.checkState(isNotEmpty(toCompress));
    ByteArrayOutputStream bos = new ByteArrayOutputStream(toCompress.length());
    GZIPOutputStream gzip = new GZIPOutputStream(bos);
    gzip.write(toCompress.getBytes(Charset.forName("UTF-8")));
    gzip.close();
    byte[] compressed = bos.toByteArray();
    bos.close();
    return compressed;
  }

  public static String deCompressString(byte[] toDecompress) throws IOException {
    Preconditions.checkNotNull(toDecompress);
    ByteArrayInputStream bis = new ByteArrayInputStream(toDecompress);
    GZIPInputStream gis = new GZIPInputStream(bis);
    BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    br.close();
    gis.close();
    bis.close();
    return sb.toString();
  }
}
