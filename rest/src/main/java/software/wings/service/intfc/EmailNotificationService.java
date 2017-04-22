package software.wings.service.intfc;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import software.wings.helpers.ext.mail.EmailData;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
public interface EmailNotificationService {
  /**
   * Send.
   *
   * @param emailData the email data
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
  void send(EmailData emailData) throws EmailException, TemplateException, IOException;

  /**
   * Send async.
   *
   * @param emailData the email data
   */
  void sendAsync(EmailData emailData);
}
