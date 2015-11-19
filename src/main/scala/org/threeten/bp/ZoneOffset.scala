/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.bp

import java.util.Objects
import org.threeten.bp.temporal.ChronoField.OFFSET_SECONDS
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.io.InvalidObjectException
import java.io.ObjectStreamException
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.Temporal
import org.threeten.bp.temporal.TemporalAccessor
import org.threeten.bp.temporal.TemporalAdjuster
import org.threeten.bp.temporal.TemporalField
import org.threeten.bp.temporal.TemporalQueries
import org.threeten.bp.temporal.TemporalQuery
import org.threeten.bp.temporal.UnsupportedTemporalTypeException
import org.threeten.bp.temporal.ValueRange
import org.threeten.bp.zone.ZoneRules

/**
  * A time-zone offset from Greenwich/UTC, such as {@code +02:00}.
  * <p>
  * A time-zone offset is the period of time that a time-zone differs from Greenwich/UTC.
  * This is usually a fixed number of hours and minutes.
  * <p>
  * Different parts of the world have different time-zone offsets.
  * The rules for how offsets vary by place and time of year are captured in the
  * {@link ZoneId} class.
  * <p>
  * For example, Paris is one hour ahead of Greenwich/UTC in winter and two hours
  * ahead in summer. The {@code ZoneId} instance for Paris will reference two
  * {@code ZoneOffset} instances - a {@code +01:00} instance for winter,
  * and a {@code +02:00} instance for summer.
  * <p>
  * In 2008, time-zone offsets around the world extended from -12:00 to +14:00.
  * To prevent any problems with that range being extended, yet still provide
  * validation, the range of offsets is restricted to -18:00 to 18:00 inclusive.
  * <p>
  * This class is designed for use with the ISO calendar system.
  * The fields of hours, minutes and seconds make assumptions that are valid for the
  * standard ISO definitions of those fields. This class may be used with other
  * calendar systems providing the definition of the time fields matches those
  * of the ISO calendar system.
  * <p>
  * Instances of {@code ZoneOffset} must be compared using {@link #equals}.
  * Implementations may choose to cache certain common offsets, however
  * applications must not rely on such caching.
  *
  * <h3>Specification for implementors</h3>
  * This class is immutable and thread-safe.
  */
@SerialVersionUID(2357656521762053153L)
object ZoneOffset {
  /**
    * Simulate JDK 8 method reference ZoneOffset::from.
    */
  val FROM: TemporalQuery[ZoneOffset] = (temporal: TemporalAccessor) => ZoneOffset.from(temporal)

  /** Cache of time-zone offset by offset in seconds. */
  private val SECONDS_CACHE: ConcurrentMap[Integer, ZoneOffset] = new ConcurrentHashMap[Integer, ZoneOffset](16, 0.75f, 4)
  /** Cache of time-zone offset by ID. */
  private val ID_CACHE: ConcurrentMap[String, ZoneOffset] = new ConcurrentHashMap[String, ZoneOffset](16, 0.75f, 4)

  /**
    * The number of seconds per hour.
    */
  private val SECONDS_PER_HOUR: Int = 60 * 60
  /**
    * The number of seconds per minute.
    */
  private val SECONDS_PER_MINUTE: Int = 60
  /**
    * The number of minutes per hour.
    */
  private val MINUTES_PER_HOUR: Int = 60
  /**
    * The abs maximum seconds.
    */
  private val MAX_SECONDS: Int = 18 * SECONDS_PER_HOUR
  /**
    * The time-zone offset for UTC, with an ID of 'Z'.
    */
  val UTC: ZoneOffset = ZoneOffset.ofTotalSeconds(0)
  /**
    * Constant for the maximum supported offset.
    */
  val MIN: ZoneOffset = ZoneOffset.ofTotalSeconds(-MAX_SECONDS)
  /**
    * Constant for the maximum supported offset.
    */
  val MAX: ZoneOffset = ZoneOffset.ofTotalSeconds(MAX_SECONDS)

