/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import java.io.IOException;
import javax.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class HTMLUtils {
  public static String retrieveInviteUrlFromEmail(String emailBody, String verificationElement)
      throws IOException, MessagingException {
    return getUrlFromTable(Jsoup.parse(emailBody).select("table"), verificationElement);
  }

  public static String retrieveResetUrlFromEmail(String emailBody, String verificationElement)
      throws IOException, MessagingException {
    return getUrlFromTable(Jsoup.parse(emailBody).select("table"), verificationElement);
  }

  private static String getUrlFromTable(Elements table, String verificationElement) {
    if (table.isEmpty()) {
      return null;
    }

    Elements rows = null;
    for (int i = 0; i < table.size(); i++) {
      rows = table.get(i).select("tr");
      if (rows.isEmpty()) {
        continue;
      }

      for (int j = 0; j < rows.size(); j++) {
        Elements aElements = rows.get(j).select("a");
        for (int k = 0; k < aElements.size(); k++) {
          String url = aElements.get(k).getElementsContainingOwnText(verificationElement).attr("abs:href");
          if (StringUtils.isNotBlank(url)) {
            return url;
          }
        }
      }
    }
    return null;
  }
}
