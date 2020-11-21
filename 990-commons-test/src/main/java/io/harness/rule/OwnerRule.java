package io.harness.rule;

import static java.lang.String.format;

import io.harness.NoopStatement;
import io.harness.exception.CategoryConfigException;
import io.harness.rule.UserInfo.UserInfoBuilder;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
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

@Slf4j
public class OwnerRule implements TestRule {
  @Getter(lazy = true) private static final JiraClient jira = connect();
  public static final String ASSIGNEE = "assignee";
  public static final String SUMMARY = "summary";
  public static final String NEEDS_FIXING = "needs fixing";
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
      log.error("Failed to connect to jira", e);
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
  public static final String DELEGATE = "DEL";

  public static final String AADITI = "aaditi.joag";
  public static final String ABHINAV = "abhinav.singh";
  public static final String ADWAIT = "adwait.bhandare";
  public static final String ALEKSANDAR = "aleksandar.radisavljevic";
  public static final String AGORODETKI = "alexandr.gorodetki";
  public static final String ALEXEI = "alexei.stirbul";
  public static final String AMAN = "aman.singh";
  public static final String ANKIT = "ankit.singhal";
  public static final String ANKUSH = "ankush.shaw";
  public static final String ANSHUL = "anshul";
  public static final String ANUBHAW = "anubhaw";
  public static final String ARVIND = "arvind.choudhary";
  public static final String AVMOHAN = "abhijith.mohan";
  public static final String BRETT = "brett";
  public static final String BRIJESH = "brijesh.dhakar";
  public static final String DEEPAK = "deepak.patankar";
  public static final String DHRUV = "dhruv.upadhyay";
  public static final String DINESH = "dinesh.garg";
  public static final String GARVIT = "garvit.pahal";
  public static final String GEORGE = "george";
  public static final String GUNA = "guna.chandrasekaran";
  public static final String HANTANG = "hannah.tang";
  public static final String HARSH = "harsh.jain";
  public static final String HITESH = "hitesh.aringa";
  public static final String IGOR = "igor.gere";
  public static final String INDER = "inderpreet.chera";
  public static final String JUHI = "juhi.agrawal";
  public static final String KAMAL = "kamal.joshi";
  public static final String KARAN = "karan.siwach";
  public static final String MARKO = "marko.barjaktarovic";
  public static final String MATT = "matthew.lin";
  public static final String MEENAKSHI = "meenakshi.raikwar";
  public static final String MEHUL = "mehul.kasliwal";
  public static final String MILOS = "milos.paunovic";
  public static final String MOHIT = "mohit.kurani";
  public static final String NATARAJA = "nataraja";
  public static final String NEMANJA = "nemanja.lukovic";
  public static final String NIKOLA = "nikola.obucina";
  public static final String NIKUNJ = "nikunj.badjatya";
  public static final String PARNIAN = "parnian";
  public static final String POOJA = "pooja";
  public static final String PRANJAL = "pranjal";
  public static final String PRASHANT = "prashant.pal";
  public static final String PRASHANTSHARMA = "prashant.sharma";
  public static final String PRAVEEN = "praveen.sugavanam";
  public static final String PUNEET = "puneet.saraswat";
  public static final String RAGHU = "raghu";
  public static final String RAJ = "raj.patel";
  public static final String RAMA = "rama";
  public static final String RAUNAK = "raunak.agrawal";
  public static final String ROHIT = "rohit.reddy";
  public static final String ROHIT_KUMAR = "rohit.kumar";
  public static final String ROHITKARELIA = "rohit.karelia";
  public static final String RUSHABH = "rushabh.shah";
  public static final String SANDESH = "sandesh.katta";
  public static final String SANJA = "sanja.jokic";
  public static final String SANYASI_NAIDU = "sanyasi.naidu";
  public static final String SHASWAT = "shaswat.deep";
  public static final String SHIVAKUMAR = "shivakumar.ningappa";
  public static final String SHUBHANSHU = "shubhanshu.verma";
  public static final String SOWMYA = "sowmya.k";
  public static final String SRINIVAS = "srinivas";
  public static final String SRIRAM = "sriram";
  public static final String SATYAM = "satyam.shanker";
  public static final String UJJAWAL = "ujjawal.prasad";
  public static final String UTKARSH = "utkarsh.gupta";
  public static final String UTSAV = "utsav.krishnan";
  public static final String VAIBHAV_SI = "vaibhav.si";
  public static final String VENKATESH = "venkatesh.kotrike";
  public static final String VIKAS = "vikas.naiyar";
  public static final String VISTAAR = "vistaar.juneja";
  public static final String VOJIN = "vojin.djukic";
  public static final String VUK = "vuk.skobalj";
  public static final String YOGESH = "yogesh.chauhan";
  public static final String VARDAN_BANSAL = "vardan.bansal";
  public static final String NANDAN = "nandan.chandrashekar";
  public static final String RIHAZ = "rihaz.zahir";
  public static final String PRABU = "prabu.rajendran";
  public static final String PHOENIKX = "nikhil.ranjan";
  public static final String SHUBHAM = "shubham.agrawal";
  public static final String ABOSII = "alexandru.bosii";
  public static final String ACASIAN = "alexandru.casian";
  public static final String ANIL = "anil.chowdhury";
  public static final String VGLIJIN = "vasile.glijin";
  public static final String MILAN = "milan.balaban";
  public static final String RAGHVENDRA = "raghvendra.singh";
  public static final String ARCHIT = "archit.singla";
  public static final String IVAN = "ivan.mijailovic";
  public static final String SAHIL = "sahil.hindwani";
  public static final String BOJANA = "bojana.milovanovic";
  public static final String LAZAR = "lazar.matovic";
  public static final String HINGER = "abhinav.hinger";
  public static final String TATHAGAT = "tathagat.chaurasiya";
  public static final String TMACARI = "tudor.macari";
  public static final String NAMAN = "naman.verma";
  public static final String DEEPAK_PUTHRAYA = "deepak.puthraya";
  public static final String LUCAS = "lucas.mari";
  public static final String NICOLAS = "nicolas.bantar";

