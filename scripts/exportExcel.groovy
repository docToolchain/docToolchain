#!/usr/bin/env groovy
// @task
// v4: Export Excel sheets to CSV and AsciiDoc tables using Apache POI

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.DataFormatter

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')

def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def DtcConfig = new GroovyClassLoader(this.class.classLoader).parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
def dtcConfig = DtcConfig.load(docDir, configFile)
def config = dtcConfig.getRaw()

def inputPath = config.inputPath ?: 'src/docs'
def srcDir = new File(docDir, inputPath)
def nl = System.getProperty("line.separator")

println "docToolchain v4 — exportExcel"
println "  srcDir: ${srcDir.absolutePath}"
println ""

def exportFileDir = new File(srcDir, 'excel')
exportFileDir.deleteDir()
exportFileDir.mkdirs()
new File(exportFileDir, 'readme.ad').write("""\
This folder contains exported workbooks from Excel.
These are generated files — **contents will be overwritten with each re-export!**
Use './dtcw exportExcel' to re-export.
""", 'UTF-8')

def tree = []
srcDir.eachFileRecurse { f ->
    if (f.name.endsWith('.xlsx') && !f.name.startsWith('~')) tree << f
}

if (!tree) {
    println "No .xlsx files found in ${srcDir.absolutePath}"
    System.exit(0)
}

def exportSheet = { sheet, evaluator, targetFileName ->
    def targetFileCSV = new File(targetFileName + '.csv')
    def targetFileAD = new File(targetFileName + '.adoc')
    def df = new DataFormatter()
    def regions = []
    sheet.numMergedRegions.times { regions << sheet.getMergedRegion(it) }

    def numCols = 0
    def headerCreated = false
    def emptyRows = 0
    def resetColor = false

    for (int rowNum = 0; rowNum <= sheet.lastRowNum; rowNum++) {
        def row = sheet.getRow(rowNum)
        if (row && !headerCreated) {
            headerCreated = true
            def width = []
            numCols = row.lastCellNum
            numCols.times { width << sheet.getColumnWidth((int) it) }
            // Guard against an empty/zero-width header row: a sum of 0 would
            // divide-by-zero. Fall back to equal column weights.
            def widthSum = (width.sum() ?: 0) as int
            width = widthSum > 0 ? width.collect { Math.round(100 * it / widthSum) } : width.collect { 1 }
            targetFileAD.append('[options="header",cols="' + width.join(',') + '"]' + nl, 'UTF-8')
            targetFileAD.append('|===' + nl, 'UTF-8')
        }
        def data = []
        def style = []
        def colors = []

        if (row && (row?.lastCellNum != -1)) {
            numCols.times { columnIndex ->
                def cell = row.getCell(columnIndex)
                if (cell) {
                    def cellValue = df.formatCellValue(cell, evaluator)
                    if (cellValue.startsWith('*') && cellValue.endsWith('€')) {
                        cellValue = cellValue.substring(1).trim()
                    }
                    def cellStyle = ''
                    def region = regions.find { it.isInRange(cell.rowIndex, cell.columnIndex) }
                    def skipCell = false
                    if (region) {
                        if (region.firstRow == cell.rowIndex && region.firstColumn == cell.columnIndex) {
                            def rowspan = 1 + region.lastRow - region.firstRow
                            def colspan = 1 + region.lastColumn - region.firstColumn
                            if (colspan > 1) cellStyle += "${colspan}"
                            if (rowspan > 1) cellStyle += ".${rowspan}"
                            cellStyle += "+"
                        } else {
                            skipCell = true
                        }
                    }
                    if (!skipCell) {
                        switch (cell.cellStyle.alignment.toString()) {
                            case 'RIGHT': cellStyle += '>'; break
                            case 'CENTER': cellStyle += '^'; break
                        }
                        switch (cell.cellStyle.verticalAlignment.toString()) {
                            case 'BOTTOM': cellStyle += '.>'; break
                            case 'CENTER': cellStyle += '.^'; break
                        }
                        def color = ''
                        try {
                            def rgb = cell.cellStyle.fillForegroundColorColor?.RGB
                            if (rgb) color = nl + "{set:cellbgcolor:#${rgb.encodeHex()}}"
                        } catch (ignored) {}
                        if (color == '' && resetColor) {
                            colors << nl + "{set:cellbgcolor!}"
                            resetColor = false
                        } else {
                            colors << color
                        }
                        if (color != '') resetColor = true
                        data << cellValue
                        style << cellStyle
                    } else {
                        data << ""; colors << ""; style << "skip"
                    }
                } else {
                    data << ""; colors << ""; style << ""
                }
            }
            emptyRows = 0
        } else {
            if (emptyRows < 3) {
                numCols.times { data << ""; colors << ""; style << "" }
                emptyRows++
            } else {
                break
            }
        }

        targetFileCSV.append(data.collect { "\"${it.replaceAll('"', '""')}\"" }.join(',') + nl, 'UTF-8')

        // Fix #1192: remove unnecessary spans
        def prev = ''
        def removed = []
        def useRemoved = true
        style.eachWithIndex { s, i ->
            if (s != "skip") {
                if (s.contains('+')) {
                    def parts = s.split('[+]', -1)
                    def span = parts[0].split('[.]')
                    def current = span.size() > 1 ? span[1] : ""
                    def suffix = parts.size() > 1 ? parts[1] : ""
                    removed << (span[0] != '' ? span[0] + '+' + suffix : suffix)
                    if (i > 0 && current != prev) useRemoved = false
                    prev = current
                } else {
                    removed << s; useRemoved = false
                }
            } else {
                removed << "skip"
            }
        }
        if (useRemoved) style = removed

        targetFileAD.append(data.withIndex().collect { value, index ->
            style[index] == "skip" ? "" :
                style[index] + "| ${value.replaceAll('[|]', '{vbar}').replaceAll("\n", ' +$0') + colors[index]}"
        }.join(nl) + nl * 2, 'UTF-8')
    }
    targetFileAD.append('|===' + nl, 'UTF-8')
    targetFileAD.write(targetFileAD.text.replaceAll("(?m)(\\r?\\n){2,}", nl + nl), 'UTF-8')
}

tree.each { File excel ->
    println "file: ${excel}"
    def excelDir = new File(exportFileDir, excel.name)
    excelDir.mkdirs()
    new FileInputStream(excel).withCloseable { inp ->
        def wb = WorkbookFactory.create(inp)
        try {
            def evaluator = wb.creationHelper.createFormulaEvaluator()
            for (int wbi = 0; wbi < wb.numberOfSheets; wbi++) {
                def sheetName = wb.getSheetAt(wbi).sheetName
                def safeSheetName = sheetName.replaceAll(/[\\/:*?"<>|]/, '_')
                println " -- sheet: ${sheetName}"
                exportSheet(wb.getSheetAt(wbi), evaluator, new File(excelDir, safeSheetName).absolutePath)
            }
        } finally {
            wb.close()
        }
    }
}

println ""
println "Excel export completed successfully."
