package io.harness.rule;

import static ch.qos.logback.core.util.OptionHelper.getEnv;
import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;

import io.harness.NoopStatement;
import io.harness.exception.CategoryConfigException;
import io.harness.rule.DevInfo.DevInfoBuilder;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.junit.Ignore;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class OwnerRule implements TestRule {
  @Getter(lazy = true) private static final JiraClient jira = connect();

  private static JiraClient connect() {
    ScmSecret scmSecret = new ScmSecret();
    String jiraTestUser = scmSecret.decryptToString(SecretName.builder().value("jira_test_user").build());
    String jiraTestPassword = scmSecret.decryptToString(SecretName.builder().value("jira_test_password").build());
    BasicCredentials credentials = new BasicCredentials(jiraTestUser, jiraTestPassword);
    try {
      return new JiraClient("https://harness.atlassian.net", credentials);
    } catch (JiraException e) {
      logger.error("Failed to connect to jira", e);
    }

    return null;
  }

  public static final String PLATFORM = "PL";
  public static final String CONTINUOUS_DEPLOYMENT_PLATFORM = "CD Platform";
  public static final String CONTINUOUS_VERIFICATION = "CV";
  public static final String CONTINUOUS_EFFICIENCY = "CE";

  public static final String AADITI = "aaditi.joag";
  public static final String ABHINAV = "abhinav.singh";
  public static final String ADWAIT = "adwait.bhandare";
  public static final String AMAN = "aman.singh";
  public static final String ANKIT = "ankit.singhal";
  public static final String ANSHUL = "anshul";
  public static final String ANUBHAW = "anubhaw";
  public static final String AVMOHAN = "abhijith.mohan";
  public static final String BRETT = "brett";
  public static final String DEEPAK = "deepak.patankar";
  public static final String DINESH = "dinesh.garg";
  public static final String GARVIT = "garvit.pahal";
  public static final String GEORGE = "george";
  public static final String HANTANG = "hannah.tang";
  public static final String HARSH = "harsh.jain";
  public static final String HITESH = "hitesh.aringa";
  public static final String JUHI = "juhi.agrawal";
  public static final String KAMAL = "kamal.joshi";
  public static final String MEENAKSHI = "meenakshi.raikwar";
  public static final String NATARAJA = "nataraja";
  public static final String PARNIAN = "parnian";
  public static final String POOJA = "pooja";
  public static final String PRANJAL = "pranjal";
  public static final String PRASHANT = "prashant";
  public static final String PRAVEEN = "praveen.sugavanam";
  public static final String PUNEET = "puneet.saraswat";
  public static final String RAGHU = "raghu";
  public static final String RAMA = "rama";
  public static final String ROHIT = "rohit.reddy";
  public static final String ROHIT_KUMAR = "rohit.kumar";
  public static final String ROHITKARELIA = "rohit.karelia";
  public static final String RUSHABH = "rushabh";
  public static final String SHASWAT = "shaswat.deep";
  public static final String SHUBHANSHU = "shubhanshu.verma";
  public static final String SOWMYA = "sowmya.k";
  public static final String SRINIVAS = "srinivas";
  public static final String SRIRAM = "sriram";
  public static final String SATYAM = "satyam";
  public static final String UJJAWAL = "ujjawal.prasad";
  public static final String UTKARSH = "utkarsh.gupta";
  public static final String VAIBHAV_SI = "vaibhav.si";
  public static final String VENKATESH = "venkatesh.kotrike";
  public static final String VIKAS = "vikas.naiyar";
  public static final String YOGESH_CHAUHAN = "yogesh.chauhan";
  public static final String VARDAN_BANSAL = "vardan.bansal";
  public static final String NANDAN = "nandan.chandrashekar";
  @Deprecated public static final String UNKNOWN = "unknown";

  private static DevInfoBuilder defaultDevInfo(String user) {
    return DevInfo.builder().email(user + "@harness.io").jira(user);
  }

  private static final Map<String, DevInfo> active =
      ImmutableMap.<String, DevInfo>builder()
          .put(AADITI, DevInfo.builder().email("aaditi.joag@harness.io").slack("UCFPUNRAQ").build())
          .put(ABHINAV, DevInfo.builder().email("abhinav.singh@harness.io").slack("UQQPR8M6Y").build())
          .put(ADWAIT, DevInfo.builder().email("adwait.bhandare@harness.io").slack("U8PL7JRMG").build())
          .put(AMAN, DevInfo.builder().email("aman.singh@harness.io").slack("UDJG47CHF").build())
          .put(ANKIT, DevInfo.builder().email("ankit.singhal@harness.io").slack("UF76W0NN5").build())
          .put(ANSHUL, defaultDevInfo(ANSHUL).slack("UASUA3E65").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ANUBHAW, DevInfo.builder().email("anubhaw@harness.io").slack("U0Z1U0HNW").build())
          .put(AVMOHAN, defaultDevInfo(AVMOHAN).slack("UK72UTBJR").team(CONTINUOUS_EFFICIENCY).build())
          .put(BRETT, DevInfo.builder().email("brett@harness.io").slack("U40VBHCGH").build())
          .put(DEEPAK, DevInfo.builder().email("deepak.patankar@harness.io").slack("UK9EKBKQS").build())
          .put(DINESH, DevInfo.builder().email("dinesh.garg@harness.io").slack("UQ0DMQG11").build())
          .put(GARVIT, DevInfo.builder().email("garvit.pahal@harness.io").slack("UHH98EXDK").build())
          .put(GEORGE, defaultDevInfo(GEORGE).slack("U88CA877V").team(PLATFORM).build())
          .put(HANTANG, DevInfo.builder().email("hannah.tang@harness.io").slack("UK8AQJSCS").build())
          .put(HARSH, DevInfo.builder().email("harsh.jain@harness.io").slack("UJ1CDM3FY").build())
          .put(HITESH, DevInfo.builder().email("hitesh.aringa@harness.io").slack("UK41C9QJH").build())
          .put(JUHI, DevInfo.builder().email("juhi.agrawal@harness.io").slack("UL1KX4K1S").build())
          .put(KAMAL, DevInfo.builder().email("kamal.joshi@harness.io").slack("UKFQ1PQBH").build())
          .put(MEENAKSHI, DevInfo.builder().email("meenakshi.raikwar@harness.io").slack("UKP2AEUNA").build())
          .put(NATARAJA, defaultDevInfo(NATARAJA).slack("UDQAS9J5C").team(PLATFORM).build())
          .put(PARNIAN, DevInfo.builder().email("parnian@harness.io").slack("U89A5MLQK").build())
          .put(POOJA, DevInfo.builder().email("pooja@harness.io").slack("UDDA9L0D6").build())
          .put(PRANJAL, DevInfo.builder().email("pranjal@harness.io").slack("UBV049Q5B").build())
          .put(PRASHANT, DevInfo.builder().email("prashant.pal@harness.io").slack("UJLBB7ULT").build())
          .put(PRAVEEN, DevInfo.builder().email("praveen.sugavanam@harness.io").slack("UAQH9QHSB").build())
          .put(PUNEET, DevInfo.builder().email("puneet.saraswat@harness.io").slack("U8PMB1XKM").build())
          .put(RAGHU, defaultDevInfo(RAGHU).slack("U4Z2PG2TD").team(CONTINUOUS_VERIFICATION).build())
          .put(RAMA, DevInfo.builder().email("rama@harness.io").slack("U69BLRG72").build())
          .put(ROHIT, DevInfo.builder().email("rohit.reddy@harness.io").slack("UKLSUUCAC").build())
          .put(ROHIT_KUMAR, DevInfo.builder().email("rohit.kumar@harness.io").slack("UL92UJN4S").build())
          .put(ROHITKARELIA, DevInfo.builder().email("rohit.karelia@harness.io").slack("UP48HU3T9").build())
          .put(RUSHABH, DevInfo.builder().email("rushabh@harness.io").slack("U8M736D36").build())
          .put(SATYAM, DevInfo.builder().email("satyam.shanker@harness.io").slack("U9Z3R0GL8").build())
          .put(SHASWAT, DevInfo.builder().email("shaswat.deep@harness.io").slack("UL9J5EH7A").build())
          .put(SHUBHANSHU, DevInfo.builder().email("shubhanshu.verma@harness.io").slack("UKLTRSAN9").build())
          .put(SOWMYA, DevInfo.builder().email("sowmya.k@harness.io").slack("UHM19HBKM").build())
          .put(SRINIVAS, DevInfo.builder().email("srinivas@harness.io").slack("U4QC23961").build())
          .put(SRIRAM, DevInfo.builder().email("sriram@harness.io").slack("U5L475PK5").build())
          .put(UJJAWAL, DevInfo.builder().email("ujjawal.prasad@harness.io").slack("UKLSV01DW").build())
          .put(UTKARSH, DevInfo.builder().email("utkarsh.gupta@harness.io").slack("UKGF0UL58").build())
          .put(VAIBHAV_SI, DevInfo.builder().email("vaibhav.si@harness.io").slack("UCK76T36U").build())
          .put(VENKATESH, DevInfo.builder().email("venkatesh.kotrike@harness.io").slack("UGF55UEHF").build())
          .put(VIKAS, DevInfo.builder().email("vikas.naiyar@harness.io").slack("UE7M4CNMA").build())
          .put(YOGESH_CHAUHAN, DevInfo.builder().email("yogesh.chauhan@harness.io").slack("UJVLUUXAT").build())
          .put(VARDAN_BANSAL, DevInfo.builder().email("vardan.bansal@harness.io").slack("UH8NYAAUU").build())
          .put(NANDAN, DevInfo.builder().email("nandan.chandrashekar@harness.io").slack("UKMS5KCBS").build())
          .put(UNKNOWN, DevInfo.builder().email("n/a").slack("channel").build())
          .build();

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Owner {
    String[] developers();

    boolean intermittent() default false;
  }

  private static String prDeveloperId = findDeveloperId(System.getenv("ghprbPullAuthorEmail"));

  @Override
  public Statement apply(Statement statement, Description description) {
    Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      throw new CategoryConfigException("Owner annotation is obligatory.");
    }

    Ignore ignore = description.getAnnotation(Ignore.class);
    if (owner.intermittent()) {
      checkForJira(description.getDisplayName(), owner.developers()[0]);
    }

    if (ignore != null) {
      checkForJira(description.getDisplayName(), owner.developers()[0]);
    }

    for (String developer : owner.developers()) {
      if (!active.containsKey(developer)) {
        throw new CategoryConfigException(format("Developer %s is not active.", developer));
      }

      if (owner.intermittent()) {
        fileOwnerAs(developer, "intermittent");
      }

      if (ignore != null) {
        fileOwnerAs(developer, "ignore");
      }
    }

    if (prDeveloperId == null || !Arrays.asList(owner.developers()).contains(prDeveloperId)) {
      if (owner.intermittent()) {
        return new NoopStatement();
      }
    }

    return statement;
  }

  private static String findDeveloperId(String email) {
    if (email == null) {
      return null;
    }

    for (Entry<String, DevInfo> entry : active.entrySet()) {
      if (entry.getValue().getEmail().equals(email)) {
        return entry.getKey();
      }
    }

    return null;
  }

  private static String generateJQL(String test) {
    return format("type = Bug"
            + " AND statusCategory != Done"
            + " AND summary ~ \"%s\""
            + " AND summary ~ \"needs fixing\"",
        test);
  }

  public static void checkForJira(String test, String developer) {
    if (getEnv("SONAR_TOKEN") == null) {
      return;
    }

    final DevInfo devInfo = active.get(developer);
    if (devInfo == null) {
      return;
    }

    try {
      JiraClient jira = getJira();
      String jql = generateJQL(test);
      Issue.SearchResult searchResult = jira.searchIssues(jql, 1);
      if (searchResult.total == 0) {
        if (devInfo.getJira() == null || devInfo.getTeam() == null) {
          return;
        }

        Issue issue = generateJiraCreate(test, devInfo).execute();
        logger.info("New jira issue was created {}", issue.getKey());
        return;
      }

      Issue issue = searchResult.issues.get(0);

      if (!issue.getProject().getKey().equals(jiraProject(devInfo))) {
        // We cannot automatically move an issue from one project to another.
        // Instead we are going to mark the current one as rejected.
        // Next time we would not find it and we will create a new one.

        // First lets set Bug Resolution to Ownership changed
        issue.update().field("customfield_10687", "Ownership changed").execute();

        issue.transition().execute("Rejected");
        return;
      }
    } catch (JiraException e) {
      logger.error("Failed to check the jira issue", e);
    }
  }

  private static String jiraProject(DevInfo devInfo) {
    if (devInfo.getTeam() == null) {
      return null;
    }
    return devInfo.getTeam().split(" ")[0];
  }

  private static String jiraComponent(DevInfo devInfo) {
    if (devInfo.getTeam() == null) {
      return null;
    }
    if (devInfo.getTeam().split(" ").length == 1) {
      return null;
    }
    return devInfo.getTeam();
  }

  private static Issue.FluentCreate generateJiraCreate(String test, DevInfo devInfo) throws JiraException {
    Issue.FluentCreate create = getJira()
                                    .createIssue(jiraProject(devInfo), "Bug")
                                    .field("assignee", devInfo.getJira())
                                    .field("summary", test + " needs fixing")
                                    .field("priority", "P1");

    String jiraComponent = jiraComponent(devInfo);
    if (jiraComponent != null) {
      List<String> list = new ArrayList();
      list.add(jiraComponent);
      create.field("components", list);
    }
    return create;
  }

  public static void fileOwnerAs(String developer, String type) {
    logger.info("Developer {} is found to be owner of {} test", developer, type);

    final DevInfo devInfo = active.get(developer);
    if (devInfo == null) {
      return;
    }

    String identify = devInfo.getSlack() == null ? developer : "<@" + devInfo.getSlack() + ">";

    try {
      final File file = new File(format("%s/owners/%s/%s", System.getProperty("java.io.tmpdir"), type, identify));

      file.getParentFile().mkdirs();
      if (!file.createNewFile()) {
        logger.debug("The owner {} was already set", identify);
      }
    } catch (Exception ignore) {
      // Ignore the exceptions
    }
  }
  }