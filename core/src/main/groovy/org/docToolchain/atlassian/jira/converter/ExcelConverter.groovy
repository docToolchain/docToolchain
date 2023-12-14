package org.docToolchain.atlassian.jira.converter

import org.apache.commons.codec.binary.Hex
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Hyperlink
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ExcelConverter extends IssueConverter {

    def ws
    Workbook wb
    FileOutputStream jiraFos
    CreationHelper hyperlinkHelper
    int lastRow

    @Override
    def prepareOutputFile(String fileName, File targetFolder, String allHeaders) {
        def extension = 'xlsx'
        def jiraResultsFilename = "${fileName}.${extension}"
        println(">> Results will be saved in '${jiraResultsFilename}' file")

        def jiraDataXls = new File(targetFolder, jiraResultsFilename)
        this.jiraFos = new FileOutputStream(jiraDataXls)

        this.wb = new XSSFWorkbook();
        this.hyperlinkHelper = wb.getCreationHelper();
        def sheetName = "${fileName}"
        def ws = wb.createSheet(sheetName)

        String rgbS = "A7A7A7"
        byte[] rgbB = Hex.decodeHex(rgbS)
        XSSFColor color = new XSSFColor(rgbB, null) //IndexedColorMap has no usage until now. So it can be set null.
        XSSFCellStyle headerCellStyle = (XSSFCellStyle) wb.createCellStyle()
        headerCellStyle.setFillForegroundColor(color)
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

        def titleRow = ws.createRow(0);
        int cellNumber = 0;
        titleRow.createCell(cellNumber).setCellValue("Key")
        allHeaders.split(",").each {field ->
            titleRow.createCell(++cellNumber).setCellValue("${field.capitalize()}")
        }
        this.lastRow = titleRow.getRowNum()
        titleRow.setRowStyle(headerCellStyle)
        this.ws = ws
    }

    @Override
    def convertAndAppend(File jiraDataAsciidoc, issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields) {
        int cellPosition = 0
        def row = ws.createRow(++lastRow)
        Hyperlink link = hyperlinkHelper.createHyperlink(HyperlinkType.URL)
        link.setAddress("${jiraRoot}/browse/${issue.key}")
        Cell cellWithUrl = row.createCell(cellPosition)
        cellWithUrl.setCellValue("${issue.key}")
        cellWithUrl.setHyperlink(link)

        row.createCell(++cellPosition).setCellValue("${issue.fields.priority.name}")
        row.createCell(++cellPosition).setCellValue("${Date.parse(jiraDateTimeFormatParse, issue.fields.created).format(jiraDateTimeOutput)}")
        row.createCell(++cellPosition).setCellValue("${issue.fields.resolutiondate ? Date.parse(jiraDateTimeFormatParse, issue.fields.resolutiondate).format(jiraDateTimeOutput) : ''}")
        row.createCell(++cellPosition).setCellValue("${issue.fields.summary}")
        row.createCell(++cellPosition).setCellValue("${issue.fields.assignee ? issue.fields.assignee.displayName : ''}")
        row.createCell(++cellPosition).setCellValue("${issue.fields.status.name}")

        // Custom fields
        customFields.each { field ->
            def position = ++cellPosition
            def foundCustom = issue.fields.find {it.key == field.key}
            row.createCell(position).setCellValue("${foundCustom ? foundCustom.value : '-'}")
        }
    }
}
