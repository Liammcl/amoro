/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.api.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Objects;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;

/** Data expiration configuration. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataExpirationConfig {
  // data-expire.enabled
  private boolean enabled;
  // data-expire.field
  private String expirationField;
  // data-expire.level
  private ExpireLevel expirationLevel;
  // data-expire.retention-time
  private long retentionTime;
  // data-expire.datetime-string-pattern
  private String dateTimePattern;
  // data-expire.datetime-number-format
  private String numberDateFormat;
  // data-expire.since
  private Since since;

  @VisibleForTesting
  public enum ExpireLevel {
    PARTITION,
    FILE;

    public static ExpireLevel fromString(String level) {
      Preconditions.checkArgument(null != level, "Invalid level type: null");
      try {
        return ExpireLevel.valueOf(level.toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(String.format("Invalid level type: %s", level), e);
      }
    }
  }

  @VisibleForTesting
  public enum Since {
    LATEST_SNAPSHOT,
    CURRENT_TIMESTAMP;

    public static Since fromString(String since) {
      Preconditions.checkArgument(null != since, "data-expire.since is invalid: null");
      try {
        return Since.valueOf(since.toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            String.format("Unable to expire data since: %s", since), e);
      }
    }
  }

  public static final Set<Type.TypeID> FIELD_TYPES =
      Sets.newHashSet(Type.TypeID.TIMESTAMP, Type.TypeID.STRING, Type.TypeID.LONG);

  private static final Logger LOG = LoggerFactory.getLogger(DataExpirationConfig.class);

  public DataExpirationConfig() {}

  public DataExpirationConfig(
      boolean enabled,
      String expirationField,
      ExpireLevel expirationLevel,
      long retentionTime,
      String dateTimePattern,
      String numberDateFormat,
      Since since) {
    this.enabled = enabled;
    this.expirationField = expirationField;
    this.expirationLevel = expirationLevel;
    this.retentionTime = retentionTime;
    this.dateTimePattern = dateTimePattern;
    this.numberDateFormat = numberDateFormat;
    this.since = since;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public DataExpirationConfig setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public String getExpirationField() {
    return expirationField;
  }

  public DataExpirationConfig setExpirationField(String expirationField) {
    this.expirationField = expirationField;
    return this;
  }

  public ExpireLevel getExpirationLevel() {
    return expirationLevel;
  }

  public DataExpirationConfig setExpirationLevel(ExpireLevel expirationLevel) {
    this.expirationLevel = expirationLevel;
    return this;
  }

  public long getRetentionTime() {
    return retentionTime;
  }

  public DataExpirationConfig setRetentionTime(long retentionTime) {
    this.retentionTime = retentionTime;
    return this;
  }

  public String getDateTimePattern() {
    return dateTimePattern;
  }

  public DataExpirationConfig setDateTimePattern(String dateTimePattern) {
    this.dateTimePattern = dateTimePattern;
    return this;
  }

  public String getNumberDateFormat() {
    return numberDateFormat;
  }

  public DataExpirationConfig setNumberDateFormat(String numberDateFormat) {
    this.numberDateFormat = numberDateFormat;
    return this;
  }

  public Since getSince() {
    return since;
  }

  public DataExpirationConfig setSince(Since since) {
    this.since = since;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataExpirationConfig)) {
      return false;
    }
    DataExpirationConfig config = (DataExpirationConfig) o;
    return enabled == config.enabled
        && retentionTime == config.retentionTime
        && Objects.equal(expirationField, config.expirationField)
        && expirationLevel == config.expirationLevel
        && Objects.equal(dateTimePattern, config.dateTimePattern)
        && Objects.equal(numberDateFormat, config.numberDateFormat)
        && since == config.since;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        enabled,
        expirationField,
        expirationLevel,
        retentionTime,
        dateTimePattern,
        numberDateFormat,
        since);
  }

  public boolean isValid(Types.NestedField field, String name) {
    return isEnabled()
        && getRetentionTime() > 0
        && validateExpirationField(field, name, getExpirationField());
  }

  private boolean validateExpirationField(
      Types.NestedField field, String name, String expirationField) {
    if (StringUtils.isBlank(expirationField) || null == field) {
      LOG.warn(
          String.format(
              "Field(%s) used to determine data expiration is illegal for table(%s)",
              expirationField, name));
      return false;
    }
    Type.TypeID typeID = field.type().typeId();
    if (!DataExpirationConfig.FIELD_TYPES.contains(typeID)) {
      LOG.warn(
          String.format(
              "Table(%s) field(%s) type(%s) is not supported for data expiration, please use the "
                  + "following types: %s",
              name,
              expirationField,
              typeID.name(),
              StringUtils.join(DataExpirationConfig.FIELD_TYPES, ", ")));
      return false;
    }

    return true;
  }
}