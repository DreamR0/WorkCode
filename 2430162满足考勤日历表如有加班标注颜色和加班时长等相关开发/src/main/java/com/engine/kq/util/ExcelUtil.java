package com.engine.kq.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import com.customization.qc2430162.Util.KqReportUtil;
import km.org.apache.poi.hssf.usermodel.HSSFCellStyle;
import km.org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.RegionUtil;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.Util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExcelUtil extends BaseBean {
    /*
     * 导出数据
     * */
    public Map export(Map<String, Object> workBook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return export(workBook, request, response,false);
    }
    public Map export(Map<String, Object> workBook, HttpServletRequest request, HttpServletResponse response,boolean isWrap) throws Exception {
        Map<String, Object> returnMap = new HashMap<>();
        String createFile = "";
        try {
            SXSSFWorkbook workbook = new SXSSFWorkbook();                        // 创建工作簿对象
            List<Object> lsSheet = (List<Object>) workBook.get("sheet");
            String fileName = Util.null2String(workBook.get("fileName"));
            if (fileName.length() == 0||true) fileName = "" + UUID.randomUUID();//解决lunix下中文文件无法生成问题
            for (int sheetNum = 0; sheetNum < lsSheet.size(); sheetNum++) {
                Map<String, Object> mySheet = (Map<String, Object>) lsSheet.get(sheetNum);
                String mySheetName = Util.null2String(mySheet.get("sheetName"));
                String sheetTitle = Util.null2String(mySheet.get("sheetTitle"));
                List<Object> sheetMemo = (List<Object>) mySheet.get("sheetMemo");
                List<Object> titleList = (List<Object>) mySheet.get("titleList");
                List<Object> dataList = (List<Object>) mySheet.get("dataList");
                List<Map<String, Object>> constraintList = (List<Map<String, Object>>) mySheet.get("constraintList");
                createFile = Util.null2String(mySheet.get("createFile"));

                Sheet sheet = workbook.createSheet(mySheetName);                     // 创建工作表

                int rowIdx = 0;
                // 产生表格标题行
                Row rowm = sheet.createRow(rowIdx);
                Cell cellTiltle = rowm.createCell(0);

                CellStyle titleStyle = this.getTitleStyle(workbook);//获取列头样式对象
                CellStyle memoStyle = this.getMemoStyle(workbook);//获取备注样式对象
                CellStyle columnTopStyle = this.getColumnTopStyle(workbook);//获取列头样式对象
                CellStyle cellStyle = this.getCellStyle(workbook,isWrap);                    //单元格样式对象
                ////qc2430162 新建colorstyle
                CellStyle colorStyle = null;


                int mergedRegion = titleList.size() - 1;
                if (mergedRegion > 15) mergedRegion = 15;
                mergedRegion = getColLength(titleList);

                CellRangeAddress region = new CellRangeAddress(rowIdx, ++rowIdx, 0, mergedRegion);
                RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                sheet.addMergedRegion(region);
                cellTiltle.setCellStyle(titleStyle);
                cellTiltle.setCellValue(sheetTitle);

                for (int i = 0; sheetMemo != null && i < sheetMemo.size(); i++) {
                    rowm = sheet.createRow(++rowIdx);
                    Cell cellMemo = rowm.createCell(0);
                    region = new CellRangeAddress(rowIdx, ++rowIdx, 0, mergedRegion);
                    RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
                    RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
                    RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
                    RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
                    sheet.addMergedRegion(region);
                    cellMemo.setCellStyle(memoStyle);
                    cellMemo.setCellValue(Util.null2String(sheetMemo.get(i)));
                }

//        // 定义所需列数
//        int columnNum = titleList.size();
//        HSSFRow rowRowName = sheet.createRow(++rowIdx);                // 在索引2的位置创建行(最顶端的行开始的第二行)
//
//        // 将列头设置到sheet的单元格中
//        for (int n = 0; n < columnNum; n++) {
//          Map title = (Map) titleList.get(n);
//
//          HSSFCell cellRowName = rowRowName.createCell(n);                //创建列头对应个数的单元格
//          cellRowName.setCellType(HSSFCell.CELL_TYPE_STRING);                //设置列头单元格的数据类型
//          HSSFRichTextString text = new HSSFRichTextString(Util.null2String(title.get("title")));
//          cellRowName.setCellValue(text);                                    //设置列头单元格的值
//          cellRowName.setCellStyle(columnTopStyle);                        //设置列头单元格样式
//          if (n == 0) {
//            sheet.setColumnWidth(n, Util.getIntValue(Util.null2String(title.get("width"))));
//          } else {
//            sheet.setColumnWidth(n, Util.getIntValue(Util.null2String(title.get("width"))));
//          }
//        }

                rowIdx = this.initDynamicTitle(sheet, titleList, columnTopStyle, rowIdx);

                //将查询出的数据设置到sheet对应的单元格中
                for (int i = 0; dataList != null && i < dataList.size(); i++) {
                    List<Object> obj = (List<Object>) dataList.get(i);//遍历每个对象
                    Row row = sheet.createRow(i + rowIdx + 1);//创建所需的行数
                    for (int j = 0; j < obj.size(); j++) {
                        Cell cell = null;   //设置单元格的数据类型
                        cell = row.createCell(j, CellType.STRING);
                        //qc2430162 这里做个判断如果包含加班，给单元格输出颜色
                        KqReportUtil kqReportUtil = new KqReportUtil();
                        boolean isSet = kqReportUtil.setCellColor(Util.null2String(obj.get(j)));
                        if (isSet){
                            new BaseBean().writeLog("==zj==(导出-cellStyle)" + Util.null2String(obj.get(j)));
                            colorStyle = this.getCellStyle(workbook,isWrap);
                            colorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                            colorStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                            cell.setCellStyle(colorStyle);
                            cell.setCellValue(Util.null2String(obj.get(j)));                        //设置单元格的值

                        }else {
                            cell.setCellValue(Util.null2String(obj.get(j)));                        //设置单元格的值
                            cell.setCellStyle(cellStyle);                                    //设置单元格样式
                        }

                    }
                }

//        int columnNum = this.getColumnLength(titleList);
//        //让列宽随着导出的列长自动适应
//        for (int colNum = 0; colNum < columnNum; colNum++) {
//          int columnWidth = sheet.getColumnWidth(colNum) / 256;
//          for (int rowNum = 0; rowNum < sheet.getLastRowNum(); rowNum++) {
//            HSSFRow currentRow;
//            //当前行未被使用过
//            if (sheet.getRow(rowNum) == null) {
//              currentRow = sheet.createRow(rowNum);
//            } else {
//              currentRow = sheet.getRow(rowNum);
//            }
//            if (currentRow.getCell(colNum) != null) {
//              //取得当前的单元格
//              HSSFCell currentCell = currentRow.getCell(colNum);
//              //如果当前单元格类型为字符串
//              if (currentCell.getCellTypeEnum() == CellType.STRING) {
//                int length = currentCell.getStringCellValue().getBytes().length;
//                if (columnWidth < length) {
//                  //将单元格里面值大小作为列宽度
//                  columnWidth = length;
//                }
//              }
//            }
//          }
//          //再根据不同列单独做下处理
//          if (colNum == 0) {
//            sheet.setColumnWidth(colNum, (columnWidth - 2) * 256);
//          } else {
//            sheet.setColumnWidth(colNum, (columnWidth + 4) * 256);
//          }
//        }
                for (int i = 0; constraintList != null && i < constraintList.size(); i++) {
                    Map<String, Object> constraint = constraintList.get(i);
                    int firstRow = Util.getIntValue(Util.null2String(constraint.get("firstRow")), 0);
                    int endRow = Util.getIntValue(Util.null2String(constraint.get("endRow")), 0);
                    int firstCol = Util.getIntValue(Util.null2String(constraint.get("firstCol")), 0);
                    int endCol = Util.getIntValue(Util.null2String(constraint.get("endCol")), 0);
                    //设置下拉框数据
                    String[] datas = (String[]) constraint.get("constraintDatas");
                    String hiddenSheetName = "constraintDataSheet"+i;
                    Sheet hiddenSheet = workbook.createSheet(hiddenSheetName);
                    workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);
                    for (int j = 0; j< datas.length; j++) {
                        hiddenSheet.createRow(j).createCell(0).setCellValue(datas[j]);
                    }
                    DataValidationHelper helper = sheet.getDataValidationHelper();
                    String formulaId = hiddenSheetName + "!$A$1:$A$" + datas.length;
                    DataValidationConstraint dataValidationConstraint = helper.createFormulaListConstraint(formulaId);
                    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
                    DataValidation dataValidation = helper.createValidation(dataValidationConstraint, cellRangeAddressList);
                    //处理Excel兼容性问题
                    if (dataValidation instanceof DataValidation) {
                        dataValidation.setSuppressDropDownArrow(true);
                        dataValidation.setShowErrorBox(true);
                    } else {
                        dataValidation.setSuppressDropDownArrow(false);
                    }
                    sheet.addValidationData(dataValidation);
                }


//                for (int i = 0; constraintList != null && i < constraintList.size(); i++) {
//                    Map<String, Object> constraint = constraintList.get(i);
//                    int firstRow = Util.getIntValue(Util.null2String(constraint.get("firstRow")), 0);
//                    int endRow = Util.getIntValue(Util.null2String(constraint.get("endRow")), 0);
//                    int firstCol = Util.getIntValue(Util.null2String(constraint.get("firstCol")), 0);
//                    int endCol = Util.getIntValue(Util.null2String(constraint.get("endCol")), 0);
//
//                    //设置下拉框数据
//                    String[] datas = (String[]) constraint.get("constraintDatas");
//                    DataValidationHelper helper = sheet.getDataValidationHelper();
//                    DataValidationConstraint dataValidationConstraint = helper.createExplicitListConstraint(datas);
//                    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
//                    DataValidation dataValidation = helper.createValidation(dataValidationConstraint, cellRangeAddressList);
//                    //处理Excel兼容性问题
//                    if (dataValidation instanceof DataValidation) {
//                        dataValidation.setSuppressDropDownArrow(true);
//                        dataValidation.setShowErrorBox(true);
//                    } else {
//                        dataValidation.setSuppressDropDownArrow(false);
//                    }
//                    sheet.addValidationData(dataValidation);
//                }
            }

            if (workbook != null) {
                if (createFile.equals("1")) {
                    String filePath = GCONST.getRootPath() + "/hrm/kq/tmpFile/";
                    File file = new File(filePath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    fileName = fileName +"("+DateUtil.getNowDateTimeStr()+")";
                    String url = filePath + fileName + ".xlsx";
                    String realUrl = "/hrm/kq/tmpFile/" + fileName + ".xlsx";
                    FileOutputStream fOut = new FileOutputStream(url);
                    workbook.write(fOut);
                    fOut.flush();
                    fOut.close();
                    returnMap.put("url", realUrl);
                } else {
                    response.reset();
                    response.setContentType("application/vnd.ms-excel;charset=utf-8");
                    response.setCharacterEncoding("utf-8");
                    String header = request.getHeader("User-Agent").toUpperCase();
                    if (header.contains("MSIE") || header.contains("TRIDENT") || header.contains("EDGE")) {
                        fileName = URLEncoder.encode(fileName, "utf-8");
                        fileName = fileName.replace("+", "%20"); //IE下载文件名空格变+号问题
                    } else {
                        fileName = new String(fileName.getBytes("utf-8"), "ISO_8859_1");
                    }
                    response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
                    response.setContentType("application/msexcel");
                    response.setContentType("application/x-msdownload");
                    OutputStream responseOutput = response.getOutputStream();
                    workbook.write(responseOutput);
                    responseOutput.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnMap;
    }

    /*
     * 列头单元格样式
     */
    private CellStyle getMemoStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 10);
        //字体加粗
        font.setBold(true);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(IndexedColors.BLACK.index);
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(IndexedColors.BLACK.index);
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(IndexedColors.BLACK.index);
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(IndexedColors.BLACK.index);
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.LEFT);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;

    }

    /*
     * 列头单元格样式
     */
    private CellStyle getTitleStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 14);
        //字体加粗
        font.setBold(true);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(IndexedColors.BLACK.index);
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(IndexedColors.BLACK.index);
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(IndexedColors.BLACK.index);
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(IndexedColors.BLACK.index);
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;

    }

    /*
     * 列头单元格样式
     */
    private CellStyle getColumnTopStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 11);
        //字体加粗
        font.setBold(true);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(IndexedColors.BLACK.index);
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(IndexedColors.BLACK.index);
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(IndexedColors.BLACK.index);
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(IndexedColors.BLACK.index);
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
        style.setWrapText(true);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;

    }

    /*
     * 列数据信息单元格样式
     */
    private CellStyle getCellStyle(SXSSFWorkbook workbook,boolean isWrap) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        //font.setFontHeightInPoints((short)10);
        //字体加粗
        //font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(IndexedColors.BLACK.index);
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(IndexedColors.BLACK.index);
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(IndexedColors.BLACK.index);
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(IndexedColors.BLACK.index);
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
//        style.setWrapText(false);
        if(isWrap){
            style.setWrapText(true);
        }else{
            style.setWrapText(false);
        }
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.LEFT);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;

    }

    private CellStyle getCellStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        //font.setFontHeightInPoints((short)10);
        //字体加粗
        //font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(IndexedColors.BLACK.index);
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(IndexedColors.BLACK.index);
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(IndexedColors.BLACK.index);
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(IndexedColors.BLACK.index);
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.LEFT);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;

    }

    public static void main(String[] args) {
        String groupid = "1";
        String filename = "c://33e2a90d-3347-490c-afff-21de0ef3e3af.xlsx";
        int startRow = 3;
        RecordSet rs = new RecordSet();
        String sql = "";
        try {
            //必要的权限判断
            List<List> lsParams = null;
            List params = null;

            Workbook workbook = WorkbookFactory.create(new FileInputStream(filename));
            for (int idx = 0; idx < workbook.getNumberOfSheets(); idx++) {
                Sheet sheet = workbook.getSheetAt(idx);
                Row row = null;
                Cell cell = null;
                lsParams = new ArrayList();
                for (int i = startRow; startRow <= sheet.getLastRowNum() && i <= sheet.getLastRowNum(); i++) {
                    row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }
                    params = new ArrayList();
                    for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                        cell = row.getCell((short) cellIndex);
                        if (cell == null) continue;
                        params.add(getCellValue(cell).trim());
                    }
                    params.add(groupid);
                    if (Util.null2String(params.get(0)).length() > 0) {
                        lsParams.add(params);
                    }
                }
                if (idx == 0) {
                    sql = "insert into kq_loaction (loactionname,longitude,latitude,address,groupid) values (?,?,?,?,?) ";
                } else {
                    sql = "insert into kq_wifi (wifiname,mac,groupid) values (?,?,?) ";
                }
                rs.executeBatchSql(sql, lsParams);
            }
        } catch (Exception e) {
            new BaseBean().writeLog(e);
        }
    }

    public int initDynamicTitle(Sheet sheet, List<Object> columns, CellStyle columnTopStyle, int rowIdx) {
      int cur_rowIndx = rowIdx+1;
      //title 总行数
      int title_rows = cur_rowIndx+getRowNums(columns)-1;
      createHeaderRow(sheet, cur_rowIndx, 0,columns,columnTopStyle,title_rows);
      return title_rows;
    }

    /**
     * 创建表头
     */
    private int createHeaderRow(Sheet sheet, int index, int cellIndex,
        List<Object> columnName, CellStyle columnTopStyle, int title_rows) {
      Row row = sheet.getRow(index) == null ? sheet.createRow(index) : sheet.getRow(index);
      int rows = getRowNums(columnName);
      for (int i = 0, exportFieldTitleSize = columnName.size(); i < exportFieldTitleSize; i++) {
        Map<String,Object> column = (Map<String, Object>) columnName.get(i);
        String title = Util.null2String(column.get("title"));
        createStringCell(row, cellIndex, title, columnTopStyle);
        if (column.get("children") != null) {
          List<Object> childchildColumns = (List<Object>) column.get("children");
          // 保持原来的
          int tempCellIndex = cellIndex;
          cellIndex = createHeaderRow(sheet, rows == 1 ? index : index + 1, cellIndex, childchildColumns,columnTopStyle,
              title_rows);
          if (childchildColumns.size() > 1) {
            addMergedRegion(sheet, index, index, tempCellIndex, cellIndex - 1);
          }
          cellIndex--;
        }else{
          addMergedRegion(sheet, index, title_rows, cellIndex, cellIndex);
        }
        cellIndex++;
      }
      return cellIndex;
    }

  /**
   * 合并单元格
   * @param sheet
   * @param firstRow
   * @param lastRow
   * @param firstCol
   * @param lastCol
   */
    public void addMergedRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
      try {
        if(firstRow == lastRow && firstCol == lastCol){
          return ;
        }
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        sheet.addMergedRegion(region);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    /**
     * 创建文本类型的Cell
     */
    public void createStringCell(Row row, int cellIndex, String title, CellStyle columnTopStyle) {
      Cell cellRowName = row.createCell(cellIndex);                  //创建列头对应个数的单元格
      cellRowName.setCellType(CellType.STRING);                //设置列头单元格的数据类型
      cellRowName.setCellValue(title);                                    //设置列头单元格的值
      cellRowName.setCellStyle(columnTopStyle);                         //设置列头单元格样式
    }

    public int initTitle(Sheet sheet, List<Object> columns, CellStyle columnTopStyle, int rowIdx) {
        List<Object> lsMergedRegion = new ArrayList<>();
        Map<String, Object> mergedRegion = null;
        int firstTitleRow = rowIdx + 1;
        int lastTitleRow = rowIdx + 2;
        Row rowRowName = sheet.createRow(++rowIdx);                 // 在索引2的位置创建行(最顶端的行开始的第二行)
        Row childRowRowName = null;
        // 定义所需列数
        Map<String, Object> column = null;
        List<Object> childColumn = null;
        int colIdx = 0;
        for (int i = 0; i < columns.size(); i++) {
            column = (Map<String, Object>) columns.get(i);
            childColumn = (List<Object>) column.get("children");
            Cell cellRowName = rowRowName.createCell(colIdx++);                  //创建列头对应个数的单元格
            cellRowName.setCellType(CellType.STRING);                //设置列头单元格的数据类型
            cellRowName.setCellValue(Util.null2String(column.get("title")));                                    //设置列头单元格的值
            cellRowName.setCellStyle(columnTopStyle);                         //设置列头单元格样式

            if (Util.getIntValue(Util.null2String(column.get("rowSpan"))) == 2) {
                if (childRowRowName == null) {
                    childRowRowName = sheet.createRow(++rowIdx);
                }
                mergedRegion = new HashMap<String, Object>();
                mergedRegion.put("startRow", firstTitleRow);
                mergedRegion.put("overRow", lastTitleRow);
                mergedRegion.put("startCol", colIdx - 1);
                mergedRegion.put("overCol", colIdx - 1);
                lsMergedRegion.add(mergedRegion);
            }

            if (childColumn != null) {
                colIdx--;
                mergedRegion = new HashMap<String, Object>();
                mergedRegion.put("startRow", firstTitleRow);
                mergedRegion.put("overRow", firstTitleRow);
                mergedRegion.put("startCol", colIdx);
                for (int j = 0; j < childColumn.size(); j++) {
                    column = (Map<String, Object>) childColumn.get(j);
                    cellRowName = childRowRowName.createCell(colIdx++);                  //创建列头对应个数的单元格
                    cellRowName.setCellType(CellType.STRING);                //设置列头单元格的数据类型
                    cellRowName.setCellValue(Util.null2String(column.get("title")));                                    //设置列头单元格的值
                    cellRowName.setCellStyle(columnTopStyle);
                }
                mergedRegion.put("overCol", colIdx - 1);
                if (childColumn.size() > 1) {
                    lsMergedRegion.add(mergedRegion);
                }
            }
        }

        for (int i = 0; i < lsMergedRegion.size(); i++) {
            mergedRegion = (Map<String, Object>) lsMergedRegion.get(i);
            Integer startrow = Util.getIntValue(Util.null2String(mergedRegion.get("startRow")));
            Integer overrow = Util.getIntValue(Util.null2String(mergedRegion.get("overRow")));
            Integer startcol = Util.getIntValue(Util.null2String(mergedRegion.get("startCol")));
            Integer overcol = Util.getIntValue(Util.null2String(mergedRegion.get("overCol")));
            CellRangeAddress region = new CellRangeAddress(startrow, overrow, startcol, overcol);
            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
            sheet.addMergedRegion(region);
        }
        return rowIdx;
    }



  /**
   * 获取具体总共有多少列
   * @param columns
   * @return
   */
  private int getColLength(List<Object> columns) {
    int length = -1;// 从0开始计算单元格的
    for(int i =0 ; i < columns.size() ; i++){
      Map<String,Object> column = (Map<String, Object>) columns.get(i);
      if (column.get("children") != null) {
        List<Object> childchildColumns = (List<Object>) column.get("children");
        length += getColLength(childchildColumns) + 1;
      } else {
        length++;
      }
    }
    return length;
  }

  /**
   * 获取具体总共有多少行
   */
  public int getRowNums(List<Object> columns) {
    int cnt = 1;
    for (int i = 0; i < columns.size(); i++) {
      Map<String,Object> column = (Map<String, Object>) columns.get(i);
      if (column.get("children") != null) {
        List<Object> childchildColumns = (List<Object>) column.get("children");
        int tmpcnt = 1+getRowNums(childchildColumns);
        if(tmpcnt > cnt){
          cnt = tmpcnt;
        }
      }
    }
    if(cnt > 1){
      return cnt;
    }else{
      return 1;
    }
  }

    public int getColumnLength(List<Object> columns) {
        int columnLength = 0;
        Map<String, Object> column = null;
        for (int i = 0; i < columns.size(); i++) {
            column = (Map<String, Object>) columns.get(i);
            if (column.get("colSpan") != null) {
                columnLength += Util.getIntValue(Util.null2String(column.get("colSpan")));
            } else {
                columnLength++;
            }
        }
        return columnLength;
    }

    /**
     * /**
     * 获取excel单元格值
     *
     * @param cell 要读取的单元格对象
     * @return
     */
    public static String getCellValue(Cell cell) {
        String cellValue = "";
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case BOOLEAN: // 得到Boolean对象的方法
                cellValue = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {// 先看是否是日期格式
                    SimpleDateFormat sft = new SimpleDateFormat("yyyy-MM-dd");
                    cellValue = String.valueOf(sft.format(cell.getDateCellValue())); // 读取日期格式
                } else {
                    cellValue = String.valueOf(new Double(cell.getNumericCellValue())); // 读取数字
                    if (cellValue.endsWith(".0"))
                        cellValue = cellValue.substring(0, cellValue.indexOf("."));
                }
                break;
            case FORMULA: // 读取公式
                cellValue = cell.getCellFormula();
                break;
            case STRING: // 读取String
                cellValue = cell.getStringCellValue();
                break;
        }

        return cellValue;
    }
}
