/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

/**
 * The Class OperationalZone.
 */
public class OperationalZone {
  private String name;
  private String description;
  private String dcName;

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

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets dc name.
   *
   * @return the dc name
   */
  public String getDcName() {
    return dcName;
  }

  /**
   * Sets dc name.
   *
   * @param dcName the dc name
   */
  public void setDcName(String dcName) {
    this.dcName = dcName;
  }
}
