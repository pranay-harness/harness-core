package software.wings.service.intfc;

import software.wings.helpers.ext.mail.EmailData;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
public interface EmailNotificationService {
  /**
   * Send.
   *
   * @param emailData the email data
   */
  boolean send(EmailData emailData);

  /**
   * Send async.
   *
   * @param emailData the email data
   */
  void sendAsync(EmailData emailData);

  // For CE team
  boolean sendCeMail(EmailData emailData, boolean isCeMail);
}
