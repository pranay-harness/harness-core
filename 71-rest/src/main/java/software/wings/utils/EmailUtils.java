package software.wings.utils;

import static java.lang.String.format;

import com.google.inject.Singleton;

import software.wings.helpers.ext.mail.EmailData;

@Singleton
public class EmailUtils {
  public String getErrorString(EmailData emailData) {
    return format("Failed to send an email with subject:[%s] , to:%s", emailData.getSubject(), emailData.getTo());
  }
}
