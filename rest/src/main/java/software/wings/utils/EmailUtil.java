package software.wings.utils;

import static java.lang.String.format;

import com.google.inject.Singleton;

import software.wings.helpers.ext.mail.EmailData;

@Singleton
public class EmailUtil {
  public String getErrorString(EmailData emailData) {
    return format("Failed to send email for subject:[%s] , to:%s", emailData.getTemplateName(), emailData.getTo());
  }
}