  /**
    * Obtains an instance of {@code ZoneOffset} using the ID.
    * <p>
    * This method parses the string ID of a {@code ZoneOffset} to
    * return an instance. The parsing accepts all the formats generated by
    * {@link #getId()}, plus some additional formats:
    * <p><ul>
    * <li>{@code Z} - for UTC
    * <li>{@code +h}
    * <li>{@code +hh}
    * <li>{@code +hh:mm}
    * <li>{@code -hh:mm}
    * <li>{@code +hhmm}
    * <li>{@code -hhmm}
    * <li>{@code +hh:mm:ss}
    * <li>{@code -hh:mm:ss}
    * <li>{@code +hhmmss}
    * <li>{@code -hhmmss}
    * </ul><p>
    * Note that &plusmn; means either the plus or minus symbol.
    * <p>
    * The ID of the returned offset will be normalized to one of the formats
    * described by {@link #getId()}.
    * <p>
    * The maximum supported range is from +18:00 to -18:00 inclusive.
    *
    * @param offsetId  the offset ID, not null
    * @return the zone-offset, not null
    * @throws DateTimeException if the offset ID is invalid
    */
  def of(offsetId: String): ZoneOffset = {
    var _offsetId = offsetId
    Objects.requireNonNull(_offsetId, "offsetId")
    val offset: ZoneOffset = ID_CACHE.get(_offsetId)
    if (offset != null)
      return offset
    var hours: Int = 0
    var minutes: Int = 0
    var seconds: Int = 0
    /*
    if (_offsetId.length == 2) // ALSO NOT RIGHT
      _offsetId = _offsetId.charAt(0) + "0" + _offsetId.charAt(1)
    */
    _offsetId.length match {
      case 2 => /// !!! FIXME FALLTHROUGH ???
        _offsetId = _offsetId.charAt(0) + "0" + _offsetId.charAt(1)
      case 3 =>
        hours = parseNumber(_offsetId, 1, false)
        minutes = 0
        seconds = 0
      case 5 =>
        hours = parseNumber(_offsetId, 1, false)
        minutes = parseNumber(_offsetId, 3, false)
        seconds = 0
      case 6 =>
        hours = parseNumber(_offsetId, 1, false)
        minutes = parseNumber(_offsetId, 4, true)
        seconds = 0
      case 7 =>
        hours = parseNumber(_offsetId, 1, false)
        minutes = parseNumber(_offsetId, 3, false)
        seconds = parseNumber(_offsetId, 5, false)
      case 9 =>
        hours = parseNumber(_offsetId, 1, false)
        minutes = parseNumber(_offsetId, 4, true)
        seconds = parseNumber(_offsetId, 7, true)
      case _ =>
        throw new DateTimeException("Invalid ID for ZoneOffset, invalid format: " + _offsetId)
    }
    val first: Char = _offsetId.charAt(0)
    if (first != '+' && first != '-')
      throw new DateTimeException("Invalid ID for ZoneOffset, plus/minus not found when expected: " + _offsetId)
    if (first == '-') ofHoursMinutesSeconds(-hours, -minutes, -seconds)
    else ofHoursMinutesSeconds(hours, minutes, seconds)
  }

  /**
    * Parse a two digit zero-prefixed number.
    *
    * @param offsetId  the offset ID, not null
    * @param pos  the position to parse, valid
    * @param precededByColon  should this number be prefixed by a precededByColon
    * @return the parsed number, from 0 to 99
    */
  private def parseNumber(offsetId: CharSequence, pos: Int, precededByColon: Boolean): Int = {
    if (precededByColon && offsetId.charAt(pos - 1) != ':')
      throw new DateTimeException("Invalid ID for ZoneOffset, colon not found when expected: " + offsetId)
    val ch1: Char = offsetId.charAt(pos)
    val ch2: Char = offsetId.charAt(pos + 1)
    if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9')
      throw new DateTimeException("Invalid ID for ZoneOffset, non numeric characters found: " + offsetId)
    (ch1 - 48) * 10 + (ch2 - 48)
  }

  /**
    * Obtains an instance of {@code ZoneOffset} using an offset in hours.
    *
    * @param hours  the time-zone offset in hours, from -18 to +18
    * @return the zone-offset, not null
    * @throws DateTimeException if the offset is not in the required range
    */
  def ofHours(hours: Int): ZoneOffset = ofHoursMinutesSeconds(hours, 0, 0)

