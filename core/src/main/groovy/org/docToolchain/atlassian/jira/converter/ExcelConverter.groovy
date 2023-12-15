package org.docToolchain.atlassian.jira.converter

import org.apache.commons.codec.binary.Hex
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Hyperlink
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.util.logging.Logger

class ExcelConverter extends IssueConverter {

    private static final Logger LOGGER = Logger.getLogger(ExcelConverter.class.getName())

    private static final String EXTENSION = 'xlsx'

    private Sheet workSheet
    private Workbook workbook
    private FileOutputStream jiraFos
    private Integer lastRow
    private String allHeaders

    ExcelConverter(File targetFolder) {
        super(targetFolder)
    }

    @Override
    def initialize(String fileName, String columns) {
        initialize(fileName, columns, fileName)
    }

    @Override
    def initialize(String fileName, String columns, String caption) {
        this.allHeaders = columns
        String jiraResultsFilename = "${fileName}.${EXTENSION}"
        println("Results will be saved in '${jiraResultsFilename}' file")

        this.outputFile = new File(targetFolder, jiraResultsFilename)
        this.jiraFos = new FileOutputStream(outputFile)
        this.workbook = new XSSFWorkbook()
        this.workSheet = workbook.createSheet(caption)

        String rgbS = "A7A7A7"
        byte[] rgbB = Hex.decodeHex(rgbS)
        XSSFColor color = new XSSFColor(rgbB, null) //IndexedColorMap has no usage until now. So it can be set null.
        XSSFCellStyle headerCellStyle = (XSSFCellStyle) workbook.createCellStyle()
        headerCellStyle.setFillForegroundColor(color)
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

        Row titleRow = workSheet.createRow(0)
        Integer cellNumber = 0
        titleRow.createCell(cellNumber).setCellValue("Key")
        columns.split(",").each { field ->
            titleRow.createCell(++cellNumber).setCellValue("${field.capitalize()}")
        }
        this.lastRow = titleRow.getRowNum()
        titleRow.setRowStyle(headerCellStyle)
    }

    @Override
    def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields) {
        LOGGER.info("Converting issue '${issue.key}' and append to ${outputFile.getName()}")
        Integer cellPosition = 0
        Row row = workSheet.createRow(++lastRow)
        Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL)
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

        for(int colNum = 0; colNum<allHeaders.size()+1;colNum++) {
            workSheet.autoSizeColumn(colNum)
        }
        // Set summary column width slightly wider but fixed size, so it doesn't change with every summary update
        workSheet.setColumnWidth(4, 25*384)


        workbook.write(jiraFos)
    }
}
