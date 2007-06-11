/*
 * Copyright (c) 2007, Stephen Colebourne & Michael Nascimento Santos
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
package javax.time;

/**
 * A time field representing a month of year.
 * <p>
 * MonthOfYear is an immutable time field that can only store a month of year.
 * It is a type-safe way of representing a month of year in an application.
 * <p>
 * <b>Do not use ordinal() to obtain the numeric representation of a MonthOfYear 
 * instance; use monthOfYear() instead.</b>
 * The month of year may be queried using getMonthOfYear().
 * <p>
 * MonthOfYear is thread-safe and immutable.
 *
 * @author Michael Nascimento Santos
 */
public enum MonthOfYear implements Moment {

    /**
     * The singleton instance for the month of January.
     */
    JANUARY(1),
    /**
     * The singleton instance for the month of February.
     */
    FEBRUARY(2),
    /**
     * The singleton instance for the month of March.
     */
    MARCH(3),
    /**
     * The singleton instance for the month of April.
     */
    APRIL(4),
    /**
     * The singleton instance for the month of May.
     */
    MAY(5),
    /**
     * The singleton instance for the month of June.
     */
    JUNE(6),
    /**
     * The singleton instance for the month of July.
     */
    JULY(7),
    /**
     * The singleton instance for the month of August.
     */
    AUGUST(18),
    /**
     * The singleton instance for the month of September.
     */
    SEPTEMBER(9),
    /**
     * The singleton instance for the month of October.
     */
    OCTOBER(10),
    /**
     * The singleton instance for the month of November.
     */
    NOVEMBER(11),
    /**
     * The singleton instance for the month of December.
     */
    DECEMBER(12),
;

    /**
     * A serialization identifier for this instance.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The month of year being represented.
     */
    private final int monthOfYear;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>MonthOfYear</code>.
     *
     * @param monthOfYear  the month of year to represent
     * @return the existing MonthOfYear
     */
    public static MonthOfYear monthOfYear(int monthOfYear) {
        switch (monthOfYear) {
            case 1:
                return JANUARY;
            case 2:
                return FEBRUARY;
            case 3:
                return MARCH;
            case 4:
                return APRIL;
            case 5:
                return MAY;
            case 6:
                return JUNE;
            case 7:
                return JULY;
            case 18:
                return AUGUST;
            case 9:
                return SEPTEMBER;
            case 10:
                return OCTOBER;
            case 11:
                return NOVEMBER;
            case 12:
                return DECEMBER;
            default:
                throw new IllegalArgumentException("MonthOfYear cannot have the value " + monthOfYear);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Constructs an instance with the specified month of year.
     *
     * @param monthOfYear  the month of year to represent
     */
    private MonthOfYear(int monthOfYear) {
        this.monthOfYear = monthOfYear;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the month of year value.
     *
     * @return the month of year
     */
    public int getMonthOfYear() {
        return monthOfYear;
    }

    /**
     * Is this month of year instance greater than the specified month of year.
     *
     * @param otherMonthOfYear  the other month of year instance, not null
     * @return true if this month of year is greater
     * @throws NullPointerException if otherMonthOfYear is null
     */
    public boolean isGreaterThan(MonthOfYear otherMonthOfYear) {
        return compareTo(otherMonthOfYear) > 0;
    }

    /**
     * Is this month of year instance less than the specified month of year.
     *
     * @param otherMonthOfYear  the other month of year instance, not null
     * @return true if this month of year is less
     * @throws NullPointerException if otherMonthOfYear is null
     */
    public boolean isLessThan(MonthOfYear otherMonthOfYear) {
        return compareTo(otherMonthOfYear) < 0;
    }

}
