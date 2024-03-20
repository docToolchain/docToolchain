package org.docToolchain.atlassian.jira.utils

import java.text.SimpleDateFormat

class DateUtil {

    static final String DEFAULT_PARSE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    static final String DEFAULT_OUTPUT_FORMAT = "dd.MM.yyyy HH:mm:ss z"

    static String format(String inputDate) {
        return format(inputDate, DEFAULT_PARSE_FORMAT, DEFAULT_OUTPUT_FORMAT)
    }

    static String format(String inputDate, String jiraDateTimeFormatParse, String jiraDateTimeOutput) {
        SimpleDateFormat inputFormat = new SimpleDateFormat(jiraDateTimeFormatParse)
        SimpleDateFormat outputFormat = new SimpleDateFormat(jiraDateTimeOutput)

        Date parsedDate = inputFormat.parse(inputDate)
        return outputFormat.format(parsedDate)
    }
}
