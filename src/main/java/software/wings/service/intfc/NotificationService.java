package software.wings.service.intfc;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
public interface NotificationService<T> {
  void send(List<String> to, List<String> cc, String templateName, Object templateModel)
      throws EmailException, TemplateException, IOException;

  void send(List<String> to, List<String> cc, String subject, String body)
      throws EmailException, TemplateException, IOException;

  void sendAsync(List<String> to, List<String> cc, String subject, String body);

  void send(T emailData) throws EmailException, TemplateException, IOException;

  void sendAsync(List<String> to, List<String> cc, String templateName, Object templateModel);
}
