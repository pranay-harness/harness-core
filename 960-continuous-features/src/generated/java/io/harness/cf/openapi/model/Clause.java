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

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Clause
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-04-12T12:00:52.324-07:00[America/Los_Angeles]")
public class Clause {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID) private String id;

  public static final String SERIALIZED_NAME_ATTRIBUTE = "attribute";
  @SerializedName(SERIALIZED_NAME_ATTRIBUTE) private String attribute;

  public static final String SERIALIZED_NAME_OP = "op";
  @SerializedName(SERIALIZED_NAME_OP) private String op;

  public static final String SERIALIZED_NAME_VALUES = "values";
  @SerializedName(SERIALIZED_NAME_VALUES) private List<String> values = new ArrayList<>();

  public static final String SERIALIZED_NAME_NEGATE = "negate";
  @SerializedName(SERIALIZED_NAME_NEGATE) private Boolean negate;

  public Clause id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   **/
  @ApiModelProperty(required = true, value = "")

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Clause attribute(String attribute) {
    this.attribute = attribute;
    return this;
  }

  /**
   * Get attribute
   * @return attribute
   **/
  @ApiModelProperty(required = true, value = "")

  public String getAttribute() {
    return attribute;
  }

  public void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  public Clause op(String op) {
    this.op = op;
    return this;
  }

  /**
   * Get op
   * @return op
   **/
  @ApiModelProperty(required = true, value = "")

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public Clause values(List<String> values) {
    this.values = values;
    return this;
  }

  public Clause addValuesItem(String valuesItem) {
    this.values.add(valuesItem);
    return this;
  }

  /**
   * Get values
   * @return values
   **/
  @ApiModelProperty(required = true, value = "")

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  public Clause negate(Boolean negate) {
    this.negate = negate;
    return this;
  }

  /**
   * Get negate
   * @return negate
   **/
  @ApiModelProperty(required = true, value = "")

  public Boolean getNegate() {
    return negate;
  }

  public void setNegate(Boolean negate) {
    this.negate = negate;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Clause clause = (Clause) o;
    return Objects.equals(this.id, clause.id) && Objects.equals(this.attribute, clause.attribute)
        && Objects.equals(this.op, clause.op) && Objects.equals(this.values, clause.values)
        && Objects.equals(this.negate, clause.negate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, attribute, op, values, negate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Clause {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    attribute: ").append(toIndentedString(attribute)).append("\n");
    sb.append("    op: ").append(toIndentedString(op)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    negate: ").append(toIndentedString(negate)).append("\n");
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
