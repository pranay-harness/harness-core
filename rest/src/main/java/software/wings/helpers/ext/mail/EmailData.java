package software.wings.helpers.ext.mail;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
@Entity(value = "emailQueue", noClassnameStored = true)
public class EmailData extends Queuable {
  private String accountId;
  private List<String> to = Lists.newArrayList();
  private List<String> cc = Lists.newArrayList();
  private String subject;
  private String body;
  private String templateName;
  private Object templateModel;
  private boolean hasHtml = true;
  private boolean system = false;

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Gets to.
   *
   * @return the to
   */
  public List<String> getTo() {
    return to;
  }

  /**
   * Sets to.
   *
   * @param to the to
   */
  public void setTo(List<String> to) {
    this.to = to;
  }

  /**
   * Gets cc.
   *
   * @return the cc
   */
  public List<String> getCc() {
    return cc;
  }

  /**
   * Sets cc.
   *
   * @param cc the cc
   */
  public void setCc(List<String> cc) {
    this.cc = cc;
  }

  /**
   * Gets subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets subject.
   *
   * @param subject the subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Gets body.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets body.
   *
   * @param body the body
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * Gets template name.
   *
   * @return the template name
   */
  public String getTemplateName() {
    return templateName;
  }

  /**
   * Sets template name.
   *
   * @param templateName the template name
   */
  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  /**
   * Gets template model.
   *
   * @return the template model
   */
  public Object getTemplateModel() {
    return templateModel;
  }

  /**
   * Sets template model.
   *
   * @param templateModel the template model
   */
  public void setTemplateModel(Object templateModel) {
    this.templateModel = templateModel;
  }

  /**
   * Is html email boolean.
   *
   * @return the boolean
   */
  public boolean isHasHtml() {
    return hasHtml;
  }

  /**
   * Sets html email.
   *
   * @param hasHtml the html email
   */
  public void setHasHtml(boolean hasHtml) {
    this.hasHtml = hasHtml;
  }

  /**
   * Getter for property 'system'.
   *
   * @return Value for property 'system'.
   */
  public boolean isSystem() {
    return system;
  }

  /**
   * Setter for property 'system'.
   *
   * @param system Value to set for property 'system'.
   */
  public void setSystem(boolean system) {
    this.system = system;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, to, cc, subject, body, templateName, templateModel, hasHtml, system);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final EmailData other = (EmailData) obj;
    return Objects.equals(this.accountId, other.accountId) && Objects.equals(this.to, other.to)
        && Objects.equals(this.cc, other.cc) && Objects.equals(this.subject, other.subject)
        && Objects.equals(this.body, other.body) && Objects.equals(this.templateName, other.templateName)
        && Objects.equals(this.templateModel, other.templateModel) && Objects.equals(this.hasHtml, other.hasHtml)
        && Objects.equals(this.system, other.system);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("to", to)
        .add("cc", cc)
        .add("subject", subject)
        .add("body", body)
        .add("templateName", templateName)
        .add("templateModel", templateModel)
        .add("hasHtml", hasHtml)
        .add("system", system)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accountId;
    private List<String> to = Lists.newArrayList();
    private List<String> cc = Lists.newArrayList();
    private String subject;
    private String body;
    private String templateName;
    private Object templateModel;
    private boolean hasHtml = true;
    private boolean system = false;
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

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withTo(List<String> to) {
      this.to = to;
      return this;
    }

    public Builder withCc(List<String> cc) {
      this.cc = cc;
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

    public Builder withHasHtml(boolean hasHtml) {
      this.hasHtml = hasHtml;
      return this;
    }

    public Builder withSystem(boolean system) {
      this.system = system;
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
          .withAccountId(accountId)
          .withTo(to)
          .withCc(cc)
          .withSubject(subject)
          .withBody(body)
          .withTemplateName(templateName)
          .withTemplateModel(templateModel)
          .withHasHtml(hasHtml)
          .withSystem(system)
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
      emailData.setAccountId(accountId);
      emailData.setTo(to);
      emailData.setCc(cc);
      emailData.setSubject(subject);
      emailData.setBody(body);
      emailData.setTemplateName(templateName);
      emailData.setTemplateModel(templateModel);
      emailData.setHasHtml(hasHtml);
      emailData.setSystem(system);
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
