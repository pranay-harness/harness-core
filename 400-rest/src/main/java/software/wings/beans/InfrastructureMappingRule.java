/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by anubhaw on 9/29/16.
 */

public class InfrastructureMappingRule {
  private String name;
  private List<Rule> rules = new ArrayList<>();
  private String appId;
  private String envId;
  private String tagId;

  /**
   * Gets rules.
   *
   * @return the rules
   */
  public List<Rule> getRules() {
    return rules;
  }

  /**
   * Sets rules.
   *
   * @param rules the rules
   */
  public void setRules(List<Rule> rules) {
    this.rules = rules;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets tag id.
   *
   * @return the tag id
   */
  public String getTagId() {
    return tagId;
  }

  /**
   * Sets tag id.
   *
   * @param tagId the tag id
   */
  public void setTagId(String tagId) {
    this.tagId = tagId;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, rules, appId, envId, tagId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("rules", rules)
        .add("appId", appId)
        .add("envId", envId)
        .add("tagId", tagId)
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InfrastructureMappingRule other = (InfrastructureMappingRule) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.rules, other.rules)
        && Objects.equals(this.appId, other.appId) && Objects.equals(this.envId, other.envId)
        && Objects.equals(this.tagId, other.tagId);
  }

  /**
   * Evaluate boolean.
   *
   * @param context the context
   * @return the boolean
   */
  public boolean evaluate(Map<String, String> context) {
    return rules.stream().allMatch(rule -> match(rule, context));
  }

  private boolean match(Rule rule, Map<String, String> context) {
    switch (rule.getOperator()) {
      case EQUAL:
        return rule.getValue().equals(context.get(rule.getPropertyName()));
      case NOT_EQUAL:
        return !rule.getValue().equals(context.get(rule.getPropertyName()));
      case STARTS_WITH:
        return context.get(rule.getPropertyName()) != null
            && context.get(rule.getPropertyName()).startsWith(rule.getValue());
      case ENDS_WITH:
        return context.get(rule.getPropertyName()) != null
            && context.get(rule.getPropertyName()).endsWith(rule.getValue());
      case CONTAINS:
        return context.get(rule.getPropertyName()) != null
            && context.get(rule.getPropertyName()).contains(rule.getValue());
      default:
        return false;
    }
  }

  /**
   * The type Rule.
   */
  public static class Rule {
    private String propertyName;
    private HostRuleOperator operator;
    private String value;

    public Rule() {}

    public Rule(String propertyName, HostRuleOperator operator, String value) {
      this.propertyName = propertyName;
      this.operator = operator;
      this.value = value;
    }

    /**
     * Gets property name.
     *
     * @return the property name
     */
    public String getPropertyName() {
      return propertyName;
    }

    /**
     * Sets property name.
     *
     * @param propertyName the property name
     */
    public void setPropertyName(String propertyName) {
      this.propertyName = propertyName;
    }

    /**
     * Gets operator.
     *
     * @return the operator
     */
    public HostRuleOperator getOperator() {
      return operator;
    }

    /**
     * Sets operator.
     *
     * @param operator the operator
     */
    public void setOperator(HostRuleOperator operator) {
      this.operator = operator;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(String value) {
      this.value = value;
    }
  }

  /**
   * The enum Host rule operator.
   */
  public enum HostRuleOperator {
    /**
     * Equal host rule operator.
     */
    EQUAL,
    /**
     * Not equal host rule operator.
     */
    NOT_EQUAL,
    /**
     * Starts with host rule operator.
     */
    STARTS_WITH,
    /**
     * End with host rule operator.
     */
    ENDS_WITH,
    /**
     * Contains host rule operator.
     */
    CONTAINS
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private List<Rule> rules = new ArrayList<>();
    private String appId;
    private String envId;
    private String tagId;

    private Builder() {}

    /**
     * An infrastructure mapping rule builder.
     *
     * @return the builder
     */
    public static Builder anInfrastructureMappingRule() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With rules builder.
     *
     * @param rules the rules
     * @return the builder
     */
    public Builder withRules(List<Rule> rules) {
      this.rules = rules;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With tag id builder.
     *
     * @param tagId the tag id
     * @return the builder
     */
    public Builder withTagId(String tagId) {
      this.tagId = tagId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anInfrastructureMappingRule().withName(name).withRules(rules).withAppId(appId).withEnvId(envId).withTagId(
          tagId);
    }

    /**
     * Build infrastructure mapping rule.
     *
     * @return the infrastructure mapping rule
     */
    public InfrastructureMappingRule build() {
      InfrastructureMappingRule infrastructureMappingRule = new InfrastructureMappingRule();
      infrastructureMappingRule.setName(name);
      infrastructureMappingRule.setRules(rules);
      infrastructureMappingRule.setAppId(appId);
      infrastructureMappingRule.setEnvId(envId);
      infrastructureMappingRule.setTagId(tagId);
      return infrastructureMappingRule;
    }
  }
}
