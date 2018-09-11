package software.wings.delegatetasks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class CustomDataCollectionUtil {
  private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  public static String resolvedUrl(String url, String host, long startTime, long endTime) {
    String result = url;
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat(ISO_DATE_FORMAT);
    df.setTimeZone(tz);

    if (result.contains("${start_time}")) {
      result = result.replace("${start_time}", String.valueOf(startTime));
    }
    if (result.contains("${end_time}")) {
      result = result.replace("${end_time}", String.valueOf(endTime));
    }
    if (result.contains("${iso_start_time}")) {
      String startIso = df.format(startTime);
      result = result.replace("${iso_start_time}", startIso);
    }
    if (result.contains("${iso_end_time}")) {
      String endIso = df.format(endTime);
      result = result.replace("${iso_end_time}", endIso);
    }
    if (result.contains("${start_time_seconds}")) {
      result = result.replace("${start_time_seconds}", String.valueOf(startTime / 1000L));
    }
    if (result.contains("${end_time_seconds}")) {
      result = result.replace("${end_time_seconds}", String.valueOf(endTime / 1000L));
    }

    if (result.contains("${host}")) {
      result = result.replace("${host}", host);
    }

    return result;
  }
}
