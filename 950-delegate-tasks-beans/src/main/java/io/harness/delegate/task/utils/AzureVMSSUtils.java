/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.utils;

import static java.lang.String.format;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AzureVMSSUtils {
  private final String ISO_8601_BASIC_FORMAT = "yyyyMMdd'T'HHmmss'Z'";

  public String dateToISO8601BasicStr(Date date) {
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat(ISO_8601_BASIC_FORMAT);
    dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
    return dateTimeFormat.format(date);
  }

  public Date iso8601BasicStrToDate(String strDate) {
    DateFormat format = new SimpleDateFormat(ISO_8601_BASIC_FORMAT);
    try {
      return format.parse(strDate);
    } catch (ParseException e) {
      throw new IllegalArgumentException(format("Unable to parse date: %s", strDate), e);
    }
  }
}
