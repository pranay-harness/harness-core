package software.wings.helpers.ext.mail;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
@Entity(value = "emailQueue", noClassnameStored = true)
public class EmailData extends Queuable {
  private String from;
  private List<String> to;
  private String subject;
  private String body;
  private String templateName;
  private Object templateModel;

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public List<String> getTo() {
    return to;
  }

  public void setTo(List<String> to) {
    this.to = to;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public Object getTemplateModel() {
    return templateModel;
  }

  public void setTemplateModel(Object templateModel) {
    this.templateModel = templateModel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailData emailData = (EmailData) o;
    return Objects.equal(from, emailData.from) && Objects.equal(to, emailData.to)
        && Objects.equal(subject, emailData.subject) && Objects.equal(body, emailData.body)
        && Objects.equal(templateName, emailData.templateName) && Objects.equal(templateModel, emailData.templateModel);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(from, to, subject, body, templateName, templateModel);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("from", from)
        .add("to", to)
        .add("subject", subject)
        .add("body", body)
        .add("templateName", templateName)
        .add("templateModel", templateModel)
        .toString();
  }

  public static final class Builder {
    private String from;
    private List<String> to;
    private String subject;
    private String body;
    private String templateName;
    private Object templateModel;
    private String id;
    private boolean running = false;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority = 0.0;
    private Date created = new Date();
    private int retries = 0;

    private Builder() {}

    public static Builder anEmailData() {
      return new Builder();
    }

    public Builder withFrom(String from) {
      this.from = from;
      return this;
    }

    public Builder withTo(List<String> to) {
      this.to = to;
      return this;
    }

    public Builder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    public Builder withTemplateName(String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder withTemplateModel(Object templateModel) {
      this.templateModel = templateModel;
      return this;
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withRunning(boolean running) {
      this.running = running;
      return this;
    }

    public Builder withResetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
      return this;
    }

    public Builder withPriority(double priority) {
      this.priority = priority;
      return this;
    }

    public Builder withCreated(Date created) {
      this.created = created;
      return this;
    }

    public Builder withRetries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder but() {
      return anEmailData()
          .withFrom(from)
          .withTo(to)
          .withSubject(subject)
          .withBody(body)
          .withTemplateName(templateName)
          .withTemplateModel(templateModel)
          .withId(id)
          .withRunning(running)
          .withResetTimestamp(resetTimestamp)
          .withEarliestGet(earliestGet)
          .withPriority(priority)
          .withCreated(created)
          .withRetries(retries);
    }

    public EmailData build() {
      EmailData emailData = new EmailData();
      emailData.setFrom(from);
      emailData.setTo(to);
      emailData.setSubject(subject);
      emailData.setBody(body);
      emailData.setTemplateName(templateName);
      emailData.setTemplateModel(templateModel);
      emailData.setId(id);
      emailData.setRunning(running);
      emailData.setResetTimestamp(resetTimestamp);
      emailData.setEarliestGet(earliestGet);
      emailData.setPriority(priority);
      emailData.setCreated(created);
      emailData.setRetries(retries);
      return emailData;
    }
  }
}
