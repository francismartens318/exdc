/*
 * Copyright (c) 2024 Exalate (https://exalate.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package customconnectornode.discourse.transform

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.ParseException
import java.text.SimpleDateFormat

class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class)
    private static final datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    private static final RFC1123Pattern = "EEE, dd MMM yyyy HH:mm:ss zzz"

    static Date getDateFromString(String dateString) {
        if (!dateString) {
            return null
        }

        // Try RFC 1123 format first
        SimpleDateFormat formatter = new SimpleDateFormat(RFC1123Pattern, Locale.US)

        try {
            return formatter.parse(dateString)
        } catch (Exception e) {
            if (! (e instanceof ParseException)) {
                logger.error("Error parsing date string: ${dateString}", e)
                return null
            }
        }

        // Fall back to original format
        formatter = new SimpleDateFormat(datePattern)
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))

        try {
            return formatter.parse(dateString)
        } catch (Exception e) {
            logger.error("Error parsing date string: ${dateString}", e)
            return null
        }
    }

    static String getStringFromDate(Date date) {
        if (!date) return null

        SimpleDateFormat formatter = new SimpleDateFormat(datePattern)
        return formatter.format(date)
    }
}
