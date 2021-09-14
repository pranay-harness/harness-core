/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm;

/**
 * The Class TestStateExecutionData.
 */
public class TestStateExecutionData extends StateExecutionData {
  private String key;
  private String value;

  /**
   * Instantiates a new test state execution data.
   */
  public TestStateExecutionData() {}

  /**
   * Instantiates a new test state execution data.
   *
   * @param key   the key
   * @param value the value
   */
  public TestStateExecutionData(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Gets key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(String key) {
    this.key = key;
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

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "TestStateExecutionData [key=" + key + ", value=" + value + "]";
  }
}
