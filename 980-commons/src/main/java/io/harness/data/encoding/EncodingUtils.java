package io.harness.data.encoding;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import io.harness.exception.WingsException;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@UtilityClass
public class EncodingUtils {
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
    try (ByteArrayInputStream bis = new ByteArrayInputStream(toDecompress);
         GZIPInputStream gis = new GZIPInputStream(bis);
         BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append(System.lineSeparator());
      }
      int last = sb.lastIndexOf(System.lineSeparator());
      if (last >= 0) {
        sb.delete(last, sb.length());
      }
      return sb.toString();
    }
  }

  public static byte[] compressBytes(byte[] toCompress) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
      gzipOutputStream.write(toCompress);
      gzipOutputStream.close();
    } catch (IOException e) {
      throw new WingsException(e);
    }
    return byteArrayOutputStream.toByteArray();
  }

  public static byte[] deCompressBytes(byte[] toDecompress) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(toDecompress)), out);
    } catch (IOException e) {
      throw new WingsException(e);
    }
    return out.toByteArray();
  }

  public static String encodeBase64(byte[] toEncode) {
    return new String(Base64.getEncoder().encode(toEncode), Charsets.UTF_8);
  }

  public static String encodeBase64(char[] toEncode) {
    return encodeBase64(new String(toEncode));
  }

  public static String encodeBase64(String toEncode) {
    return encodeBase64(toEncode.getBytes(Charsets.UTF_8));
  }

  public static byte[] encodeBase64ToByteArray(byte[] toEncode) {
    return Base64.getEncoder().encode(toEncode);
  }

  public static byte[] decodeBase64(String toDecode) {
    return Base64.getDecoder().decode(toDecode);
  }

  public static byte[] decodeBase64(char[] toDecode) {
    return Base64.getDecoder().decode(new String(toDecode));
  }

  public static byte[] decodeBase64(byte[] toDecode) {
    return Base64.getDecoder().decode(new String(toDecode, Charsets.UTF_8));
  }

  public static String decodeBase64ToString(String toDecode) {
    return new String(decodeBase64(toDecode), Charsets.UTF_8);
  }
}
