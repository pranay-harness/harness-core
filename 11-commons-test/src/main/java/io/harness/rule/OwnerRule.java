package io.harness.rule;

import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;

import io.harness.NoopStatement;
import io.harness.exception.CategoryConfigException;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Map;

@Slf4j
public class OwnerRule implements TestRule {
  public static final String AADITI = "aaditi.joag@harness.io";
  public static final String ADWAIT = "adwait.bhandare@harness.io";
  public static final String AMAN = "aman.singh@harness.io";
  public static final String ANKIT = "ankit.singhal@harness.io";
  public static final String ANSHUL = "anshul@harness.io";
  public static final String ANUBHAW = "anubhaw@harness.io";
  public static final String AVMOHAN = "abhijith.mohan@harness.io";
  public static final String BRETT = "brett@harness.io";
  public static final String DEEPAK = "deepak.patankar@harness.io";
  public static final String GARVIT = "garvit.pahal@harness.io";
  public static final String GEORGE = "george@harness.io";
  public static final String HANTANG = "hannah.tang@harness.io";
  public static final String HARSH = "harsh.jain@harness.io";
  public static final String HITESH = "hitesh.aringa@harness.io";
  public static final String JATIN = "jatin@harness.io";
  public static final String JUHI = "juhi.agrawal@harness.io";
  public static final String KAMAL = "kamal.joshi@harness.io";
  public static final String MARK = "mark.lu@harness.io";
  public static final String MEENAKSHI = "meenakshi.raikwar@harness.io";
  public static final String NATARAJA = "nataraja@harness.io";
  public static final String PARNIAN = "parnian@harness.io";
  public static final String POOJA = "pooja@harness.io";
  public static final String PRANJAL = "pranjal@harness.io";
  public static final String PRAVEEN = "praveen.sugavanam@harness.io";
  public static final String PUNEET = "puneet.saraswat@harness.io";
  public static final String RAGHU = "raghu@harness.io";
  public static final String RAMA = "rama@harness.io";
  public static final String ROHIT = "rohit.reddy@harness.io";
  public static final String RUSHABH = "rushabh@harness.io";
  public static final String SHASWAT = "shaswat.deep@harness.io";
  public static final String SHUBHANSHU = "shubhanshu.verma@harness.io";
  public static final String SOWMYA = "sowmya.k@harness.io";
  public static final String SRINIVAS = "srinivas@harness.io";
  public static final String SRIRAM = "sriram@harness.io";
  public static final String SUNIL = "sunil@harness.io";
  public static final String SWAMY = "swamy@harness.io";
  public static final String UJJAWAL = "ujjawal.prasad@harness.io";
  public static final String UTKARSH = "utkarsh.gupta@harness.io";
  public static final String VAIBHAV_SI = "vaibhav.si@harness.io";
  public static final String VAIBHAV_TULSYAN = "vaibhav.tulsyan@harness.io";
  public static final String VENKATESH = "venkatesh.kotrike@harness.io";
  public static final String VIKAS = "vikas.naiyar@harness.io";
  public static final String YOGESH_CHAUHAN = "yogesh.chauhan@harness.io";
  public static final String ROHIT_KUMAR = "rohit.kumar@harness.io";
  @Deprecated public static final String UNKNOWN = "unknown";

  @Value
  @Builder
  public static class DevInfo {
    private String slack;
  }

  @Getter
  private static final Map<String, DevInfo> active = ImmutableMap.<String, DevInfo>builder()
                                                         .put(AADITI, DevInfo.builder().build())
                                                         .put(ADWAIT, DevInfo.builder().build())
                                                         .put(ANKIT, DevInfo.builder().build())
                                                         .put(AMAN, DevInfo.builder().build())
                                                         .put(ANSHUL, DevInfo.builder().build())
                                                         .put(ANUBHAW, DevInfo.builder().build())
                                                         .put(AVMOHAN, DevInfo.builder().build())
                                                         .put(BRETT, DevInfo.builder().build())
                                                         .put(DEEPAK, DevInfo.builder().build())
                                                         .put(GARVIT, DevInfo.builder().build())
                                                         .put(GEORGE, DevInfo.builder().slack("george").build())
                                                         .put(HANTANG, DevInfo.builder().build())
                                                         .put(HARSH, DevInfo.builder().build())
                                                         .put(HITESH, DevInfo.builder().build())
                                                         .put(JATIN, DevInfo.builder().build())
                                                         .put(JUHI, DevInfo.builder().build())
                                                         .put(KAMAL, DevInfo.builder().build())
                                                         .put(MARK, DevInfo.builder().build())
                                                         .put(MEENAKSHI, DevInfo.builder().build())
                                                         .put(NATARAJA, DevInfo.builder().build())
                                                         .put(PARNIAN, DevInfo.builder().build())
                                                         .put(POOJA, DevInfo.builder().build())
                                                         .put(PRANJAL, DevInfo.builder().build())
                                                         .put(PRAVEEN, DevInfo.builder().build())
                                                         .put(PUNEET, DevInfo.builder().build())
                                                         .put(RAGHU, DevInfo.builder().build())
                                                         .put(RAMA, DevInfo.builder().build())
                                                         .put(ROHIT, DevInfo.builder().build())
                                                         .put(RUSHABH, DevInfo.builder().build())
                                                         .put(SHASWAT, DevInfo.builder().build())
                                                         .put(SHUBHANSHU, DevInfo.builder().build())
                                                         .put(SOWMYA, DevInfo.builder().build())
                                                         .put(SRINIVAS, DevInfo.builder().build())
                                                         .put(SRIRAM, DevInfo.builder().build())
                                                         .put(SUNIL, DevInfo.builder().build())
                                                         .put(SWAMY, DevInfo.builder().build())
                                                         .put(UJJAWAL, DevInfo.builder().build())
                                                         .put(UTKARSH, DevInfo.builder().build())
                                                         .put(VAIBHAV_SI, DevInfo.builder().build())
                                                         .put(VAIBHAV_TULSYAN, DevInfo.builder().build())
                                                         .put(VENKATESH, DevInfo.builder().build())
                                                         .put(VIKAS, DevInfo.builder().build())
                                                         .put(YOGESH_CHAUHAN, DevInfo.builder().build())
                                                         .put(ROHIT_KUMAR, DevInfo.builder().build())
                                                         .put(UNKNOWN, DevInfo.builder().slack("channel").build())
                                                         .build();

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Owner {
    String[] emails();

    boolean intermittent() default false;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      throw new CategoryConfigException("Owner annotation is obligatory.");
    }

    for (String email : owner.emails()) {
      if (!active.containsKey(email)) {
        throw new CategoryConfigException(format("Email %s is not active.", email));
      }
    }

    final String prEmail = System.getenv("ghprbPullAuthorEmail");
    if (prEmail == null) {
      if (owner.intermittent()) {
        return new NoopStatement();
      }
      return statement;
    }

    logger.info("ghprbPullAuthorEmail = {}", prEmail);

    // If there is email, it should match
    final boolean match = Arrays.asList(owner.emails()).contains(prEmail);
    if (!match) {
      if (owner.intermittent()) {
        return new NoopStatement();
      }
    }

    return statement;
  }
  }