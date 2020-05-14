package io.harness.rule;

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
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class OwnerRule implements TestRule {
  @Getter(lazy = true) private static final JiraClient jira = connect();
  public static final String ASSIGNEE = "assignee";
  public static final String SUMMARY = "summary";
  public static final String NEEDS_FIXING = "needs fixing";
  public static final String COMPONENTS = "components";
  public static final String PRIORITY = "priority";
  public static final String PRIORITY_VALUE0 = "P0";
  public static final String PRIORITY_VALUE1 = "P1";
  public static final String DESCRIPTION = "description";
  public static final String DESCRIPTION_VALUE =
      "This is auto generated jira issue for tracking the fix of the test in the title.";

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

  public static final String GHPRB_PULL_AUTHOR_EMAIL = "ghprbPullAuthorEmail";

  public static final String PLATFORM = "PL";
  public static final String CONTINUOUS_DEPLOYMENT_PLATFORM = "CDP";
  public static final String CONTINUOUS_DEPLOYMENT_CORE = "CDC";
  public static final String CONTINUOUS_VERIFICATION = "CV";
  public static final String CONTINUOUS_EFFICIENCY = "CE";
  public static final String CONTINUOUS_INTEGRATION = "CI";
  public static final String DEVELOPER_EXPERIENCE = "DX";
  public static final String SWAT = "SWAT";

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
  public static final String IGOR = "igor.gere";
  public static final String JUHI = "juhi.agrawal";
  public static final String KAMAL = "kamal.joshi";
  public static final String MARKO = "marko.barjaktarovic";
  public static final String MEENAKSHI = "meenakshi.raikwar";
  public static final String MEHUL = "mehul.kasliwal";
  public static final String MOHIT = "mohit.kurani";
  public static final String NATARAJA = "nataraja";
  public static final String NIKOLA = "nikola.obucina";
  public static final String PARNIAN = "parnian";
  public static final String POOJA = "pooja";
  public static final String PRANJAL = "pranjal";
  public static final String PRASHANT = "prashant.pal";
  public static final String PRAVEEN = "praveen.sugavanam";
  public static final String PUNEET = "puneet.saraswat";
  public static final String RAGHU = "raghu";
  public static final String RAMA = "rama";
  public static final String RAUNAK = "raunak.agrawal";
  public static final String ROHIT = "rohit.reddy";
  public static final String ROHIT_KUMAR = "rohit.kumar";
  public static final String ROHITKARELIA = "rohit.karelia";
  public static final String RUSHABH = "rushabh.shah";
  public static final String SHASWAT = "shaswat.deep";
  public static final String SHIVAKUMAR = "shivakumar.ningappa";
  public static final String SHUBHANSHU = "shubhanshu.verma";
  public static final String SOWMYA = "sowmya.k";
  public static final String SRINIVAS = "srinivas";
  public static final String SRIRAM = "sriram";
  public static final String SATYAM = "satyam.shanker";
  public static final String UJJAWAL = "ujjawal.prasad";
  public static final String UTKARSH = "utkarsh.gupta";
  public static final String VAIBHAV_SI = "vaibhav.si";
  public static final String VENKATESH = "venkatesh.kotrike";
  public static final String VIKAS = "vikas.naiyar";
  public static final String VUK = "vuk.skobalj";
  public static final String YOGESH = "yogesh.chauhan";
  public static final String VARDAN_BANSAL = "vardan.bansal";
  public static final String NANDAN = "nandan.chandrashekar";
  public static final String RIHAZ = "rihaz.zahir";
  public static final String PRABU = "prabu.rajendran";
  public static final String PHOENIKX = "nikhil.ranjan";
  public static final String SHUBHAM = "shubham.agrawal";
  public static final String ACASIAN = "alexandru.casian";
  public static final String ANIL = "anil.chowdhury";
  public static final String VGLIJIN = "vasile.glijin";
  @Deprecated public static final String UNKNOWN = "unknown";

  private static DevInfoBuilder defaultDevInfo(String user) {
    return DevInfo.builder().email(user + "@harness.io").jira(user);
  }

  private static final Map<String, DevInfo> active =
      ImmutableMap.<String, DevInfo>builder()
          .put(AADITI, defaultDevInfo(AADITI).slack("UCFPUNRAQ").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(ABHINAV, defaultDevInfo(ABHINAV).slack("UQQPR8M6Y").team(DEVELOPER_EXPERIENCE).build())
          .put(AMAN, defaultDevInfo(AMAN).slack("UDJG47CHF").team(PLATFORM).build())
          .put(ANKIT, defaultDevInfo(ANKIT).slack("UF76W0NN5").team(PLATFORM).build())
          .put(ANSHUL, defaultDevInfo(ANSHUL).slack("UASUA3E65").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ANUBHAW, defaultDevInfo(ANUBHAW).slack("U0Z1U0HNW").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(AVMOHAN, defaultDevInfo(AVMOHAN).slack("UK72UTBJR").team(CONTINUOUS_EFFICIENCY).build())
          .put(BRETT, defaultDevInfo(BRETT).slack("U40VBHCGH").team(SWAT).build())
          .put(DEEPAK, defaultDevInfo(DEEPAK).slack("UK9EKBKQS").team(DEVELOPER_EXPERIENCE).build())
          .put(DINESH, DevInfo.builder().email("dinesh.garg@harness.io").slack("UQ0DMQG11").build())
          .put(GARVIT, defaultDevInfo(GARVIT).slack("UHH98EXDK").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(GEORGE, defaultDevInfo(GEORGE).slack("U88CA877V").team(PLATFORM).build())
          .put(HANTANG, defaultDevInfo(HANTANG).slack("UK8AQJSCS").team(CONTINUOUS_EFFICIENCY).build())
          .put(HARSH, defaultDevInfo(HARSH).slack("UJ1CDM3FY").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(HITESH, defaultDevInfo(HITESH).slack("UK41C9QJH").team(CONTINUOUS_EFFICIENCY).build())
          .put(IGOR, defaultDevInfo(IGOR).slack("U0104P0SC03").team(DEVELOPER_EXPERIENCE).build())
          .put(JUHI, defaultDevInfo(JUHI).slack("UL1KX4K1S").build())
          .put(KAMAL, defaultDevInfo(KAMAL).slack("UKFQ1PQBH").team(CONTINUOUS_VERIFICATION).build())
          .put(MARKO, defaultDevInfo(MARKO).slack("UVDT91N9W").team(PLATFORM).build())
          .put(MEENAKSHI, DevInfo.builder().email("meenakshi.raikwar@harness.io").slack("UKP2AEUNA").build())
          .put(MEHUL, defaultDevInfo(MEHUL).slack("URYP18AHX").team(PLATFORM).build())
          .put(MOHIT, defaultDevInfo(MOHIT).slack("USB6NTE22").team(PLATFORM).build())
          .put(NATARAJA, defaultDevInfo(NATARAJA).slack("UDQAS9J5C").team(PLATFORM).build())
          .put(NIKOLA, defaultDevInfo(NIKOLA).slack("U011CFJ4YDV").team(PLATFORM).build())
          .put(PARNIAN, defaultDevInfo(PARNIAN).slack("U89A5MLQK").team(CONTINUOUS_VERIFICATION).build())
          .put(POOJA, defaultDevInfo(POOJA).slack("UDDA9L0D6").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(PRANJAL, defaultDevInfo(PRANJAL).slack("UBV049Q5B").team(SWAT).build())
          .put(PRASHANT, defaultDevInfo(PRASHANT).slack("UJLBB7ULT").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(PRAVEEN, defaultDevInfo(PRAVEEN).slack("UAQH9QHSB").team(CONTINUOUS_VERIFICATION).build())
          .put(PUNEET, defaultDevInfo(PUNEET).slack("U8PMB1XKM").team(CONTINUOUS_EFFICIENCY).build())
          .put(RAGHU, defaultDevInfo(RAGHU).slack("U4Z2PG2TD").team(CONTINUOUS_VERIFICATION).build())
          .put(RAUNAK, defaultDevInfo(RAUNAK).slack("UU06GNQ0M").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(RAMA, defaultDevInfo(RAMA).slack("U69BLRG72").team(DEVELOPER_EXPERIENCE).build())
          .put(ROHIT, defaultDevInfo(ROHIT).slack("UKLSUUCAC").team(CONTINUOUS_EFFICIENCY).build())
          .put(ROHIT_KUMAR, defaultDevInfo(ROHIT_KUMAR).slack("UL92UJN4S").team(DEVELOPER_EXPERIENCE).build())
          .put(ROHITKARELIA, defaultDevInfo(ROHITKARELIA).slack("UP48HU3T9").team(SWAT).build())
          .put(RUSHABH, defaultDevInfo(RUSHABH).slack("U8M736D36").team(PLATFORM).jira("rushabh").build())
          .put(ADWAIT, defaultDevInfo(ADWAIT).slack("U8PL7JRMG").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(SATYAM, defaultDevInfo(SATYAM).slack("U9Z3R0GL8").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ANIL, defaultDevInfo(ANIL).slack("U0132ESPZ08").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(SHASWAT, DevInfo.builder().email("shaswat.deep@harness.io").slack("UL9J5EH7A").build())
          .put(SHIVAKUMAR,
              defaultDevInfo(SHIVAKUMAR)
                  .slack("U01124VC43C")
                  .team(CONTINUOUS_INTEGRATION)
                  .jira("5e875b7bcb85aa0c1471748c")
                  .build())
          .put(SHUBHANSHU, defaultDevInfo(SHUBHANSHU).slack("UKLTRSAN9").team(CONTINUOUS_EFFICIENCY).build())
          .put(SOWMYA, defaultDevInfo(SOWMYA).slack("UHM19HBKM").team(CONTINUOUS_VERIFICATION).build())
          .put(SRINIVAS, defaultDevInfo(SRINIVAS).slack("U4QC23961").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(SRIRAM, defaultDevInfo(SRIRAM).slack("U5L475PK5").team(CONTINUOUS_VERIFICATION).build())
          .put(UJJAWAL, defaultDevInfo(UJJAWAL).slack("UKLSV01DW").team(PLATFORM).build())
          .put(UTKARSH, defaultDevInfo(UTKARSH).slack("UKGF0UL58").team(PLATFORM).build())
          .put(VAIBHAV_SI, defaultDevInfo(VAIBHAV_SI).slack("UCK76T36U").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(VENKATESH, DevInfo.builder().email("venkatesh.kotrike@harness.io").slack("UGF55UEHF").build())
          .put(VIKAS, defaultDevInfo(VIKAS).slack("UE7M4CNMA").team(PLATFORM).build())
          .put(VUK, defaultDevInfo(VUK).slack("U0115RT3EQL").team(PLATFORM).build())
          .put(YOGESH, defaultDevInfo(YOGESH).slack("UJVLUUXAT").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(VARDAN_BANSAL, defaultDevInfo(VARDAN_BANSAL).slack("UH8NYAAUU").team(DEVELOPER_EXPERIENCE).build())
          .put(NANDAN, defaultDevInfo(NANDAN).slack("UKMS5KCBS").team(CONTINUOUS_VERIFICATION).build())
          .put(RIHAZ, defaultDevInfo(RIHAZ).slack("USUP66518").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(PRABU, defaultDevInfo(PRABU).slack("UTF8GHZEK").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(PHOENIKX, defaultDevInfo(PHOENIKX).slack("URWLPPJ8Z").team(PLATFORM).build())
          .put(SHUBHAM,
              defaultDevInfo(SHUBHAM)
                  .slack("UTRFKPL57")
                  .team(CONTINUOUS_INTEGRATION)
                  .jira("5e47234c2a59dc0c8fe4ccb8")
                  .build())
          .put(ACASIAN, defaultDevInfo(ACASIAN).slack("U012UEVAPAR").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(VGLIJIN, defaultDevInfo(VGLIJIN).slack("U012W2S777V").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(UNKNOWN, DevInfo.builder().email("n/a").slack("channel").build())
          .build();

  private static String prDeveloperId = findDeveloperId(System.getenv(GHPRB_PULL_AUTHOR_EMAIL));

  @Override
  public Statement apply(Statement statement, Description description) {
    Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      throw new CategoryConfigException("Owner annotation is obligatory.");
    }

    Ignore ignore = description.getAnnotation(Ignore.class);
    if (System.getenv("SONAR_TOKEN") != null) {
      if (owner.intermittent()) {
        checkForJira(description.getDisplayName(), owner.developers()[0], PRIORITY_VALUE1);
      }

      if (ignore != null) {
        checkForJira(description.getDisplayName(), owner.developers()[0], PRIORITY_VALUE1);
      }
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

  public static DevInfo findDeveloper(String developerId) {
    return active.get(developerId);
  }

  public static String findDeveloperId(String email) {
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
            + " AND %s ~ \"%s\""
            + " AND %s ~ \"%s\"",
        SUMMARY, test, SUMMARY, NEEDS_FIXING);
  }

  public static void checkForJira(String test, String developer, String priority) {
    if (!"true".equals(System.getenv("ENABLE_TEST_JIRA_REPORTS"))) {
      return;
    }

    DevInfo devInfo = active.get(developer);
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

        Issue issue = generateJiraCreate(test, devInfo, priority).execute();
        logger.info("New jira issue was created {}", issue.getKey());
        return;
      }

      Issue issue = searchResult.issues.get(0);

      if (!issue.getProject().getKey().equals(devInfo.getTeam())) {
        // We cannot automatically move an issue from one project to another.
        // Instead we are going to mark the current one as rejected.
        // Next time we would not find it and we will create a new one.

        // First lets set Bug Resolution to Ownership changed
        issue.update().field("customfield_10687", "Ownership changed").execute();

        issue.transition().execute("Rejected");
        return;
      }

      if (devInfo.getJira() != null && !issue.getAssignee().getEmail().equals(devInfo.getEmail())) {
        issue.update().field(ASSIGNEE, devInfo.getJira()).execute();
      }

      if (priority.compareTo(issue.getPriority().getName()) > 0) {
        issue.update().field(PRIORITY, priority).execute();
      }

    } catch (JiraException e) {
      logger.error("Failed when checking the jira issue", e);
    }
  }

  private static Issue.FluentCreate generateJiraCreate(String test, DevInfo devInfo, String priority)
      throws JiraException {
    return getJira()
        .createIssue(devInfo.getTeam(), "Bug")
        .field(ASSIGNEE, devInfo.getJira())
        .field(SUMMARY, test + " " + NEEDS_FIXING)
        .field(PRIORITY, priority)
        .field(DESCRIPTION, DESCRIPTION_VALUE);
  }

  public static void fileOwnerAs(String developer, String type) {
    logger.info("Developer {} is found to be owner of {} test", developer, type);

    DevInfo devInfo = active.get(developer);
    if (devInfo == null) {
      return;
    }

    String identify = devInfo.getSlack() == null ? developer : "<@" + devInfo.getSlack() + ">";

    try {
      File file = new File(format("%s/owners/%s/%s", System.getProperty("java.io.tmpdir"), type, identify));

      file.getParentFile().mkdirs();
      if (!file.createNewFile()) {
        logger.debug("The owner {} was already set", identify);
      }
    } catch (Exception ignore) {
      // Ignore the exceptions
    }
  }
}
