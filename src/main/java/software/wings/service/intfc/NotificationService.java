package software.wings.service.intfc;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import software.wings.helpers.ext.mail.EmailData;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
public interface NotificationService<T> {
  void send(String from, List<String> to, String templateName, Object templateModel)
      throws EmailException, TemplateException, IOException;
  void send(String from, List<String> to, String subject, String body)
      throws EmailException, TemplateException, IOException;

  void sendAsync(String from, List<String> to, String subject, String body);

  void send(T emailData) throws EmailException, TemplateException, IOException;

  void sendAsync(String from, List<String> to, String templateName, Object templateModel);
}
