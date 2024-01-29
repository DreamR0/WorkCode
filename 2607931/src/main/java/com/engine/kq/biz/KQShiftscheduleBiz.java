package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2607931.util.CheckUtil;
import weaver.common.DateUtil;
import weaver.conn.BatchRecordSet;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.*;

public class KQShiftscheduleBiz extends BaseBean {

  /**
   * 调班流程 保存排班数据
   * @param userIds
   * @param fromDate
   * @param toDate
   * @param serialId
   * @param groupId
   */
  public void saveShiftschedule(String userIds, String fromDate, String toDate, String serialId, String groupId){
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    BatchRecordSet bRs = new BatchRecordSet();
    RecordSet rs = new RecordSet();
    List<List<Object>> paramInsert = new ArrayList<>();
    List<Object> params = null;
    String sql = "";
    try {
      KQShiftScheduleComInfo kqShiftScheduleComInfo = new KQShiftScheduleComInfo();
      List sqlParams = new ArrayList();
      Object[] objects= DBUtil.transListIn(userIds,sqlParams);
      sql = "update kq_shiftschedule set isdelete = 1 where resourceid in("+objects[0]+") and kqdate>=? and kqdate<=? ";
      rs.executeUpdate(sql,sqlParams,fromDate,toDate);
      //qc20230816 这里把当前已有排班日期存储起来
      List<String> kq_shiftList =new ArrayList<String>();
      String selectsql = "select * from kq_shiftschedule where isdelete = 1 and resourceid in("+objects[0]+") and kqdate>=? and kqdate<=? ";
      rs.executeQuery(selectsql,sqlParams,fromDate,toDate);
      while (rs.next()){
        kq_shiftList.add(Util.null2String(rs.getString("kqdate")));
      }
      List<String> lsUserId = Util.splitString2List(userIds,",");
      CheckUtil checkUtil = new CheckUtil();
      for(String userId : lsUserId) {
        boolean isEnd = false;
        Calendar cal = DateUtil.getCalendar();
        for (String date = fromDate; !isEnd; ) {
          if (date.equals(toDate)) isEnd = true;
          params = new ArrayList<Object>();
          params.add(date);
          params.add(serialId);
          params.add(userId);
          params.add(groupId);
          paramInsert.add(params);
          cal.setTime(DateUtil.parseToDate(date));
          date = DateUtil.getDate(cal.getTime(), 1);
        }
      }

      for (int i = 0; paramInsert != null && i < paramInsert.size(); i++) {
        params = paramInsert.get(i);
        String kqdate = Util.null2String(params.get(0));
        String serialid = Util.null2String(params.get(1));
        String resourceid = Util.null2String(params.get(2));
        //qczj 这里如果是周六周日就跳过排班
        Date kqDate = format.parse(kqdate);
        Boolean isHave = false;
        new BaseBean().writeLog("==zj==(kqDate)" + JSON.toJSONString(kqDate));
        new BaseBean().writeLog("==zj==(isHave)" + JSON.toJSONString(isHave));
        isHave = checkUtil.dateToWeek(kqDate);
        if (isHave){
          continue;
        }
        String tmp_groupId = Util.null2String(params.get(3));
        sql = "insert into kq_shiftschedule (kqdate,serialid,resourceid,groupid,isdelete) values(?,?,?,?,0)";
        rs.executeUpdate(sql, kqdate,serialid,resourceid,tmp_groupId);
      }

      kqShiftScheduleComInfo.removeCache();
    }catch (Exception e){
      writeLog(e);
    }
  }
}