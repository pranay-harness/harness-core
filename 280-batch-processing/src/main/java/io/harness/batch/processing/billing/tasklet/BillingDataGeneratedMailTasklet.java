package io.harness.batch.processing.billing.tasklet;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.tasklet.dao.intfc.DataGeneratedNotificationDao;
import io.harness.batch.processing.billing.tasklet.entities.DataGeneratedNotification;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.mail.CEMailNotificationService;
import io.harness.ccm.cluster.entities.CEUserInfo;
import io.harness.ccm.commons.utils.DataUtils;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.User;
import software.wings.beans.ce.CEMetadataRecord;
import software.wings.beans.security.UserGroup;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class BillingDataGeneratedMailTasklet implements Tasklet {
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private DataGeneratedNotificationDao notificationDao;
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataUtils utils;
  @Autowired private CEMailNotificationService emailNotificationService;
  private JobParameters parameters;

  private static final int MAX_RETRY_COUNT = 3;
  private static final long ONE_DAY_MILLIS = 86400000;
  private static final String CE_EXPLORER_URL = "/account/%s/continuous-efficiency/overview";
  private static final String FIRST_RECORD_TIME_QUERY =
      "SELECT STARTTIME FROM billing_data WHERE ACCOUNTID = '%s' ORDER BY STARTTIME ASC LIMIT 1;";
  private static final String CE_CLUSTER_URL = "/account/%s/continuous-efficiency/cluster/insights?clusterList=%s";
  private static final String GET_CLUSTERS_QUERY =
      "SELECT CLUSTERID, CLUSTERNAME FROM billing_data_hourly WHERE ACCOUNTID = '%s' AND STARTTIME >= '%s' AND CLUSTERID IS NOT NULL GROUP BY CLUSTERID, CLUSTERNAME;";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    cloudToHarnessMappingService.upsertCEMetaDataRecord(CEMetadataRecord.builder().clusterDataConfigured(true).build());
    log.info("Running BillingDataGeneratedMailTasklet for accountId : {}", accountId);
    boolean notificationSend = notificationDao.isMailSent(accountId);
    if (!notificationSend) {
      long firstEventTime = getFirstDataRecordTime(accountId);
      long cutoffTime = getStartOfCurrentDay() - 4 * ONE_DAY_MILLIS;
      if (cutoffTime >= firstEventTime) {
        notificationSend = true;
        notificationDao.save(DataGeneratedNotification.builder().accountId(accountId).mailSent(true).build());
        log.info("Old account, accountId : {} , First event time : {}", accountId, firstEventTime);
      }
    }
    if (!notificationSend) {
      Map<String, String> clusters = getClusters(accountId);
      if (!clusters.isEmpty()) {
        List<CEUserInfo> users = getUsers(accountId);
        if (!users.isEmpty()) {
          sendMail(users, clusters, accountId);
          notificationDao.save(DataGeneratedNotification.builder().accountId(accountId).mailSent(true).build());
          log.info("Data generated mail sent, accountId : {}", accountId);
        } else {
          log.info("No users found in BillingDataGeneratedMailTasklet, accountId : {}", accountId);
        }
      }
    }
    return null;
  }

  // Map returns clusterId -> clusterName mapping
  private Map<String, String> getClusters(String accountId) throws SQLException {
    ResultSet resultSet = null;
    Map<String, String> clusters = new HashMap<>();
    boolean successful = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successful && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             Statement statement = dbConnection.createStatement()) {
          resultSet = statement.executeQuery(
              String.format(GET_CLUSTERS_QUERY, accountId, Instant.ofEpochMilli(getStartOfCurrentDay())));
          while (resultSet.next()) {
            clusters.put(resultSet.getString("CLUSTERID"), resultSet.getString("CLUSTERNAME"));
          }
          successful = true;
        } catch (SQLException e) {
          log.error("Failed to get clusters in BillingDataGeneratedMailTasklet, Exception: ", e);
          retryCount++;
        } finally {
          DBUtils.close(resultSet);
        }
      }
    } else {
      log.info("Not able to fetch clusters in BillingDataGeneratedMailTasklet");
    }
    return clusters;
  }

  // To obtain users who have CE_ADMIN permissions
  private List<CEUserInfo> getUsers(String accountId) {
    List<CEUserInfo> users = new ArrayList<>();
    List<UserGroup> userGroups = cloudToHarnessMappingService.listUserGroupsForAccount(accountId);
    List<UserGroup> ceAdminUserGroups =
        userGroups.stream()
            .filter(userGroup -> userGroup.getAccountPermissions().getPermissions().contains(PermissionType.CE_ADMIN))
            .collect(Collectors.toList());
    Set<String> userIds = new HashSet<>();
    ceAdminUserGroups.forEach(userGroup -> userIds.addAll(userGroup.getMemberIds()));

    userIds.forEach(id -> {
      User user = cloudToHarnessMappingService.getUser(id);
      users.add(CEUserInfo.builder().name(user.getName()).email(user.getEmail()).build());
    });
    return users;
  }

  private void sendMail(List<CEUserInfo> users, Map<String, String> clusters, String accountId) {
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("CLUSTERS", getClusterLinks(clusters, clusters.keySet(), accountId));
    try {
      templateModel.put(
          "EXPLORER_URL", emailNotificationService.buildAbsoluteUrl(String.format(CE_EXPLORER_URL, accountId)));
    } catch (URISyntaxException e) {
      log.error("Can't build explorer url : ", e);
    }
    users.forEach(user -> {
      templateModel.put("USER", user.getName());
      EmailData emailData = EmailData.builder()
                                .to(Collections.singletonList(user.getEmail()))
                                .templateName("ce_cluster_data_generated")
                                .templateModel(templateModel)
                                .accountId(accountId)
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(0);
      emailNotificationService.send(emailData);
    });
  }

  private String getClusterLinks(Map<String, String> clusterToClusterName, Set<String> clusters, String accountId) {
    StringJoiner joiner = new StringJoiner(", ");
    String htmlLinkTag =
        "<a href=\"%s\" target=\"_blank\" style=\"text-decoration: none;\"><span style=\"color: #00ade4\">%s</span></a>";

    for (String cluster : clusters) {
      String link = "";
      try {
        link = emailNotificationService.buildAbsoluteUrl(String.format(CE_CLUSTER_URL, accountId, cluster));
      } catch (URISyntaxException e) {
        log.error("Can't build cluster url : ", e);
      }
      joiner.add(String.format(htmlLinkTag, link, clusterToClusterName.get(cluster)));
    }
    return joiner.toString();
  }

  private long getFirstDataRecordTime(String accountId) {
    ResultSet resultSet = null;
    boolean successful = false;
    long time = getStartOfCurrentDay();
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successful && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             Statement statement = dbConnection.createStatement()) {
          resultSet = statement.executeQuery(String.format(FIRST_RECORD_TIME_QUERY, accountId));
          while (resultSet.next()) {
            time = resultSet.getTimestamp("STARTTIME", utils.getDefaultCalendar()).getTime();
          }
          successful = true;
        } catch (SQLException e) {
          log.error("Failed to get FirstDataRecordTime in BillingDataGeneratedMailTasklet, Exception: ", e);
          retryCount++;
        } finally {
          DBUtils.close(resultSet);
        }
      }
    } else {
      log.info("Not able to fetch FirstDataRecordTime in BillingDataGeneratedMailTasklet");
    }
    return time;
  }

  private long getStartOfCurrentDay() {
    ZoneId zoneId = ZoneId.of("GMT");
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.atStartOfDay(zoneId);
    return zdtStart.toEpochSecond() * 1000 - 2 * ONE_DAY_MILLIS;
  }
}