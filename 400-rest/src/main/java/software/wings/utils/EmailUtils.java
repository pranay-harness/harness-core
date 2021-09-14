/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static java.lang.String.format;

import software.wings.helpers.ext.mail.EmailData;

import com.google.inject.Singleton;

@Singleton
public class EmailUtils {
  public String getErrorString(EmailData emailData) {
    return format("Failed to send an email with subject:[%s] , to:%s", emailData.getSubject(), emailData.getTo());
  }
}