  @Deprecated public static final String UNKNOWN = "unknown";

  private static UserInfoBuilder defaultUserInfo(String user) {
    return UserInfo.builder().email(user + "@harness.io").jira(user);
  }

  private static final Map<String, UserInfo> active =
      ImmutableMap.<String, UserInfo>builder()
          .put(AADITI, defaultUserInfo(AADITI).slack("UCFPUNRAQ").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(ABHINAV, defaultUserInfo(ABHINAV).slack("UQQPR8M6Y").team(DEVELOPER_EXPERIENCE).build())
          .put(ALEKSANDAR, defaultUserInfo(ALEKSANDAR).slack("U012MKR5FUZ").team(CONTINUOUS_INTEGRATION).build())
          .put(AGORODETKI, defaultUserInfo(AGORODETKI).slack("U013KM8H2NL").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(ALEXEI, defaultUserInfo(ALEXEI).slack("U012VS112EN").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(AMAN, defaultUserInfo(AMAN).slack("UDJG47CHF").team(PLATFORM).build())
          .put(ANKIT, defaultUserInfo(ANKIT).slack("UF76W0NN5").team(PLATFORM).build())
          .put(ANKUSH, defaultUserInfo(ANKUSH).slack("U016VE8EP7X").team(PLATFORM).build())
          .put(ANSHUL, defaultUserInfo(ANSHUL).slack("UASUA3E65").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ANUBHAW, defaultUserInfo(ANUBHAW).slack("U0Z1U0HNW").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ARVIND, defaultUserInfo(ARVIND).slack("U01542TQGCU").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(AVMOHAN, defaultUserInfo(AVMOHAN).slack("UK72UTBJR").team(CONTINUOUS_EFFICIENCY).build())
          .put(BRETT, defaultUserInfo(BRETT).slack("U40VBHCGH").team(SWAT).build())
          .put(BRIJESH, defaultUserInfo(BRIJESH).slack("U015LRWS8KV").team(PLATFORM).build())
          .put(DEEPAK, defaultUserInfo(DEEPAK).slack("UK9EKBKQS").team(DEVELOPER_EXPERIENCE).build())
          .put(DHRUV, defaultUserInfo(DHRUV).slack("U012WHYL7G8").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(DINESH, UserInfo.builder().email("dinesh.garg@harness.io").slack("UQ0DMQG11").build())
          .put(GARVIT, defaultUserInfo(GARVIT).slack("UHH98EXDK").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(GEORGE, defaultUserInfo(GEORGE).slack("U88CA877V").team(DELEGATE).build())
          .put(GUNA, defaultUserInfo(GUNA).slack("UD45NV41W").team(DELEGATE).build())
          .put(HANTANG, defaultUserInfo(HANTANG).slack("UK8AQJSCS").team(CONTINUOUS_EFFICIENCY).build())
          .put(HARSH, defaultUserInfo(HARSH).slack("UJ1CDM3FY").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(HITESH, defaultUserInfo(HITESH).slack("UK41C9QJH").team(CONTINUOUS_EFFICIENCY).build())
          .put(IGOR, defaultUserInfo(IGOR).slack("U0104P0SC03").team(DEVELOPER_EXPERIENCE).build())
          .put(INDER, defaultUserInfo(INDER).slack("U0155TBCW7R").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(JUHI, defaultUserInfo(JUHI).slack("UL1KX4K1S").build())
          .put(KAMAL, defaultUserInfo(KAMAL).slack("UKFQ1PQBH").team(CONTINUOUS_VERIFICATION).build())
          .put(KARAN, defaultUserInfo(KARAN).slack("U015LRX21FD").team(PLATFORM).build())
          .put(LUCAS, defaultUserInfo(LUCAS).slack("U01C8MPTS2J").team(DELEGATE).build())
          .put(MARKO, defaultUserInfo(MARKO).slack("UVDT91N9W").team(DELEGATE).build())
          .put(MATT, defaultUserInfo(MATT).slack("U019QR7TA7M").team(DELEGATE).build())
          .put(MEENAKSHI, UserInfo.builder().email("meenakshi.raikwar@harness.io").slack("UKP2AEUNA").build())
          .put(MEHUL, defaultUserInfo(MEHUL).slack("URYP18AHX").team(PLATFORM).build())
          .put(MILOS, defaultUserInfo(MILOS).slack("U014WS8CXEV").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(MOHIT, defaultUserInfo(MOHIT).slack("USB6NTE22").team(PLATFORM).build())
          .put(NATARAJA, defaultUserInfo(NATARAJA).slack("UDQAS9J5C").team(PLATFORM).build())
          .put(NEMANJA, defaultUserInfo(NEMANJA).slack("U016F8DDQSC").team(CONTINUOUS_VERIFICATION).build())
          .put(NICOLAS, defaultUserInfo(NICOLAS).slack("U01C8MPQVQE").team(DELEGATE).build())
          .put(NIKOLA, defaultUserInfo(NIKOLA).slack("U011CFJ4YDV").team(PLATFORM).build())
          .put(NIKUNJ, defaultUserInfo(NIKUNJ).slack("U019JUP10AF").team(CONTINUOUS_EFFICIENCY).build())
          .put(PARNIAN, defaultUserInfo(PARNIAN).slack("U89A5MLQK").team(CONTINUOUS_VERIFICATION).build())
          .put(POOJA, defaultUserInfo(POOJA).slack("UDDA9L0D6").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(PRANJAL, defaultUserInfo(PRANJAL).slack("UBV049Q5B").team(SWAT).build())
          .put(PRASHANT, defaultUserInfo(PRASHANT).slack("UJLBB7ULT").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(PRAVEEN, defaultUserInfo(PRAVEEN).slack("UAQH9QHSB").team(CONTINUOUS_VERIFICATION).build())
          .put(PUNEET, defaultUserInfo(PUNEET).slack("U8PMB1XKM").team(CONTINUOUS_EFFICIENCY).build())
          .put(PRASHANTSHARMA, defaultUserInfo(PRASHANTSHARMA).slack("U015LRWKR6X").team(PLATFORM).build())
          .put(RAGHU, defaultUserInfo(RAGHU).slack("U4Z2PG2TD").team(CONTINUOUS_VERIFICATION).build())
          .put(RAJ, defaultUserInfo(RAJ).slack("U01CPMHBX27").team(PLATFORM).build())
          .put(RAUNAK, defaultUserInfo(RAUNAK).slack("UU06GNQ0M").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(RAMA, defaultUserInfo(RAMA).slack("U69BLRG72").team(DEVELOPER_EXPERIENCE).build())
          .put(ROHIT, defaultUserInfo(ROHIT).slack("UKLSUUCAC").team(CONTINUOUS_EFFICIENCY).build())
          .put(ROHIT_KUMAR, defaultUserInfo(ROHIT_KUMAR).slack("UL92UJN4S").team(DEVELOPER_EXPERIENCE).build())
          .put(ROHITKARELIA, defaultUserInfo(ROHITKARELIA).slack("UP48HU3T9").team(SWAT).build())
          .put(RUSHABH, defaultUserInfo(RUSHABH).slack("U8M736D36").team(PLATFORM).jira("rushabh").build())
          .put(ADWAIT, defaultUserInfo(ADWAIT).slack("U8PL7JRMG").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(SATYAM, defaultUserInfo(SATYAM).slack("U9Z3R0GL8").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ANIL, defaultUserInfo(ANIL).slack("U0132ESPZ08").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(
              RAGHVENDRA, defaultUserInfo(RAGHVENDRA).slack("U012F7A157Y").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(SANDESH, defaultUserInfo(SANDESH).slack("U015PLPSD47").team(CONTINUOUS_EFFICIENCY).build())
          .put(SANJA, defaultUserInfo(SANJA).slack("U015Q24465T").team(DELEGATE).build())
          .put(SANYASI_NAIDU, defaultUserInfo(SANYASI_NAIDU).slack("U012P5KH3RU").team(DEVELOPER_EXPERIENCE).build())
          .put(SHASWAT, UserInfo.builder().email("shaswat.deep@harness.io").slack("UL9J5EH7A").build())
          .put(SHIVAKUMAR,
              defaultUserInfo(SHIVAKUMAR)
                  .slack("U01124VC43C")
                  .team(CONTINUOUS_INTEGRATION)
                  .jira("5e875b7bcb85aa0c1471748c")
                  .build())
          .put(SHUBHANSHU, defaultUserInfo(SHUBHANSHU).slack("UKLTRSAN9").team(CONTINUOUS_EFFICIENCY).build())
          .put(SOWMYA, defaultUserInfo(SOWMYA).slack("UHM19HBKM").team(CONTINUOUS_VERIFICATION).build())
          .put(SRINIVAS, defaultUserInfo(SRINIVAS).slack("U4QC23961").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(SRIRAM, defaultUserInfo(SRIRAM).slack("U5L475PK5").team(CONTINUOUS_VERIFICATION).build())
          .put(UJJAWAL, defaultUserInfo(UJJAWAL).slack("UKLSV01DW").team(PLATFORM).build())
          .put(UTKARSH, defaultUserInfo(UTKARSH).slack("UKGF0UL58").team(PLATFORM).build())
          .put(UTSAV, defaultUserInfo(UTSAV).slack("U015DTMQRV4").team(CONTINUOUS_EFFICIENCY).build())
          .put(VAIBHAV_SI, defaultUserInfo(VAIBHAV_SI).slack("UCK76T36U").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(VENKATESH, UserInfo.builder().email("venkatesh.kotrike@harness.io").slack("UGF55UEHF").build())
          .put(VIKAS, defaultUserInfo(VIKAS).slack("UE7M4CNMA").team(PLATFORM).build())
          .put(VOJIN, defaultUserInfo(VOJIN).slack("U015TFFL83G").team(PLATFORM).build())
          .put(VISTAAR, defaultUserInfo(VISTAAR).slack("U0138Q1JEHM").team(CONTINUOUS_INTEGRATION).build())
          .put(VUK, defaultUserInfo(VUK).slack("U0115RT3EQL").team(DELEGATE).build())
          .put(YOGESH, defaultUserInfo(YOGESH).slack("UJVLUUXAT").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(VARDAN_BANSAL, defaultUserInfo(VARDAN_BANSAL).slack("UH8NYAAUU").team(DEVELOPER_EXPERIENCE).build())
          .put(NANDAN, defaultUserInfo(NANDAN).slack("UKMS5KCBS").team(CONTINUOUS_VERIFICATION).build())
          .put(RIHAZ, defaultUserInfo(RIHAZ).slack("USUP66518").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(PRABU, defaultUserInfo(PRABU).slack("UTF8GHZEK").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(PHOENIKX, defaultUserInfo(PHOENIKX).slack("URWLPPJ8Z").team(PLATFORM).build())
          .put(SHUBHAM,
              defaultUserInfo(SHUBHAM)
                  .slack("UTRFKPL57")
                  .team(CONTINUOUS_INTEGRATION)
                  .jira("5e47234c2a59dc0c8fe4ccb8")
                  .build())
          .put(ABOSII, defaultUserInfo(ABOSII).slack("U01321VFH1A").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(ACASIAN, defaultUserInfo(ACASIAN).slack("U012UEVAPAR").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(VGLIJIN, defaultUserInfo(VGLIJIN).slack("U012W2S777V").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(MILAN, defaultUserInfo(MILAN).slack("U012P4GHM7Y").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(ARCHIT, defaultUserInfo(ARCHIT).slack("U012QGPR9N0").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(IVAN, defaultUserInfo(IVAN).slack("U014BFQ9PJS").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(TATHAGAT, defaultUserInfo(TATHAGAT).slack("U015DTMJLA2").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(SAHIL, defaultUserInfo(SAHIL).slack("U0141LFMEF8").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(BOJANA, defaultUserInfo(BOJANA).slack("U014GS4NFLM").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(LAZAR, defaultUserInfo(LAZAR).slack("U0150TB4LSK").team(DELEGATE).build())
          .put(HINGER, defaultUserInfo(HINGER).slack("U015DTMV2A2").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .put(UNKNOWN, UserInfo.builder().email("n/a").slack("channel").build())
          .put(TMACARI, defaultUserInfo(TMACARI).slack("U0172NX6SGY").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(NAMAN, defaultUserInfo(NAMAN).slack("U0173M823CN").team(CONTINUOUS_DEPLOYMENT_PLATFORM).build())
          .put(DEEPAK_PUTHRAYA,
              defaultUserInfo(DEEPAK_PUTHRAYA).slack("U018SBXS5M1").team(CONTINUOUS_DEPLOYMENT_CORE).build())
          .build();

  private static String prDeveloperId = findDeveloperId(System.getenv(GHPRB_PULL_AUTHOR_EMAIL));

  private static final Map<String, TeamInfo> teamInfo =
      ImmutableMap.<String, TeamInfo>builder()
          .put(CONTINUOUS_DEPLOYMENT_CORE,
              TeamInfo.builder().team(CONTINUOUS_DEPLOYMENT_CORE).leader(PRASHANT).leader(POOJA).build())
          .put(CONTINUOUS_DEPLOYMENT_PLATFORM,
              TeamInfo.builder().team(CONTINUOUS_DEPLOYMENT_PLATFORM).leader(ANSHUL).build())
          .put(CONTINUOUS_EFFICIENCY,
              TeamInfo.builder().team(CONTINUOUS_EFFICIENCY).leader(PUNEET).leader(AVMOHAN).build())
          .put(CONTINUOUS_INTEGRATION, TeamInfo.builder().team(CONTINUOUS_INTEGRATION).leader(SHIVAKUMAR).build())
          .put(CONTINUOUS_VERIFICATION, TeamInfo.builder().team(CONTINUOUS_VERIFICATION).leader(RAGHU).build())
          .put(DELEGATE, TeamInfo.builder().team(DELEGATE).leader(GEORGE).leader(MARKO).build())
          .put(DEVELOPER_EXPERIENCE, TeamInfo.builder().team(DEVELOPER_EXPERIENCE).leader(RAMA).build())
          .put(PLATFORM, TeamInfo.builder().team(PLATFORM).leader(ANKIT).leader(VIKAS).build())
          .put(SWAT, TeamInfo.builder().team(SWAT).leader(BRETT).build())
          .build();

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

  public static UserInfo findDeveloper(String developerId) {
    return active.get(developerId);
  }

  public static String findDeveloperId(String email) {
    if (email == null) {
      return null;
    }

    for (Entry<String, UserInfo> entry : active.entrySet()) {
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

    UserInfo userInfo = active.get(developer);
    if (userInfo == null) {
      return;
    }

    try {
      JiraClient jira = getJira();
      String jql = generateJQL(test);
      Issue.SearchResult searchResult = jira.searchIssues(jql, 1);
      if (searchResult.total == 0) {
        if (userInfo.getJira() == null || userInfo.getTeam() == null) {
          return;
        }

        Issue issue = generateJiraCreate(test, userInfo, priority).execute();
        log.info("New jira issue was created {}", issue.getKey());
        return;
      }

      Issue issue = searchResult.issues.get(0);

      if (!issue.getProject().getKey().equals(userInfo.getTeam())) {
        // We cannot automatically move an issue from one project to another.
        // Instead we are going to mark the current one as rejected.
        // Next time we would not find it and we will create a new one.

        // First lets set Bug Resolution to Ownership changed
        issue.update().field("customfield_10687", "Ownership changed").execute();

        issue.transition().execute("Rejected");
        return;
      }

      if (userInfo.getJira() != null && !issue.getAssignee().getEmail().equals(userInfo.getEmail())) {
        issue.update().field(ASSIGNEE, userInfo.getJira()).execute();
      }

      if (priority.compareTo(issue.getPriority().getName()) > 0) {
        issue.update().field(PRIORITY, priority).execute();
      }

    } catch (JiraException e) {
      log.error("Failed when checking the jira issue", e);
    }
  }

  private static Issue.FluentCreate generateJiraCreate(String test, UserInfo userInfo, String priority)
      throws JiraException {
    return getJira()
        .createIssue(userInfo.getTeam(), "Bug")
        .field(ASSIGNEE, userInfo.getJira())
        .field(SUMMARY, test + " " + NEEDS_FIXING)
        .field(PRIORITY, priority)
        .field(DESCRIPTION, DESCRIPTION_VALUE);
  }

  public static void fileOwnerAs(String developer, String type) {
    log.info("Developer {} is found to be owner of {} test", developer, type);
    createSlackFile(developer, type);

    UserInfo userInfo = active.get(developer);
    if (userInfo == null) {
      return;
    }

    TeamInfo teamLeader = teamInfo.get(userInfo.getTeam());
    if (teamLeader == null) {
      return;
    }

    for (String leader : teamLeader.getLeaders()) {
      if (!leader.equals(developer)) {
        fileLeaderAs(leader, type);
      }
    }
  }

  public static void fileLeaderAs(String leader, String type) {
    createSlackFile(leader, type + "-leaders");
  }

  private static String root =
      System.getenv().getOrDefault("TEST_OWNERS_ROOT_DIR", System.getProperty("java.io.tmpdir") + "/owners");

  private static void createSlackFile(String developer, String directory) {
    UserInfo userInfo = active.get(developer);
    if (userInfo == null) {
      return;
    }
    String identify = userInfo.getSlack() == null ? developer : "<@" + userInfo.getSlack() + ">";

    try {
      File file = new File(format("%s/%s/%s", root, directory, identify));

      file.getParentFile().mkdirs();
      if (file.createNewFile()) {
        log.info("Wrote to {}", file);
      } else {
        log.debug("{} was already set", file);
      }
    } catch (Exception ignore) {
      // Ignore the exceptions
    }
  }
}
