package com.engine.kq.biz;

import com.engine.kq.bean.ImportSetting;
import com.engine.kq.timer.KQQueue;
import com.engine.kq.timer.KQTaskBean;
import com.engine.kq.wfset.util.SplitActionUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.general.StaticObj;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.common.Tools;
import weaver.interfaces.datasource.DataSource;
import weaver.systeminfo.SystemEnv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class KQScheduleSignImport extends BaseBean {
  private List errorInfo = new ArrayList();
  private String fileName = "";
  private String keyField = "";
  private int userlanguage = 7;   //登录语言

  public KQScheduleSignImport() {
  }

  public KQScheduleSignImport(Map<String, Object> params) {
    this.keyField = StringUtil.vString(params.get("keyField"), "workcode");
    this.fileName = Util.null2String(params.get("excelfile"));
  }

  /**
   * 私有方法，获取Excel表格对应表格中的数据
   *
   * @param cell Excel表格的行
   * @param row  Excel表格的列
   * @return Excel表格对应表格中的数据
   */
  private String getCellValue(HSSFCell cell, HSSFRow row) {
    if (cell == null) return "";
    String cellValue = "";
    switch (cell.getCellType()) {

      case NUMERIC:
        if (HSSFDateUtil.isCellDateFormatted(cell)) {
          SimpleDateFormat sdf = null;
          if (cell.getCellStyle().getDataFormat() == HSSFDataFormat.getBuiltinFormat("h:mm:ss")) {
            sdf = new SimpleDateFormat("HH:mm:ss");
          } else {// 日期
            sdf = new SimpleDateFormat("yyyy-MM-dd");
          }
          Date date = cell.getDateCellValue();
          cellValue = sdf.format(date);
        } else {
          cellValue = new java.text.DecimalFormat("0").format(cell.getNumericCellValue());
        }
        break;
      case STRING:
        cellValue = cell.getStringCellValue();
        break;
      case FORMULA:
        cellValue = (DateFormat.getDateInstance().format((cell.getDateCellValue()))).toString();

        break;
      default:

        break;
    }
    return cellValue;
  }

  /**
   * 将Excel表格中的数据导入数据库
   *
   */
  public synchronized void ExcelToDB() {
    String sql = "";
    BatchRecordSet bRs = new BatchRecordSet();
    List<List<Object>> lsParams = new ArrayList<>();
    List<Object> params = null;
    List<List<Object>> lsDelParams = new ArrayList<>();
    List<Object> delParams = null;

    List<String> lsFormatData = new ArrayList<>();
    List<List<Object>> lsFormatParams = new ArrayList<>();
    List<Object> formatParams = null;

    try {
      ImageFileManager manager = new ImageFileManager();
      manager.getImageFileInfoById(Util.getIntValue(this.fileName));
      HSSFWorkbook workbook = new HSSFWorkbook(new POIFSFileSystem(manager.getInputStream()));
      HSSFSheet sheet = workbook.getSheetAt(0);
      HSSFRow row = null;

      int rowsNum = sheet.getLastRowNum();
      for (int i = 1; i < rowsNum + 1; i++) {
        row = sheet.getRow(i);
        if (row==null||!ScanRow(row)) continue;
        int tmpUserId = 0;
        String tmpLoginid = "";
        String tmpWorkcode = "";
        String tmpLastname = "";
        String tmpSigndate = "";
        String tmpSigntime = "";
        String tmpClientaddress = "";
        int tmpIsincom = 1;
        String tmpLongitude = "";
        String tmpLatitude = "";
        String tmpAddr = "";
        for (int j = 0; j < row.getLastCellNum(); j++) {
          if (j == 0) {
            tmpLoginid = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
          } else if (j == 1) {
            tmpWorkcode = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
          } else if (j == 2) {
            tmpLastname = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
          } else if (j == 3) {
            tmpSigndate = Util.null2String(this.getCellValue(row.getCell((short) j), row));
            if(tmpSigndate.length()==0)continue;
            SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(SDF.parse(tmpSigndate + " 00:00:00"));
            tmpSigndate = TimeUtil.getFormartString(calendar, "yyyy-MM-dd");
          } else if (j == 4) {
            tmpSigntime = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
            if(tmpSigntime.length()==0)continue;
            if (tmpSigntime.length() > 0) {
              String[] a = tmpSigntime.split(":");
              if (a != null && a.length < 3) {
                tmpSigntime += ":00";
              }
            }
            SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(SDF.parse("1999-01-01 " + tmpSigntime));
            tmpSigntime = TimeUtil.getFormartString(calendar, "HH:mm:ss");
          } else if (j == 5) {
            tmpClientaddress = Util.null2String(this.getCellValue(row.getCell((short) j), row));
          }else if (j == 6){
            tmpAddr = Util.null2String(this.getCellValue(row.getCell((short) j),row));
          }
        }
        if(tmpLoginid.length()==0&&tmpWorkcode.length()==0&&tmpLastname.length()==0)continue;
        if (this.keyField.equals("loginid")) {
          tmpUserId = getUserId(tmpLoginid);
        } else if (this.keyField.equals("workcode")) {
          tmpUserId = getUserId(tmpWorkcode);
        } else if (this.keyField.equals("lastname")) {
          tmpUserId = getUserId(tmpLastname);
        }
        if(tmpUserId<=0)continue;
        params = new ArrayList<>();
        params.add(tmpUserId);
        params.add(1);
        params.add(tmpSigndate);
        params.add(tmpSigntime);
        params.add(tmpClientaddress);
        params.add(tmpIsincom);
        params.add(1);
        params.add("importExcel");
        params.add(tmpLongitude);
        params.add(tmpLatitude);
        params.add(tmpAddr);
        lsParams.add(params);

        String formatData = tmpUserId+"|"+tmpSigndate;
        if(!lsFormatData.contains(formatData)){
          lsFormatData.add(formatData);
        }
      }
      Map<String,List<String>> overtimeMap = Maps.newHashMap();
      List<String> overtimeList = Lists.newArrayList();
      //刷新报表数据
      for(int i=0;lsFormatData!=null&&i<lsFormatData.size();i++){
        formatParams = new ArrayList<>();
        String[] formatData = Util.splitString(lsFormatData.get(i),"|");
        String date_1 = DateUtil.addDate(formatData[1], -1);
        formatParams.add(formatData[0]);
        formatParams.add(date_1);
        lsFormatParams.add(formatParams);

        formatParams = new ArrayList<>();
        formatParams.add(formatData[0]);
        formatParams.add(formatData[1]);
        lsFormatParams.add(formatParams);

        delParams = new ArrayList<>();
        delParams.add(formatData[0]);
        delParams.add(formatData[1]);
        lsDelParams.add(delParams);

        String resourceId = formatData[0];
        String kqdate = formatData[1];
        if(overtimeMap.containsKey(resourceId)){
          List<String> tmp_overtimeList = overtimeMap.get(resourceId);
          if(!tmp_overtimeList.contains(kqdate)){
            tmp_overtimeList.add(kqdate);
          }
        }else{
          if(!overtimeList.contains(kqdate)){
            overtimeList.add(kqdate);
          }
          overtimeMap.put(resourceId, overtimeList);
        }
      }

      //删除本次同步数据
      sql = " delete from hrmschedulesign where signfrom='importExcel' and userid =? and signdate = ? ";
      bRs.executeBatchSql(sql, lsDelParams);

      sql = " insert into HrmScheduleSign (userid, usertype, signdate, signtime, clientaddress, isincom, isimport, signfrom, longitude, latitude, addr) "
              + " values(?,?,?,?,?,?,?,?,?,?,?)";
      bRs.executeBatchSql(sql,lsParams);

      new KQFormatBiz().format(lsFormatParams);

      //处理加班生成
      List<KQTaskBean> tasks = new ArrayList<>();
      for(Map.Entry<String, List<String>> mme: overtimeMap.entrySet()){
        String resid = mme.getKey();
        List<String> overList = mme.getValue();
        for(String date : overList){
          SplitActionUtil.pushOverTimeTasks(date,date,resid,tasks);
        }
      }
      if(!tasks.isEmpty()){
        KQQueue.writeTasks(tasks);
      }
    } catch (Exception e) {
      writeLog(e);
    }

  }

  /**
   * 验证数据的合法性
   *
   */
  public boolean ScanRow(HSSFRow row) {
    boolean canImport = true;
    try {
      int userid = 0;
      String loginid = "", workcode = "", lastname = "";
      for (int j = 0; j < row.getLastCellNum(); j++) {
        if (j == 0) {
          //验证loginid
          loginid = this.getCellValue(row.getCell((short) j), row);
        } else if (j == 1) {
          //验证workcode
          workcode = this.getCellValue(row.getCell((short) j), row);
        } else if (j == 2) {
          //验证用户名
          lastname = this.getCellValue(row.getCell((short) j), row);
        } else if (j == 3) {
          //验证考勤时间
          String signdate = Util.null2String(this.getCellValue(row.getCell((short) j), row));
          String signtime = Util.null2String(this.getCellValue(row.getCell((short) (j+1)), row)).trim();
          if (signdate.length() == 0) {
            canImport = false;
          }
          if (signtime.length() == 0) {
            canImport = false;
          }
          if (signtime.length() > 0) {
            String[] a = signtime.split(":");
            if (a != null && a.length < 3) {
              signtime += ":00";
            }
          }

          String signdatetime = signdate + " " + signtime;
          if (signdate.length() > 0 && signtime.length() > 0 && checkData(signdatetime, "datetime")) canImport = false;
        }
      }
      if (this.keyField.equals("loginid")) {
        if (loginid.length() == 0) canImport = false;
        userid = getUserId(loginid);
      }else if (this.keyField.equals("workcode")) {
        if (workcode.length() == 0) canImport = false;
        userid = getUserId(workcode);
      } else if (this.keyField.equals("lastname")) {
        if (lastname.length() == 0) canImport = false;
        userid = getUserId(lastname);
      }
      if (userid == 0) {
        canImport = false;
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return canImport;
  }

  /**
   * 验证数据的合法性
   *
   */
  public List ScanFile() {
    try {
      ImageFileManager manager = new ImageFileManager();
      manager.getImageFileInfoById(Util.getIntValue(this.fileName));
      HSSFWorkbook workbook = new HSSFWorkbook(new POIFSFileSystem(manager.getInputStream()));
      HSSFSheet sheet = workbook.getSheetAt(0);

      HSSFRow row = null;

      int userid = 0;
      String loginid = "", workcode = "", lastname = "";
      int rowsNum = sheet.getLastRowNum();
      for (int i = 1; i < rowsNum + 1; i++) {
        row = sheet.getRow(i);
        if(row==null)continue;
        for (int j = 0; j < row.getLastCellNum(); j++) {
          if (j == 0) {
            //验证loginid
            loginid = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
          } else if (j == 1) {
            //验证workcode
            workcode = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
          } else if (j == 2) {
            //验证用户名
            lastname = Util.null2String(this.getCellValue(row.getCell((short) j), row)).trim();
          } else if (j == 3) {
            //验证考勤时间
            HSSFCell cell3 = row.getCell((short) j);
            String signdate = Util.null2String(this.getCellValue(cell3, row));
            HSSFCell cell4 = row.getCell((short) (j+1));
            String signtime = Util.null2String(this.getCellValue(cell4, row)).trim();

            if (signtime.length() > 0) {
              String[] a = signtime.split(":");
              if (a != null && a.length < 3) {
                signtime += ":00";
              }
            }

            if (this.keyField.equals("loginid")) {
              if (loginid.length() == 0) {
                errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(	503585, userlanguage));
              } else {
                userid = getUserId(loginid);
                if (userid == 0) {
                  errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(	503586, userlanguage));
                }
              }
            } else if (this.keyField.equals("workcode")) {
              if (workcode.length() == 0) {
                errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83770, userlanguage));
              } else {
                userid = getUserId(workcode);
                if (userid == 0) {
                  errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83772, userlanguage));
                }
              }
            } else if (this.keyField.equals("lastname")) {
              if (lastname.length() == 0) {
                errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83774, userlanguage));
              } else {
                userid = getUserId(lastname);
                if (userid == 0) {
                  errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83777, userlanguage));
                }
              }
            }

            if (signdate.length() == 0) {
              errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83762, userlanguage));
            }
            if (signtime.length() == 0) {
              errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83763, userlanguage));
            }
            String signdatetime = signdate + " " + signtime;
            if (signdate.length() > 0 && signtime.length() > 0 && checkData(signdatetime, "datetime"))
              errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + i + " " + SystemEnv.getHtmlLabelName(83768, userlanguage));
          }
        }
      }
    } catch (Exception e) {
      writeLog(e);
      errorInfo.add(SystemEnv.getHtmlLabelName(83779, userlanguage));
    }
    return errorInfo;
  }

  /**
   * 判断数据类型
   *
   * @param value
   * @param type
   * @return
   */
  public static boolean checkData(String value, String type) {
    boolean boo = false;

    if (type.equals("int")) {
      try {
        Integer.parseInt(value);
      } catch (Exception e) {
        boo = true;
      }
    } else if (type.equals("float")) {
      try {
        Float.parseFloat(value);
      } catch (Exception e) {
        boo = true;
      }
    } else if (type.equals("datetime")) {
      try {
        SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(SDF.parse(value));

      } catch (ParseException e) {
        boo = true;
      }
    }
    return boo;
  }

  public int getUserId(String val) {
    int userid = 0;
    RecordSet rs = new RecordSet();
    String sql = "";
    if(Util.isEnableMultiLang()) {
      if (rs.getDBType().equalsIgnoreCase("sqlserver")) {
        sql = "select id from hrmresource where ltrim(rtrim(dbo.convToMultiLang(ltrim(rtrim(" + keyField + "))," + userlanguage + ")))='" + Util.null2String(val).trim() + "'";
      } else {
        sql = "select id from hrmresource where ltrim(rtrim(convToMultiLang(ltrim(rtrim(" + keyField + "))," + userlanguage + ")))='" + Util.null2String(val).trim() + "'";
      }
    }else{
      sql = "select id from hrmresource where ltrim(rtrim( "+ keyField +"))='" + Util.null2String(val).trim() + "'";
    }
    rs.execute(sql);
    if (rs.next()) {
      userid = rs.getInt("id");
    }
    return userid;
  }

  public static int getUserId(String val, String keyField, int userlanguage) {
    int userid = 0;
    RecordSet rs = new RecordSet();
    String sql = "";
    if(Util.isEnableMultiLang()) {
      if (rs.getDBType().equalsIgnoreCase("sqlserver")) {
        sql = "select id from hrmresource where ltrim(rtrim(dbo.convToMultiLang(ltrim(rtrim(" + keyField + "))," + userlanguage + ")))='" + Util.null2String(val).trim() + "'";
      } else {
        sql = "select id from hrmresource where ltrim(rtrim(convToMultiLang(ltrim(rtrim(" + keyField + "))," + userlanguage + ")))='" + Util.null2String(val).trim() + "'";
      }
    }else{
      sql = "select id from hrmresource where ltrim(rtrim("+ keyField +"))='" + Util.null2String(val).trim() + "'";
    }
    rs.execute(sql);
    if (rs.next()) {
      userid = rs.getInt("id");
    }
    return userid;
  }

  public void importData(String beginDate, String endDate, boolean isSyn) throws Exception {
    RecordSet rs = new RecordSet();
    String sql = "";
    endDate = Tools.getDate(endDate,1);
    BatchRecordSet bRs = new BatchRecordSet();
    List<List<Object>> lsParams = new ArrayList<>();
    List<Object> params = null;

    List<List<Object>> lsDelParams = new ArrayList<>();
    List<Object> delParams = null;

    List<String> lsFormatData = new ArrayList<>();
    List<List<Object>> lsFormatParams = new ArrayList<>();
    List<Object> formatParams = null;


    List<ImportSetting> lsImportSetting = new ArrayList<>();
    ImportSetting importSetting = null;
    sql = " select datasourceid, loginid, workcode, lastname, signdate, signtime, tablename, clientaddress,longitude,latitude,addr,memo " +
            " from HrmScheduleSignSet ";
    rs.execute(sql);
    while (rs.next()) {
      importSetting = new ImportSetting();
      importSetting.setDatasourceid(Util.null2String(rs.getString("datasourceid")).trim());
      importSetting.setLoginid(Util.null2String(rs.getString("loginid")).trim());
      importSetting.setWorkcode(Util.null2String(rs.getString("workcode")).trim());
      importSetting.setLastname(Util.null2String(rs.getString("lastname")).trim());
      importSetting.setSigndate(Util.null2String(rs.getString("signdate")).trim());
      importSetting.setSigntime(Util.null2String(rs.getString("signtime")).trim());
      importSetting.setTablename(Util.null2String(rs.getString("tablename")).trim());
      importSetting.setClientaddress(Util.null2String(rs.getString("clientaddress")).trim());
      importSetting.setLongitude(Util.null2String(rs.getString("longitude")).trim());
      importSetting.setLatitude(Util.null2String(rs.getString("latitude")).trim());
      importSetting.setAddr(Util.null2String(rs.getString("addr")).trim());
      importSetting.setMemo(Util.null2String(rs.getString("memo")).trim());
      lsImportSetting.add(importSetting);
    }

    for(int i=0;lsImportSetting!=null&&i<lsImportSetting.size();i++) {
      DataSource ds = null;
      Connection conn = null;
      try {
        importSetting = lsImportSetting.get(i);
        String datasourceid = importSetting.getDatasourceid();
        String loginid = importSetting.getLoginid();
        String workcode = importSetting.getWorkcode();
        String lastname = importSetting.getLastname();
        String signdate =importSetting.getSigndate();
        String signtime = importSetting.getSigntime();
        String tablename = importSetting.getTablename();
        String clientaddress = importSetting.getClientaddress();
        String longitude=importSetting.getLongitude();
        String latitude=importSetting.getLatitude();
        String addr=importSetting.getAddr();
        String memo=importSetting.getMemo();

        ds = (DataSource) StaticObj.getServiceByFullname(("datasource." + datasourceid), DataSource.class);
        if(ds==null)continue;
        conn = ds.getConnection();
        conn.setAutoCommit(true);
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        DateFormat df = new SimpleDateFormat("HH:mm:ss");

        sql = "select * from " + tablename + " where " + signdate + " >= '" + beginDate + "' and " + signdate + " < '" + endDate + "' order by " + signdate + (signtime.length() == 0 || signtime.equalsIgnoreCase(signdate) ? "" : (", " + signtime));
        pstmt = conn.prepareStatement(sql);
        resultSet = pstmt.executeQuery();
        while (resultSet.next()) {
          String tmpLoginid = "";
          String tmpWorkcode = "";
          String tmpLastname = "";
          String tmpSigndate = "";
          String tmpSigntime = "";
          String tmpClientaddress = "";
          String tmpLongitude = "";
          String tmpLatitude = "";
          String tmpAddr = "";
          String tmpMemo = "";

          if (loginid.length() > 0) tmpLoginid = Util.null2String(resultSet.getString(loginid)).trim();
          if (workcode.length() > 0) tmpWorkcode = Util.null2String(resultSet.getString(workcode)).trim();
          if (lastname.length() > 0) tmpLastname = Util.null2String(resultSet.getString(lastname)).trim();
          if (signdate.length() > 0) tmpSigndate = Util.null2String(resultSet.getString(signdate)).trim();
          if (clientaddress.length() > 0) tmpClientaddress = Util.null2String(resultSet.getString(clientaddress));
          if (longitude.length() > 0) tmpLongitude = Util.null2String(resultSet.getString(longitude)).trim();
          if (latitude.length() > 0) tmpLatitude = Util.null2String(resultSet.getString(latitude)).trim();
          if (addr.length() > 0) tmpAddr = Util.null2String(resultSet.getString(addr)).trim();
          if (memo.length() > 0) tmpMemo = Util.null2String(resultSet.getString(memo)).trim();

          if (signtime.length() > 0) {
            tmpSigntime = Util.null2String(resultSet.getString(signtime)).trim();
            //判断tmpSigntime格式 08:21:11 如果为长格式需要格式化
            if (tmpSigntime.length() > 8) {
              tmpSigntime = df.format(Timestamp.valueOf(tmpSigntime));
            } else if (tmpSigntime.length() < 8) {
              //不带秒的情况  自动补齐
              tmpSigntime += ":00";
            }
          } else {
            tmpSigntime = "";
          }

          if (tmpSigndate.length() > 10) {
            //如果时间为长格式，从signdate字段中格式化时间
            if (tmpSigndate.length() == 16) {
              //不带秒的情况  自动补齐
              tmpSigntime += ":00";
            }
            if (tmpSigntime.length() == 0) {//以signtime字段为有限
              tmpSigntime = df.format(Timestamp.valueOf(tmpSigndate));
            }
            tmpSigndate = Tools.formatDate(tmpSigndate, "yyyy-MM-dd");
          }

          //如果时间格式不带秒补齐
          if (tmpSigntime.length() <= 5) {
            tmpSigntime += ":00";
          }
          int userid = 0;
          if (Util.null2String(tmpLoginid).length() > 0) {
            userid = getUserId(tmpLoginid, "loginid",this.userlanguage);
          }
          if (userid==0&&Util.null2String(tmpWorkcode).length() > 0) {
            userid = getUserId(tmpWorkcode, "workcode",this.userlanguage);
          }
          if (userid==0&&Util.null2String(tmpLastname).length() > 0) {
            userid = getUserId(tmpLastname, "lastname",this.userlanguage);
          }

          if(userid<=0)continue;
          params = new ArrayList<>();
          params.add(userid);
          params.add(1);
          params.add(tmpSigndate);
          params.add(tmpSigntime);
          params.add(tmpClientaddress);
          params.add(1);
          params.add(1);
          params.add("OutDataSourceSyn");
          params.add(tmpLongitude);
          params.add(tmpLatitude);
          params.add(tmpAddr);
          params.add(tmpMemo);
          lsParams.add(params);

          String formatData = userid+"|"+tmpSigndate;
          if(!lsFormatData.contains(formatData)){
            lsFormatData.add(formatData);
          }
        }
        resultSet.close();
        pstmt.close();
      } catch (Exception e) {
        writeLog(e);
      } finally {
        if (conn != null) conn.close();
      }
    }

    Map<String,List<String>> overtimeMap = Maps.newHashMap();
    List<String> overtimeList = Lists.newArrayList();
    //刷新报表数据
    for(int i=0;lsFormatData!=null&&i<lsFormatData.size();i++){
      formatParams = new ArrayList<>();
      String[] formatData = Util.splitString(lsFormatData.get(i),"|");
      String date_1 = DateUtil.addDate(formatData[1], -1);
      formatParams.add(formatData[0]);
      formatParams.add(date_1);
      lsFormatParams.add(formatParams);

      formatParams = new ArrayList<>();
      formatParams.add(formatData[0]);
      formatParams.add(formatData[1]);
      lsFormatParams.add(formatParams);

      delParams = new ArrayList<>();
      delParams.add(formatData[0]);
      delParams.add(formatData[1]);
      lsDelParams.add(delParams);

      String resourceId = formatData[0];
      String kqdate = formatData[1];
      if(overtimeMap.containsKey(resourceId)){
        List<String> tmp_overtimeList = overtimeMap.get(resourceId);
        if(!tmp_overtimeList.contains(kqdate)){
          tmp_overtimeList.add(kqdate);
        }
      }else{
        if(!overtimeList.contains(kqdate)){
          overtimeList.add(kqdate);
        }
        overtimeMap.put(resourceId, overtimeList);
      }
    }

    //删除本次同步数据
    sql = " delete from hrmschedulesign where signfrom='OutDataSourceSyn' and userid =? and signdate = ? ";
    bRs.executeBatchSql(sql, lsDelParams);

    sql = " insert into HrmScheduleSign (userid, usertype, signdate, signtime, clientaddress, isincom, isimport, signfrom, longitude, latitude, addr,memo) "
            + " values(?,?,?,?,?,?,?,?,?,?,?,?)";
    bRs.executeBatchSql(sql,lsParams);

    new KQFormatBiz().format(lsFormatParams);

    //处理加班生成
    List<KQTaskBean> tasks = new ArrayList<>();
    for(Map.Entry<String, List<String>> mme: overtimeMap.entrySet()){
      String resid = mme.getKey();
      List<String> overList = mme.getValue();
      for(String date : overList){
        SplitActionUtil.pushOverTimeTasks(date,date,resid,tasks);
      }
    }
    if(!tasks.isEmpty()){
      KQQueue.writeTasks(tasks);
    }
  }

  public int getUserlanguage() {
    return userlanguage;
  }

  public void setUserlanguage(int userlanguage) {
    this.userlanguage = userlanguage;
  }

}