  /**
    * Obtains an instance of {@code ZoneOffset} using an offset in
    * hours and minutes.
    * <p>
    * The sign of the hours and minutes components must match.
    * Thus, if the hours is negative, the minutes must be negative or zero.
    * If the hours is zero, the minutes may be positive, negative or zero.
    *
    * @param hours  the time-zone offset in hours, from -18 to +18
    * @param minutes  the time-zone offset in minutes, from 0 to &plusmn;59, sign matches hours
    * @return the zone-offset, not null
    * @throws DateTimeException if the offset is not in the required range
    */
  def ofHoursMinutes(hours: Int, minutes: Int): ZoneOffset = ofHoursMinutesSeconds(hours, minutes, 0)

  /**
    * Obtains an instance of {@code ZoneOffset} using an offset in
    * hours, minutes and seconds.
    * <p>
    * The sign of the hours, minutes and seconds components must match.
    * Thus, if the hours is negative, the minutes and seconds must be negative or zero.
    *
    * @param hours  the time-zone offset in hours, from -18 to +18
    * @param minutes  the time-zone offset in minutes, from 0 to &plusmn;59, sign matches hours and seconds
    * @param seconds  the time-zone offset in seconds, from 0 to &plusmn;59, sign matches hours and minutes
    * @return the zone-offset, not null
    * @throws DateTimeException if the offset is not in the required range
    */
  def ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): ZoneOffset = {
    validate(hours, minutes, seconds)
    val totalSecs: Int = totalSeconds(hours, minutes, seconds)
    ofTotalSeconds(totalSecs)
  }

  /**
    * Obtains an instance of {@code ZoneOffset} from a temporal object.
    * <p>
    * A {@code TemporalAccessor} represents some form of date and time information.
    * This factory converts the arbitrary temporal object to an instance of {@code ZoneOffset}.
    * <p>
    * The conversion uses the {@link TemporalQueries#offset()} query, which relies
    * on extracting the {@link ChronoField#OFFSET_SECONDS OFFSET_SECONDS} field.
    * <p>
    * This method matches the signature of the functional interface {@link TemporalQuery}
    * allowing it to be used in queries via method reference, {@code ZoneOffset::from}.
    *
    * @param temporal  the temporal object to convert, not null
    * @return the zone-offset, not null
    * @throws DateTimeException if unable to convert to an { @code ZoneOffset}
    */
  def from(temporal: TemporalAccessor): ZoneOffset = {
    val offset: ZoneOffset = temporal.query(TemporalQueries.offset)
    if (offset == null)
      throw new DateTimeException("Unable to obtain ZoneOffset from TemporalAccessor: " + temporal + ", type " + temporal.getClass.getName)
    offset
  }

  /**
    * Validates the offset fields.
    *
    * @param hours  the time-zone offset in hours, from -18 to +18
    * @param minutes  the time-zone offset in minutes, from 0 to &plusmn;59
    * @param seconds  the time-zone offset in seconds, from 0 to &plusmn;59
    * @throws DateTimeException if the offset is not in the required range
    */
  private def validate(hours: Int, minutes: Int, seconds: Int): Unit = {
    if (hours < -18 || hours > 18)
      throw new DateTimeException("Zone offset hours not in valid range: value " + hours + " is not in the range -18 to 18")
    if (hours > 0) {
      if (minutes < 0 || seconds < 0)
        throw new DateTimeException("Zone offset minutes and seconds must be positive because hours is positive")
    }
    else if (hours < 0) {
      if (minutes > 0 || seconds > 0)
        throw new DateTimeException("Zone offset minutes and seconds must be negative because hours is negative")
    }
    else if ((minutes > 0 && seconds < 0) || (minutes < 0 && seconds > 0))
      throw new DateTimeException("Zone offset minutes and seconds must have the same sign")
    if (Math.abs(minutes) > 59)
      throw new DateTimeException("Zone offset minutes not in valid range: abs(value) " + Math.abs(minutes) + " is not in the range 0 to 59")
    if (Math.abs(seconds) > 59)
      throw new DateTimeException("Zone offset seconds not in valid range: abs(value) " + Math.abs(seconds) + " is not in the range 0 to 59")
    if (Math.abs(hours) == 18 && (Math.abs(minutes) > 0 || Math.abs(seconds) > 0))
      throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00")
  }

  /**
    * Calculates the total offset in seconds.
    *
    * @param hours  the time-zone offset in hours, from -18 to +18
    * @param minutes  the time-zone offset in minutes, from 0 to &plusmn;59, sign matches hours and seconds
    * @param seconds  the time-zone offset in seconds, from 0 to &plusmn;59, sign matches hours and minutes
    * @return the total in seconds
    */
  private def totalSeconds(hours: Int, minutes: Int, seconds: Int): Int =
    hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds

  /**
    * Obtains an instance of {@code ZoneOffset} specifying the total offset in seconds
    * <p>
    * The offset must be in the range {@code -18:00} to {@code +18:00}, which corresponds to -64800 to +64800.
    *
    * @param totalSeconds  the total time-zone offset in seconds, from -64800 to +64800
    * @return the ZoneOffset, not null
    * @throws DateTimeException if the offset is not in the required range
    */
  def ofTotalSeconds(totalSeconds: Int): ZoneOffset =
    if (Math.abs(totalSeconds) > MAX_SECONDS)
      throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00")
    else if (totalSeconds % (15 * SECONDS_PER_MINUTE) == 0) {
      val totalSecs: Integer = totalSeconds
      var result: ZoneOffset = SECONDS_CACHE.get(totalSecs)
      if (result == null) {
        result = new ZoneOffset(totalSeconds)
        SECONDS_CACHE.putIfAbsent(totalSecs, result)
        result = SECONDS_CACHE.get(totalSecs)
        ID_CACHE.putIfAbsent(result.getId, result)
      }
      result
    } else
      new ZoneOffset(totalSeconds)

  private def buildId(totalSeconds: Int): String = {
    if (totalSeconds == 0)
      "Z"
    else {
      val absTotalSeconds: Int = Math.abs(totalSeconds)
      val buf: StringBuilder = new StringBuilder
      val absHours: Int = absTotalSeconds / SECONDS_PER_HOUR
      val absMinutes: Int = (absTotalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR
      buf.append(if (totalSeconds < 0) "-" else "+").append(if (absHours < 10) "0" else "").append(absHours).append(if (absMinutes < 10) ":0" else ":").append(absMinutes)
      val absSeconds: Int = absTotalSeconds % SECONDS_PER_MINUTE
      if (absSeconds != 0) {
        buf.append(if (absSeconds < 10) ":0" else ":").append(absSeconds)
      }
      buf.toString
    }
  }

  @throws[IOException]
  private[bp] def readExternal(in: DataInput): ZoneOffset = {
    val offsetByte: Int = in.readByte
    if (offsetByte == 127) ZoneOffset.ofTotalSeconds(in.readInt) else ZoneOffset.ofTotalSeconds(offsetByte * 900)
  }
}

