package io.harness.batch.processing.billing.tasklet;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.tasklet.dao.intfc.DataGeneratedNotificationDao;
import io.harness.batch.processing.billing.tasklet.entities.DataGeneratedNotification;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.mail.CEMailNotificationService;
import io.harness.ccm.cluster.entities.CEUserInfo;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.helpers.ext.mail.EmailData;
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
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Singleton
public class BillingDataGeneratedMailTasklet implements Tasklet {
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private DataGeneratedNotificationDao notificationDao;
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;
  @Autowired private CEMailNotificationService emailNotificationService;
  private JobParameters parameters;

  private static final int MAX_RETRY_COUNT = 3;
  private static final long ONE_DAY_MILLIS = 86400000;
  private static final String CE_EXPLORER_URL = "/account/%s/continuous-efficiency/cluster/insights";
  private static final String FIRST_RECORD_TIME_QUERY =
      "SELECT STARTTIME FROM billing_data WHERE ACCOUNTID = '%s' ORDER BY STARTTIME ASC LIMIT 1;";
  private static final String CE_CLUSTER_URL =
      "/account/%s/continuous-efficiency/cluster/insights?aggregationType=DAY&chartType=column&clusterList=%s&currentView=TOTAL_COST&groupBy=Cluster&isFilterOn=true&showOthers=false&showUnallocated=false&utilizationAggregationType=Average";
  private static final String GET_CLUSTERS_QUERY =
      "SELECT CLUSTERID, CLUSTERNAME FROM billing_data_hourly WHERE ACCOUNTID = '%s' AND STARTTIME >= '%s' AND CLUSTERID IS NOT NULL GROUP BY CLUSTERID, CLUSTERNAME;";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    boolean notificationSend = notificationDao.isMailSent(accountId);
    if (!notificationSend) {
      long firstEventTime = getFirstDataRecordTime(accountId);
      long cutoffTime = getStartOfCurrentDay() - ONE_DAY_MILLIS;
      if (cutoffTime >= firstEventTime) {
        notificationSend = true;
        notificationDao.save(DataGeneratedNotification.builder().accountId(accountId).mailSent(true).build());
      }
    }
    if (!notificationSend) {
      notificationDao.save(DataGeneratedNotification.builder().accountId(accountId).mailSent(true).build());
      Map<String, String> clusters = getClusters(accountId);
      List<CEUserInfo> users = getUsers(clusters);
      sendMail(users, clusters, accountId);
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
          logger.error("Failed to get clusters in BillingDataGeneratedMailTasklet, Exception: ", e);
          retryCount++;
        } finally {
          DBUtils.close(resultSet);
        }
      }
    } else {
      logger.info("Not able to fetch clusters in BillingDataGeneratedMailTasklet");
    }
    return clusters;
  }

  // To obtain users who enabled given clusters, returns a map of user email to list of clusters that he enabled
  private List<CEUserInfo> getUsers(Map<String, String> clusters) {
    Map<String, CEUserInfo> users = new HashMap<>();
    clusters.keySet().forEach(clusterId -> {
      CEUserInfo userInfo = cloudToHarnessMappingService.getUserForCluster(clusterId);
      if (userInfo != null) {
        String email = userInfo.getEmail();
        if (!users.containsKey(email)) {
          users.put(email,
              CEUserInfo.builder().email(email).name(userInfo.getName()).clustersEnabled(new ArrayList<>()).build());
        }
        users.get(email).getClustersEnabled().add(clusterId);
      }
    });

    return new ArrayList<>(users.values());
  }

  private void sendMail(List<CEUserInfo> users, Map<String, String> clusters, String accountId) {
    users.forEach(user -> {
      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("USER", user.getName());
      templateModel.put("CLUSTERS", getClusterLinks(clusters, user.getClustersEnabled(), accountId));
      try {
        templateModel.put(
            "EXPLORER_URL", emailNotificationService.buildAbsoluteUrl(String.format(CE_EXPLORER_URL, accountId)));
      } catch (URISyntaxException e) {
        logger.error("Can't build explorer url : ", e);
      }
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

  private String getClusterLinks(Map<String, String> clusterToClusterName, List<String> clusters, String accountId) {
    StringJoiner joiner = new StringJoiner(", ");
    String htmlLinkTag =
        "<a href=\"%s\" target=\"_blank\" style=\"text-decoration: none;\"><span style=\"color: #00ade4\">%s</span></a>";

    for (String cluster : clusters) {
      String link = "";
      try {
        link = emailNotificationService.buildAbsoluteUrl(String.format(CE_CLUSTER_URL, accountId, cluster));
      } catch (URISyntaxException e) {
        logger.error("Can't build cluster url : ", e);
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
          logger.error("Failed to get FirstDataRecordTime in BillingDataGeneratedMailTasklet, Exception: ", e);
          retryCount++;
        } finally {
          DBUtils.close(resultSet);
        }
      }
    } else {
      logger.info("Not able to fetch FirstDataRecordTime in BillingDataGeneratedMailTasklet");
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