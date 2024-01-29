package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.io.Writer;
import java.util.Map;
import java.util.UUID;
import weaver.conn.ConnStatement;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.common.DbFunctionUtil;

/**
 * 加班记录考勤事件日志
 * 现在先做一个加班的，把加班生成的时候流转，对应流程，班次，考勤组信息，加班规则信息
 */
public class KQOvertimeLogBiz {

  /**
   * 明细表里记录一下班次和考勤组信息
   * @param resourceid
   * @param workTimeEntityLogMap
   * @param main_uuid
   * @param worktimeType
   * @return
   */
  public String logDetailWorkTimeEntity(String resourceid, Map<String, Object> workTimeEntityLogMap,
      String main_uuid,String worktimeType) {
    RecordSet rs = new RecordSet();

    String serial_info = "";
    if(workTimeEntityLogMap != null){
      serial_info = JSON.toJSONString(workTimeEntityLogMap,SerializerFeature.DisableCheckSpecialChar,SerializerFeature.DisableCircularReferenceDetect);
    }
    String uuid = UUID.randomUUID().toString();
    boolean isok = false;
    if(rs.getDBType().equalsIgnoreCase("oracle")&& Util.null2String(rs.getOrgindbtype()).equals("oracle")){
      String sql = "insert into kq_overtime_log_detail(resourceid,createdatetime,serial_info,overtime_info,event_info,uuid,overtimetype,main_uuid) "
          + " values(?,"+ DbFunctionUtil.getCurrentFullTimeFunction(rs.getDBType())+",empty_clob(),empty_clob(),empty_clob(),?,?,?) ";
      isok = rs.executeUpdate(sql,resourceid,uuid,worktimeType,main_uuid);
    }else{
      String sql = "insert into kq_overtime_log_detail(resourceid,createdatetime,serial_info,uuid,overtimetype,main_uuid) "
          + " values("+resourceid+","+DbFunctionUtil.getCurrentFullTimeFunction(rs.getDBType())+",?,'"+uuid+"','"+worktimeType+"','"+main_uuid+"') ";
      rs.executeUpdate(sql,serial_info);
    }

    if(rs.getDBType().equalsIgnoreCase("oracle")&& Util.null2String(rs.getOrgindbtype()).equals("oracle")){
      ConnStatement stat=null;
      try {
        stat = new ConnStatement();
        // 需要使用for update方法来进行更新，
        // 但是，特别需要注意，如果原来CLOB字段有值，需要使用empty_clob()将其清空。
        // 如果原来是null，也不能更新，必须是empty_clob()返回的结果。
        stat.setStatementSql("select serial_info from kq_overtime_log_detail where uuid=? for update", false);
        stat.setString(1,uuid);
        stat.executeQuery();
        if (stat.next()) {
          oracle.sql.CLOB clob = (oracle.sql.CLOB) stat.getClob("serial_info");
          Writer outStream = clob.getCharacterOutputStream();
          char[] c = serial_info.toCharArray();
          outStream.write(c, 0, c.length);
          outStream.flush();
          outStream.close();
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (stat != null) {
          stat.close();
        }
      }
    }

    return uuid;
  }

  /**
   * 在日志表里更新一下生成的加班id
   * 这样可以根据加班id找到对应的日志
   * @param uuid
   * @param overtimeid
   * @return
   */
  public void updateOvertimeId(String uuid,String overtimeid) {
    RecordSet rs = new RecordSet();
    String sql = "update kq_overtime_log_detail set overtimeid="+overtimeid+" where uuid='"+uuid+"' ";
    boolean isOk = rs.executeUpdate(sql);
  }
  /**
   * 记录一下加班生成明细
   * @param resourceid
   * @param overtimeLogMap
   * @param uuid
   */
  public void logDetailOvertimeMap(String resourceid, Map<String, Object> overtimeLogMap,String uuid) {
    RecordSet rs = new RecordSet();

    String overtime_info = "";
    if(overtimeLogMap != null){
      overtime_info = JSON.toJSONString(overtimeLogMap, SerializerFeature.DisableCheckSpecialChar,SerializerFeature.DisableCircularReferenceDetect);
    }
    if(rs.getDBType().equalsIgnoreCase("oracle")&& Util.null2String(rs.getOrgindbtype()).equals("oracle")){
      ConnStatement stat=null;
      try {
        stat = new ConnStatement();
        // 需要使用for update方法来进行更新，
        // 但是，特别需要注意，如果原来CLOB字段有值，需要使用empty_clob()将其清空。
        // 如果原来是null，也不能更新，必须是empty_clob()返回的结果。
        stat.setStatementSql("select overtime_info from kq_overtime_log_detail where uuid=? for update", false);
        stat.setString(1,uuid);
        stat.executeQuery();
        if (stat.next()) {
          oracle.sql.CLOB clob = (oracle.sql.CLOB) stat.getClob("overtime_info");
          Writer outStream = clob.getCharacterOutputStream();
          char[] c = overtime_info.toCharArray();
          outStream.write(c, 0, c.length);
          outStream.flush();
          outStream.close();
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (stat != null) {
          stat.close();
        }
      }
    }else{
      String sql = "update kq_overtime_log_detail set overtime_info='"+overtime_info+"' where uuid='"+uuid+"'";
      rs.executeUpdate(sql);
    }

  }

  /**
   * 记录一下流程开始日期结束日期的
   * @param resourceid
   * @param eventLogMap
   * @param overtimetype
   * @return
   */
  public String logEvent(String resourceid, Map<String, Object> eventLogMap, String overtimetype) {
    RecordSet rs = new RecordSet();

    String event_info = "";
    if(eventLogMap != null){
      event_info = JSON.toJSONString(eventLogMap,SerializerFeature.DisableCheckSpecialChar,SerializerFeature.DisableCircularReferenceDetect);
    }
    String uuid = UUID.randomUUID().toString();
    boolean isok = false;
    if(rs.getDBType().equalsIgnoreCase("oracle")&& Util.null2String(rs.getOrgindbtype()).equals("oracle")){
      String sql = "insert into kq_overtime_log(resourceid,createdatetime,event_info,uuid,overtimetype) "
          + " values("+resourceid+","+ DbFunctionUtil.getCurrentFullTimeFunction(rs.getDBType())+",empty_clob(),'"+uuid+"','"+overtimetype+"') ";
      isok = rs.executeUpdate(sql);
    }else{
      String sql = "insert into kq_overtime_log(resourceid,createdatetime,event_info,uuid,overtimetype) "
          + " values("+resourceid+","+DbFunctionUtil.getCurrentFullTimeFunction(rs.getDBType())+",?,'"+uuid+"','"+overtimetype+"') ";
      rs.executeUpdate(sql,event_info);
    }
    if(isok && rs.getDBType().equalsIgnoreCase("oracle")&& Util.null2String(rs.getOrgindbtype()).equals("oracle")){
      ConnStatement stat=null;
      try {
        stat = new ConnStatement();
        // 需要使用for update方法来进行更新，
        // 但是，特别需要注意，如果原来CLOB字段有值，需要使用empty_clob()将其清空。
        // 如果原来是null，也不能更新，必须是empty_clob()返回的结果。
        stat.setStatementSql("select event_info from kq_overtime_log where uuid=? for update", false);
        stat.setString(1,uuid);
        stat.executeQuery();
        if (stat.next()) {
          oracle.sql.CLOB clob = (oracle.sql.CLOB) stat.getClob("event_info");
          Writer outStream = clob.getCharacterOutputStream();
          char[] c = event_info.toCharArray();
          outStream.write(c, 0, c.length);
          outStream.flush();
          outStream.close();
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (stat != null) {
          stat.close();
        }
      }
    }
    return uuid;
  }

  /**
   * 记录对应的加班生成过程中使用的表sql和表数据
   * @param resourceid
   * @param eventLogMap
   * @param uuid
   * @param overtimetype
   */
  public void logDetailEvent(String resourceid, Map<String, Object> eventLogMap, String uuid, String overtimetype) {
    RecordSet rs = new RecordSet();

    String event_info = "";
    if(eventLogMap != null){
      event_info = JSON.toJSONString(eventLogMap,SerializerFeature.DisableCheckSpecialChar,SerializerFeature.DisableCircularReferenceDetect);
    }

    if(rs.getDBType().equalsIgnoreCase("oracle")&&rs.getOrgindbtype().equalsIgnoreCase("oracle")){
      ConnStatement stat=null;
      try {
        stat = new ConnStatement();
        // 需要使用for update方法来进行更新，
        // 但是，特别需要注意，如果原来CLOB字段有值，需要使用empty_clob()将其清空。
        // 如果原来是null，也不能更新，必须是empty_clob()返回的结果。
        stat.setStatementSql("select event_info from kq_overtime_log_detail where uuid=? for update", false);
        stat.setString(1,uuid);
        stat.executeQuery();
        if (stat.next()) {
          oracle.sql.CLOB clob = (oracle.sql.CLOB) stat.getClob("event_info");
          Writer outStream = clob.getCharacterOutputStream();
          char[] c = event_info.toCharArray();
          outStream.write(c, 0, c.length);
          outStream.flush();
          outStream.close();
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (stat != null) {
          stat.close();
        }
      }
    }else{
      String regexp = "\'";
      event_info =event_info.replaceAll(regexp, "\"");
      String sql = "update kq_overtime_log_detail set event_info='"+event_info+"' where uuid='"+uuid+"'";
      rs.executeUpdate(sql);
    }

  }
}
