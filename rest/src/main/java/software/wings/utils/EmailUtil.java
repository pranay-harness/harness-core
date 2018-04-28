package software.wings.utils;

import com.google.inject.Singleton;

import software.wings.helpers.ext.mail.EmailData;

@Singleton
public class EmailUtil {
  public String getErrorString(EmailData emailData) {
    return String.format(
        "Failed to send email for subject:[%s] , to:%s", emailData.getTemplateName(), emailData.getTo());
  }
}
