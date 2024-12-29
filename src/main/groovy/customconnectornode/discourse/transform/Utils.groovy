package customconnectornode.discourse.transform

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat

class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class)

    static Date getDateFromString(String dateString) {
        try {
                if (!dateString) {
                    return null
                }

                def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
                return formatter.parse(dateString)
            }
        catch (Exception e) {
                logger.error("Error parsing date string: ${dateString}", e)
                return null
            }

    }
}
