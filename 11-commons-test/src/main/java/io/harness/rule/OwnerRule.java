package io.harness.rule;

import static java.lang.String.format;

import com.google.common.collect.ImmutableList;

import io.harness.category.element.CliFunctionalTests;
import io.harness.category.element.FunctionalTests;
import io.harness.category.element.IntegrationTests;
import io.harness.category.element.UnitTests;
import io.harness.exception.CategoryConfigException;
import lombok.extern.slf4j.Slf4j;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class OwnerRule extends RepeatRule {
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

  private static List<String> active = ImmutableList.<String>builder()
                                           .add(AADITI)
                                           .add(ADWAIT)
                                           .add(ANKIT)
                                           .add(AMAN)
                                           .add(ANSHUL)
                                           .add(ANUBHAW)
                                           .add(AVMOHAN)
                                           .add(BRETT)
                                           .add(DEEPAK)
                                           .add(GARVIT)
                                           .add(GEORGE)
                                           .add(HANTANG)
                                           .add(HARSH)
                                           .add(HITESH)
                                           .add(JATIN)
                                           .add(JUHI)
                                           .add(KAMAL)
                                           .add(MARK)
                                           .add(MEENAKSHI)
                                           .add(NATARAJA)
                                           .add(PARNIAN)
                                           .add(POOJA)
                                           .add(PRANJAL)
                                           .add(PRAVEEN)
                                           .add(PUNEET)
                                           .add(RAGHU)
                                           .add(RAMA)
                                           .add(ROHIT)
                                           .add(RUSHABH)
                                           .add(SHASWAT)
                                           .add(SHUBHANSHU)
                                           .add(SOWMYA)
                                           .add(SRINIVAS)
                                           .add(SRIRAM)
                                           .add(SUNIL)
                                           .add(SWAMY)
                                           .add(UJJAWAL)
                                           .add(UTKARSH)
                                           .add(VAIBHAV_SI)
                                           .add(VAIBHAV_TULSYAN)
                                           .add(VENKATESH)
                                           .add(VIKAS)
                                           .add(YOGESH_CHAUHAN)
                                           .build();

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Owner {
    String[] emails();
    boolean resent() default true;
    boolean intermittent() default false;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return statement;
    }

    for (String email : owner.emails()) {
      if (!active.contains(email)) {
        throw new CategoryConfigException(format("Email %s is not active.", email));
      }
    }

    final String prEmail = System.getenv("ghprbPullAuthorEmail");
    if (prEmail == null) {
      if (owner.intermittent()) {
        return RepeatRule.RepeatStatement.builder().build();
      }
      return statement;
    }

    logger.info("ghprbPullAuthorEmail = {}", prEmail);

    // If there is email, it should match
    final boolean match = Arrays.asList(owner.emails()).contains(prEmail);
    if (!match) {
      if (owner.intermittent()) {
        return RepeatRule.RepeatStatement.builder().build();
      }
      return statement;
    }

    if (!owner.resent()) {
      return statement;
    }

    final Class categoryElement = CategoryTimeoutRule.fetchCategoryElement(description.getAnnotation(Category.class));

    int repeatCount = 20;
    if (categoryElement == UnitTests.class) {
      repeatCount = 15;
    } else if (categoryElement == IntegrationTests.class) {
      repeatCount = 10;
    } else if (categoryElement == FunctionalTests.class) {
      repeatCount = 5;
    } else if (categoryElement == CliFunctionalTests.class) {
      repeatCount = 5;
    }

    return RepeatRule.RepeatStatement.builder()
        .statement(statement)
        .parentRule(this)
        .times(repeatCount)
        .successes(repeatCount)
        .timeoutOnly(true)
        .build();
  }
  }