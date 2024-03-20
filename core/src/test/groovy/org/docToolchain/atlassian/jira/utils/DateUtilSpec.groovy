package org.docToolchain.atlassian.jira.utils

import spock.lang.Specification

class DateUtilSpec extends Specification {

    def setupSpec() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    def "test format with default format"() {
        when: "formatting a date"
            String formattedDate = DateUtil.format("2019-01-01T12:00:00.000+0000")
        then: "the date is formatted correctly"
            formattedDate == "01.01.2019 12:00:00 UTC"
    }

    def "test format with custom format"() {
        when: "formatting a date"
            String formattedDate = DateUtil.format("2019-01-01T12:00:00.000+0000", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "MM.yyyy HH:mm:ss z")
        then: "the date is formatted correctly"
            formattedDate == "01.2019 12:00:00 UTC"
    }
}
