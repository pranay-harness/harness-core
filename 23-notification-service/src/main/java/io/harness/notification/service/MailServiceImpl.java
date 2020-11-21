package io.harness.notification.service;

import static io.harness.NotificationClientConstants.HARNESS_NAME;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.exception.ExceptionUtils;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.SmtpConfig;
import io.harness.notification.remote.dto.EmailSettingDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.service.api.MailService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.serializer.YamlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class MailServiceImpl implements MailService {
  private final Configuration cfg = new Configuration(VERSION_2_3_23);
  private final SmtpConfig smtpConfig;
  private final NotificationSettingsService notificationSettingsService;
  private final NotificationTemplateService notificationTemplateService;
  private final YamlUtils yamlUtils;

  @Override
  public boolean send(NotificationRequest notificationRequest) {
    if (Objects.isNull(notificationRequest) || !notificationRequest.hasEmail()) {
      return false;
    }

    String notificationId = notificationRequest.getId();
    NotificationRequest.Email mailDetails = notificationRequest.getEmail();
    Map<String, String> templateData = mailDetails.getTemplateDataMap();
    String templateId = mailDetails.getTemplateId();

    if (Objects.isNull(stripToNull(templateId))) {
      log.info("template Id is null for notification request {}", notificationId);
      return false;
    }

    List<String> emailIds = resolveRecipients(notificationRequest);
    if (isEmpty(emailIds)) {
      log.info("No recipients found in notification request {}", notificationId);
      return false;
    }

    return send(emailIds, templateId, templateData, notificationId, notificationRequest.getTeam());
  }

  @Override
  public boolean send(
      List<String> emailIds, String templateId, Map<String, String> templateData, String notificationId, Team team) {
    try {
      String subject = null;
      String body = null;
      Optional<EmailTemplate> emailTemplateOpt = getTemplate(templateId, team);
      if (!emailTemplateOpt.isPresent()) {
        log.error(
            "Failed to send email for notification request {} possibly due to no valid template with name {} found",
            notificationId, templateId);
        return false;
      }
      EmailTemplate emailTemplate = emailTemplateOpt.get();

      subject = processTemplate(templateId + "-subject", emailTemplate.getSubject(), templateData);
      body = processTemplate(templateId + "-body", emailTemplate.getBody(), templateData);

      return this.send(emailIds, subject, body, notificationId, team);
    } catch (Exception e) {
      log.error("Failed to send email. Check template details for notificationId: {}\n{}", notificationId,
          ExceptionUtils.getMessage(e));
      return false;
    }
  }

  @Override
  public boolean send(
      List<String> emailIds, String templateId, Map<String, String> templateData, String notificationId) {
    return send(emailIds, templateId, templateData, notificationId, null);
  }

  @Override
  public boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO) {
    EmailSettingDTO emailSettingDTO = (EmailSettingDTO) notificationSettingDTO;
    return send(Collections.singletonList(emailSettingDTO.getRecipient()), emailSettingDTO.getSubject(),
        emailSettingDTO.getBody(), emailSettingDTO.getNotificationId(), null);
  }

  private boolean send(List<String> emailIds, String subject, String body, String notificationId, Team team) {
    try {
      if (Objects.isNull(stripToNull(body))) {
        log.error("No email body available. Aborting notification request {}", notificationId);
        return false;
      }

      Email email = new HtmlEmail();
      email.setHostName(smtpConfig.getHost());
      email.setSmtpPort(smtpConfig.getPort());

      if (!isEmpty(smtpConfig.getPassword())) {
        email.setAuthenticator(
            new DefaultAuthenticator(smtpConfig.getUsername(), new String(smtpConfig.getPassword())));
      }
      email.setSSLOnConnect(smtpConfig.isUseSSL());
      if (smtpConfig.isUseSSL()) {
        email.setSslSmtpPort(Integer.toString(smtpConfig.getPort()));
      }

      try {
        email.setReplyTo(ImmutableList.of(new InternetAddress(smtpConfig.getFromAddress())));
      } catch (AddressException | EmailException e) {
        log.error(ExceptionUtils.getMessage(e), e);
      }
      email.setFrom(smtpConfig.getFromAddress(), HARNESS_NAME);

      for (String emailId : emailIds) {
        email.addTo(emailId);
      }

      email.setSubject(subject);
      ((HtmlEmail) email).setHtmlMsg(body);
      email.send();
    } catch (EmailException e) {
      log.error("Failed to send email. Check SMTP configuration. notificationId: {}\n{}", notificationId,
          ExceptionUtils.getMessage(e));
      return false;
    }
    return true;
  }

  private String processTemplate(String templateName, String templateStr, Map<String, String> templateData) {
    if (Objects.isNull(stripToNull(templateStr))) {
      return null;
    }
    Template template = null;
    try {
      template = new Template(templateName, templateStr, cfg);
    } catch (IOException e) {
      log.error("Can't parse the stored file as .ftl template file");
    }

    if (Objects.isNull(template)) {
      return null;
    }

    Map<String, String> dataMap = new HashMap<>(templateData);
    boolean exceptionCaught;
    StringWriter strWriter;
    do {
      strWriter = new StringWriter();
      exceptionCaught = false;
      try {
        template.process(dataMap, strWriter);
      } catch (InvalidReferenceException e) {
        exceptionCaught = true;
        dataMap.put(e.getBlamedExpressionString(), String.format("${%s}", e.getBlamedExpressionString()));
      } catch (IOException | TemplateException e) {
        log.error("Failed to process template. Check template {}", templateName);
      }
    } while (exceptionCaught);

    return strWriter.toString();
  }

  private Optional<EmailTemplate> getTemplate(String templateId, Team team) {
    Optional<String> templateStrOptional = notificationTemplateService.getTemplateAsString(templateId, team);
    Optional<EmailTemplate> emailTemplateOpt = Optional.empty();
    if (templateStrOptional.isPresent()) {
      try {
        emailTemplateOpt =
            Optional.of(yamlUtils.read(templateStrOptional.get(), new TypeReference<EmailTemplate>() {}));
      } catch (IOException e) {
        log.error("failed to parse template {} into yaml. Please check the format", templateId);
      }
    }
    return emailTemplateOpt;
  }

  private List<String> resolveRecipients(NotificationRequest notificationRequest) {
    NotificationRequest.Email emailDetails = notificationRequest.getEmail();
    List<String> recipients = new ArrayList<>(emailDetails.getEmailIdsList());
    List<String> resolvedRecipients = notificationSettingsService.getNotificationSettingsForGroups(
        emailDetails.getUserGroupIdsList(), NotificationChannelType.EMAIL, notificationRequest.getAccountId());
    recipients.addAll(resolvedRecipients);
    return recipients;
  }

  @Getter
  @Setter
  public static class EmailTemplate {
    private String subject;
    private String body;
  }
}
