package software.wings.utils;

import org.apache.commons.validator.routines.UrlValidator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

/**
 * Created by anubhaw on 5/2/17.
 */
public class HttpUtil {
  static UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});

  public static boolean connectableHost(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 5000); // 5 sec timeout
      return true;
    } catch (IOException ignored) {
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }

  public static boolean connectableHost(String urlString) {
    try {
      URL url = new URL(urlString);
      return connectableHost(url.getHost(), url.getPort() < 0 ? 80 : url.getPort());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static boolean connectableHttpUrl(String url) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod("HEAD");
      int responseCode = connection.getResponseCode();
      if ((responseCode >= 200 && responseCode <= 399) || responseCode == 401 || responseCode == 403) {
        return true;
      }
    } catch (IOException ignored) {
    }
    return false;
  }
  public static boolean validUrl(String url) {
    return urlValidator.isValid(url);
  }
}
