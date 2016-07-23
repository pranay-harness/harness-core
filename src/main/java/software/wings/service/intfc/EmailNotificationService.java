package software.wings.service.intfc;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/23/16.
 *
 * @param <T> the generic type
 */
public interface EmailNotificationService<T> {
  /**
   * Send.
   *
   * @param to            the to
   * @param cc            the cc
   * @param templateName  the template name
   * @param templateModel the template model
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
  void send(List<String> to, List<String> cc, String templateName, Object templateModel)
      throws EmailException, TemplateException, IOException;

  /**
   * Send.
   *
   * @param to      the to
   * @param cc      the cc
   * @param subject the subject
   * @param body    the body
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
  void send(List<String> to, List<String> cc, String subject, String body)
      throws EmailException, TemplateException, IOException;

  /**
   * Send async.
   *
   * @param to      the to
   * @param cc      the cc
   * @param subject the subject
   * @param body    the body
   */
  void sendAsync(List<String> to, List<String> cc, String subject, String body);

  /**
   * Send.
   *
   * @param emailData the email data
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
  void send(T emailData) throws EmailException, TemplateException, IOException;

  /**
   * Send async.
   *
   * @param to            the to
   * @param cc            the cc
   * @param templateName  the template name
   * @param templateModel the template model
   */
  void sendAsync(List<String> to, List<String> cc, String templateName, Object templateModel);
}
