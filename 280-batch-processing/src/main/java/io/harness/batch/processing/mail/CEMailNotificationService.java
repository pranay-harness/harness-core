/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.mail;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.exception.WingsException;

import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CEMailNotificationService {
  @Autowired WingsPersistence wingsPersistence;
  @Autowired BatchMainConfig mainConfiguration;
  @Autowired CEMailer mailer;

  public boolean send(EmailData emailData) {
    return send(emailData, null);
  }

  public boolean send(EmailData emailData, byte[] image) {
    SmtpConfig defaultSMTPConfig;
    defaultSMTPConfig = mainConfiguration.getSmtpConfig();
    boolean isDefaultSMTPConfigValid = isSmtpConfigValid(defaultSMTPConfig);

    boolean mailSentSuccessFully = false;
    if (!isDefaultSMTPConfigValid) {
      log.warn("Mail not sent, : {}", getErrorString(emailData));
    } else {
      mailSentSuccessFully = true;
      if (!sendMail(defaultSMTPConfig, emailData, image)) {
        log.warn("Mail not sent, : {}", getErrorString(emailData));
        mailSentSuccessFully = false;
      }
    }

    return mailSentSuccessFully;
  }

  public String buildAbsoluteUrl(String fragment) throws URISyntaxException {
    String baseUrl = mainConfiguration.getBaseUrl();
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  private boolean sendMail(SmtpConfig config, EmailData emailData, byte[] image) {
    if (config.equals(mainConfiguration.getSmtpConfig())) {
      try {
        mailer.send(config, emailData, image);
        return true;
      } catch (WingsException e) {
        String errorString = getErrorString(emailData);
        log.warn(errorString, e);
        return false;
      }
    } else {
      log.warn("Mail not sent in sendMail");
      return false;
    }
  }

  private boolean isSmtpConfigValid(SmtpConfig config) {
    return config != null && config.valid();
  }

  private String getErrorString(EmailData emailData) {
    return String.format(
        "Failed to send an email with subject:[%s] , to:%s", emailData.getSubject(), emailData.getTo());
  }
}