/**
  * Constructor.
  *
  * @param totalSeconds  the total time-zone offset in seconds, from -64800 to +64800
  */
@SerialVersionUID(2357656521762053153L)
final class ZoneOffset private(private val totalSeconds: Int) extends ZoneId with TemporalAccessor with TemporalAdjuster with Comparable[ZoneOffset] with Serializable {

  /**
    * The string form of the time-zone offset.
    */
  @transient
  private val id: String = ZoneOffset.buildId(totalSeconds)

  /**
    * Gets the total zone offset in seconds.
    * <p>
    * This is the primary way to access the offset amount.
    * It returns the total of the hours, minutes and seconds fields as a
    * single offset that can be added to a time.
    *
    * @return the total zone offset amount in seconds
    */
  def getTotalSeconds: Int = totalSeconds

  /**
    * Gets the normalized zone offset ID.
    * <p>
    * The ID is minor variation to the standard ISO-8601 formatted string
    * for the offset. There are three formats:
    * <p><ul>
    * <li>{@code Z} - for UTC (ISO-8601)
    * <li>{@code +hh:mm} or {@code -hh:mm} - if the seconds are zero (ISO-8601)
    * <li>{@code +hh:mm:ss} or {@code -hh:mm:ss} - if the seconds are non-zero (not ISO-8601)
    * </ul><p>
    *
    * @return the zone offset ID, not null
    */
  def getId: String = id

  /**
    * Gets the associated time-zone rules.
    * <p>
    * The rules will always return this offset when queried.
    * The implementation class is immutable, thread-safe and serializable.
    *
    * @return the rules, not null
    */
  def getRules: ZoneRules = ZoneRules.of(this)

