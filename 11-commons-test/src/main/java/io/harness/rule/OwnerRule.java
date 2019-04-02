package io.harness.rule;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import io.harness.category.element.FunctionalTests;
import io.harness.category.element.IntegrationTests;
import io.harness.category.element.UnitTests;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

public class OwnerRule extends RepeatRule {
  private static final Logger logger = LoggerFactory.getLogger(OwnerRule.class);

  private static List<String> active = asList("aaditi.joag@harness.io", "adwait.bhandare@harness.io",
      "anshul@harness.io", "anubhaw@harness.io", "george@harness.io", "jatin@harness.io", "mark.lu@harness.io",
      "raghu@harness.io", "rathnakara.malatesha@harness.io", "pooja@harness.io", "pranjal@harness.io",
      "praveen.sugavanam@harness.io", "puneet.saraswat@harness.io", "srinivas@harness.io", "sriram@harness.io",
      "sunil@harness.io", "swamy@harness.io", "vaibhav.si@harness.io", "vaibhav.tulsyan@harness.io");

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
        throw new RuntimeException(format("Email %s is not active.", email));
      }
    }

    // If there is email, it should match
    final String prEmail = System.getenv("ghprbPullAuthorEmail");
    if (prEmail == null) {
      if (owner.intermittent()) {
        return RepeatRule.RepeatStatement.builder().build();
      }
      return statement;
    }

    logger.info("ghprbPullAuthorEmail = {}", prEmail);

    final boolean match = Arrays.stream(owner.emails()).anyMatch(email -> email.equals(prEmail));
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
