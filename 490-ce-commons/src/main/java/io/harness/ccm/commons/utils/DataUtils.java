package io.harness.ccm.commons.utils;

import com.google.inject.Singleton;
import java.util.Calendar;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DataUtils {
  public Calendar getDefaultCalendar() {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  }
}