  /**
    * Checks if the specified field is supported.
    * <p>
    * This checks if this offset can be queried for the specified field.
    * If false, then calling the {@link #range(TemporalField) range} and
    * {@link #get(TemporalField) get} methods will throw an exception.
    * <p>
    * If the field is a {@link ChronoField} then the query is implemented here.
    * The {@code OFFSET_SECONDS} field returns true.
    * All other {@code ChronoField} instances will return false.
    * <p>
    * If the field is not a {@code ChronoField}, then the result of this method
    * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
    * passing {@code this} as the argument.
    * Whether the field is supported is determined by the field.
    *
    * @param field  the field to check, null returns false
    * @return true if the field is supported on this offset, false if not
    */
  def isSupported(field: TemporalField): Boolean =
    if (field.isInstanceOf[ChronoField]) field eq OFFSET_SECONDS
    else field != null && field.isSupportedBy(this)

  /**
    * Gets the range of valid values for the specified field.
    * <p>
    * The range object expresses the minimum and maximum valid values for a field.
    * This offset is used to enhance the accuracy of the returned range.
    * If it is not possible to return the range, because the field is not supported
    * or for some other reason, an exception is thrown.
    * <p>
    * If the field is a {@link ChronoField} then the query is implemented here.
    * The {@link #isSupported(TemporalField) supported fields} will return
    * appropriate range instances.
    * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
    * <p>
    * If the field is not a {@code ChronoField}, then the result of this method
    * is obtained by invoking {@code TemporalField.rangeRefinedBy(TemporalAccessor)}
    * passing {@code this} as the argument.
    * Whether the range can be obtained is determined by the field.
    *
    * @param field  the field to query the range for, not null
    * @return the range of valid values for the field, not null
    * @throws DateTimeException if the range for the field cannot be obtained
    */
  override def range(field: TemporalField): ValueRange =
    if (field eq OFFSET_SECONDS)
      field.range
    else if (field.isInstanceOf[ChronoField])
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field)
    else
      field.rangeRefinedBy(this)

  /**
    * Gets the value of the specified field from this offset as an {@code int}.
    * <p>
    * This queries this offset for the value for the specified field.
    * The returned value will always be within the valid range of values for the field.
    * If it is not possible to return the value, because the field is not supported
    * or for some other reason, an exception is thrown.
    * <p>
    * If the field is a {@link ChronoField} then the query is implemented here.
    * The {@code OFFSET_SECONDS} field returns the value of the offset.
    * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
    * <p>
    * If the field is not a {@code ChronoField}, then the result of this method
    * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
    * passing {@code this} as the argument. Whether the value can be obtained,
    * and what the value represents, is determined by the field.
    *
    * @param field  the field to get, not null
    * @return the value for the field
    * @throws DateTimeException if a value for the field cannot be obtained
    * @throws ArithmeticException if numeric overflow occurs
    */
  override def get(field: TemporalField): Int =
    if (field eq OFFSET_SECONDS)
      totalSeconds
    else if (field.isInstanceOf[ChronoField])
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field)
    else
      range(field).checkValidIntValue(getLong(field), field)

  /**
    * Gets the value of the specified field from this offset as a {@code long}.
    * <p>
    * This queries this offset for the value for the specified field.
    * If it is not possible to return the value, because the field is not supported
    * or for some other reason, an exception is thrown.
    * <p>
    * If the field is a {@link ChronoField} then the query is implemented here.
    * The {@code OFFSET_SECONDS} field returns the value of the offset.
    * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
    * <p>
    * If the field is not a {@code ChronoField}, then the result of this method
    * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
    * passing {@code this} as the argument. Whether the value can be obtained,
    * and what the value represents, is determined by the field.
    *
    * @param field  the field to get, not null
    * @return the value for the field
    * @throws DateTimeException if a value for the field cannot be obtained
    * @throws ArithmeticException if numeric overflow occurs
    */
  def getLong(field: TemporalField): Long =
    if (field eq OFFSET_SECONDS)
      totalSeconds
    else if (field.isInstanceOf[ChronoField])
      throw new DateTimeException("Unsupported field: " + field)
    else
      field.getFrom(this)

  /**
    * Queries this offset using the specified query.
    * <p>
    * This queries this offset using the specified query strategy object.
    * The {@code TemporalQuery} object defines the logic to be used to
    * obtain the result. Read the documentation of the query to understand
    * what the result of this method will be.
    * <p>
    * The result of this method is obtained by invoking the
    * {@link TemporalQuery#queryFrom(TemporalAccessor)} method on the
    * specified query passing {@code this} as the argument.
    *
    * @tparam R the type of the result
    * @param query  the query to invoke, not null
    * @return the query result, null may be returned (defined by the query)
    * @throws DateTimeException if unable to query (defined by the query)
    * @throws ArithmeticException if numeric overflow occurs (defined by the query)
    */
  override def query[R >: Null](query: TemporalQuery[R]): R =
    if ((query eq TemporalQueries.offset) || (query eq TemporalQueries.zone))
      this.asInstanceOf[R]
    else if ((query eq TemporalQueries.localDate) || (query eq TemporalQueries.localTime) || (query eq TemporalQueries.precision) || (query eq TemporalQueries.chronology) || (query eq TemporalQueries.zoneId))
      null
    else
      query.queryFrom(this)

  /**
    * Adjusts the specified temporal object to have the same offset as this object.
    * <p>
    * This returns a temporal object of the same observable type as the input
    * with the offset changed to be the same as this.
    * <p>
    * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
    * passing {@link ChronoField#OFFSET_SECONDS} as the field.
    * <p>
    * In most cases, it is clearer to reverse the calling pattern by using
    * {@link Temporal#with(TemporalAdjuster)}:
    * <pre>
    * // these two lines are equivalent, but the second approach is recommended
    * temporal = thisOffset.adjustInto(temporal);
    * temporal = temporal.with(thisOffset);
    * </pre>
    * <p>
    * This instance is immutable and unaffected by this method call.
    *
    * @param temporal  the target object to be adjusted, not null
    * @return the adjusted object, not null
    * @throws DateTimeException if unable to make the adjustment
    * @throws ArithmeticException if numeric overflow occurs
    */
  def adjustInto(temporal: Temporal): Temporal = temporal.`with`(OFFSET_SECONDS, totalSeconds)

  /**
    * Compares this offset to another offset in descending order.
    * <p>
    * The offsets are compared in the order that they occur for the same time
    * of day around the world. Thus, an offset of {@code +10:00} comes before an
    * offset of {@code +09:00} and so on down to {@code -18:00}.
    * <p>
    * The comparison is "consistent with equals", as defined by {@link Comparable}.
    *
    * @param other  the other date to compare to, not null
    * @return the comparator value, negative if less, postive if greater
    * @throws NullPointerException if { @code other} is null
    */
  def compareTo(other: ZoneOffset): Int = other.totalSeconds - totalSeconds

  /**
    * Checks if this offset is equal to another offset.
    * <p>
    * The comparison is based on the amount of the offset in seconds.
    * This is equivalent to a comparison by ID.
    *
    * @param obj  the object to check, null returns false
    * @return true if this is equal to the other offset
    */
  override def equals(obj: Any): Boolean =
    obj match {
      case that: ZoneOffset => (this eq that) || (totalSeconds == that.totalSeconds)
      case _ => false
    }

  /**
    * A hash code for this offset.
    *
    * @return a suitable hash code
    */
  override def hashCode: Int = totalSeconds

  /**
    * Outputs this offset as a {@code String}, using the normalized ID.
    *
    * @return a string representation of this offset, not null
    */
  override def toString: String = id

  private def writeReplace: AnyRef = new Ser(Ser.ZONE_OFFSET_TYPE, this)

  /**
    * Defend against malicious streams.
    * @return never
    * @throws InvalidObjectException always
    */
  @throws(classOf[ObjectStreamException])
  private def readResolve: AnyRef = throw new InvalidObjectException("Deserialization via serialization delegate")

  @throws(classOf[IOException])
  private[bp] def write(out: DataOutput): Unit = {
    out.writeByte(Ser.ZONE_OFFSET_TYPE)
    writeExternal(out)
  }

  @throws(classOf[IOException])
  private[bp] def writeExternal(out: DataOutput): Unit = {
    val offsetSecs: Int = totalSeconds
    val offsetByte: Int = if (offsetSecs % 900 == 0) offsetSecs / 900 else 127
    out.writeByte(offsetByte)
    if (offsetByte == 127) {
      out.writeInt(offsetSecs)
    }
  }
}