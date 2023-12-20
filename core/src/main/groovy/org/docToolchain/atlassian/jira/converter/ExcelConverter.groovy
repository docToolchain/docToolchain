package org.docToolchain.atlassian.jira.converter

import org.apache.commons.codec.binary.Hex
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Hyperlink
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.docToolchain.atlassian.jira.utils.DateUtil

import java.util.logging.Logger

class ExcelConverter extends IssueConverter {

    private static final Logger LOGGER = Logger.getLogger(ExcelConverter.class.getName())

    private static final String EXTENSION = 'xlsx'

    private Sheet workSheet
    private Workbook workbook
    private FileOutputStream jiraFos
    private Integer lastRow
    private List<String> columns

    ExcelConverter(File targetFolder) {
        super(targetFolder)
    }

    def prepareWorkbook(String fileName) {
        this.workbook = new XSSFWorkbook()
        String jiraResultsFilename = "${fileName}.${EXTENSION}"
        println("Results will be saved in '${jiraResultsFilename}' file")

        this.outputFile = new File(targetFolder, jiraResultsFilename)
        this.jiraFos = new FileOutputStream(outputFile)
    }

    @Override
    def initialize(String fileName, List<String> columns) {
        initialize(fileName, columns, fileName)
    }

    @Override
    def initialize(String fileName, List<String> columns, String caption) {
        if(workbook == null) {
            prepareWorkbook(fileName)
        }
        this.columns = columns

        String safeSheetName = WorkbookUtil.createSafeSheetName(caption)
        this.workSheet = workbook.createSheet(safeSheetName)

        String rgbS = "A7A7A7"
        byte[] rgbB = Hex.decodeHex(rgbS)
        XSSFColor color = new XSSFColor(rgbB, null) //IndexedColorMap has no usage until now. So it can be set null.
        XSSFCellStyle headerCellStyle = (XSSFCellStyle) workbook.createCellStyle()
        headerCellStyle.setFillForegroundColor(color)
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

        Row titleRow = workSheet.createRow(0)
        Integer cellNumber = 0
        columns.each { field ->
            titleRow.createCell(cellNumber++).setCellValue("${field.capitalize()}")
        }
        this.lastRow = titleRow.getRowNum()
        titleRow.setRowStyle(headerCellStyle)
    }

    @Override
    def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput,
                         Boolean showAssignee, Boolean showTicketStatus, Boolean showTicketType,
                         Boolean showPriority, Boolean showCreatedDate, Boolean showResolvedDate,
                         Map<String, String> customFields) {
        LOGGER.info("Converting issue '${issue.key}' and append to ${outputFile.getName()}")
        Integer cellPosition = 0
        Row row = workSheet.createRow(++lastRow)
        Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL)
        link.setAddress("${jiraRoot}/browse/${issue.key}")
        Cell cellWithUrl = row.createCell(cellPosition)
        cellWithUrl.setCellValue("${issue.key}")
        cellWithUrl.setHyperlink(link)

        //TODO this is a workaround and this will be removed when we will have a better solution
        if(showPriority) {
            row.createCell(++cellPosition).setCellValue("${issue.fields.priority.name}")
        }
        if(showCreatedDate) {
            row.createCell(++cellPosition).setCellValue("${DateUtil.format(issue.fields.created, jiraDateTimeFormatParse, jiraDateTimeOutput)}")
        }
        if(showResolvedDate) {
            row.createCell(++cellPosition).setCellValue("${issue.fields.resolutiondate ? DateUtil.format(issue.fields.resolutiondate, jiraDateTimeFormatParse, jiraDateTimeOutput) : ''}")
        }
        // end of workaround

        row.createCell(++cellPosition).setCellValue("${issue.fields.summary}")
        if (showAssignee) {
            row.createCell(++cellPosition).setCellValue("${issue.fields.assignee ? issue.fields.assignee.displayName : 'not assigned'}")
        }
        if (showTicketStatus) {
            row.createCell(++cellPosition).setCellValue("${issue.fields.status.name}")
        }
        if (showTicketType) {
            row.createCell(++cellPosition).setCellValue("${issue.fields.issuetype.name}")
        }

        // Custom fields
        customFields.each { field ->
            def position = ++cellPosition
            def foundCustom = issue.fields.find {it.key == field.key}
            row.createCell(position).setCellValue("${foundCustom ? foundCustom.value : '-'}")
        }
    }

    @Override
    def finalizeOutput() {
        for(int colNum = 0; colNum<columns.size()+1;colNum++) {
            workSheet.autoSizeColumn(colNum)
        }
        // Set summary column width slightly wider but fixed size, so it doesn't change with every summary update
        workSheet.setColumnWidth(4, 25*384)
        workbook.write(jiraFos)
    }
}
