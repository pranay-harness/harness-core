/*
 * Harness feature flag service
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: ff@harness.io
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package io.harness.cf.openapi.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ServingRule
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-03-24T19:32:06.834-07:00[America/Los_Angeles]")
public class ServingRule {
  public static final String SERIALIZED_NAME_RULE_ID = "ruleId";
  @SerializedName(SERIALIZED_NAME_RULE_ID) private String ruleId;

  public static final String SERIALIZED_NAME_PRIORITY = "priority";
  @SerializedName(SERIALIZED_NAME_PRIORITY) private Integer priority;

  public static final String SERIALIZED_NAME_CLAUSES = "clauses";
  @SerializedName(SERIALIZED_NAME_CLAUSES) private List<io.harness.cf.openapi.model.Clause> clauses = new ArrayList<>();

  public static final String SERIALIZED_NAME_SERVE = "serve";
  @SerializedName(SERIALIZED_NAME_SERVE) private io.harness.cf.openapi.model.Serve serve;

  public ServingRule ruleId(String ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  /**
   * Get ruleId
   * @return ruleId
   **/
  @ApiModelProperty(required = true, value = "")

  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }

  public ServingRule priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Get priority
   * @return priority
   **/
  @ApiModelProperty(required = true, value = "")

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public ServingRule clauses(List<io.harness.cf.openapi.model.Clause> clauses) {
    this.clauses = clauses;
    return this;
  }

  public ServingRule addClausesItem(io.harness.cf.openapi.model.Clause clausesItem) {
    this.clauses.add(clausesItem);
    return this;
  }

  /**
   * Get clauses
   * @return clauses
   **/
  @ApiModelProperty(required = true, value = "")

  public List<io.harness.cf.openapi.model.Clause> getClauses() {
    return clauses;
  }

  public void setClauses(List<Clause> clauses) {
    this.clauses = clauses;
  }

  public ServingRule serve(io.harness.cf.openapi.model.Serve serve) {
    this.serve = serve;
    return this;
  }

  /**
   * Get serve
   * @return serve
   **/
  @ApiModelProperty(required = true, value = "")

  public io.harness.cf.openapi.model.Serve getServe() {
    return serve;
  }

  public void setServe(Serve serve) {
    this.serve = serve;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServingRule servingRule = (ServingRule) o;
    return Objects.equals(this.ruleId, servingRule.ruleId) && Objects.equals(this.priority, servingRule.priority)
        && Objects.equals(this.clauses, servingRule.clauses) && Objects.equals(this.serve, servingRule.serve);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleId, priority, clauses, serve);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServingRule {\n");
    sb.append("    ruleId: ").append(toIndentedString(ruleId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    clauses: ").append(toIndentedString(clauses)).append("\n");
    sb.append("    serve: ").append(toIndentedString(serve)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
