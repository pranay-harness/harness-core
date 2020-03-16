package io.harness;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public interface UrlConnectionMixin {
  static int checkIfFileExists(String fileUrl) throws IOException {
    URL url = new URL(fileUrl);
    HttpURLConnection huc = (HttpURLConnection) url.openConnection();
    huc.setRequestMethod("HEAD");
    return huc.getResponseCode();
  }
}
