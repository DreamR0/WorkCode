package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.Util_DataCache;
import com.engine.kq.biz.chain.cominfo.HalfShiftComIndex;
import com.engine.kq.biz.chain.cominfo.RestShiftComIndex;
import com.engine.kq.biz.chain.cominfo.ShiftComIndex;
import com.engine.kq.biz.chain.cominfo.ShiftInfoCominfoBean;
import com.engine.kq.biz.chain.cominfo.WorkShiftComIndex;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.log.KQLog;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.math.NumberUtils;
import weaver.cache.CacheBase;
import weaver.cache.CacheColumn;
import weaver.cache.CacheColumnType;
import weaver.cache.CacheItem;
import weaver.cache.CacheMap;
import weaver.cache.PKColumn;
import weaver.cluster.CacheManager;
import weaver.cluster.CacheMessage;
import weaver.conn.RecordSet;
import weaver.email.MailReciveStatusUtils;
import weaver.email.domain.AccountStautsBean;
import weaver.general.BaseBean;
import weaver.general.StaticObj;
import weaver.general.Util;

/**
 * 班次管理缓存类 改成用redis的
 */
public class KQShiftManagementRedis {
  public static final String KQ_SHIFT_COMINFO_STATUS = "KQ_SHIFT_COMINFO_STATUS";

  private static final BaseBean baseBean = new BaseBean();

  /**
   * 是否是E9 radis缓存方案。false：未启用radis。true：使用radis。
   */
  public static boolean isNewSession = false;

  /**
   * 非集群，单节点服务器部署时，账号接收状态量获取方法。（线程安全的）
   */
  public static ConcurrentHashMap<String, Object> KQ_SHIFT_COMINFO_STATUS_MAP = new ConcurrentHashMap<String, Object>();

  // 初始化执行
  static {
    useNewSessionMode();
  }

  /**
   * 判断是否开启redis缓存。
   */
  public static void useNewSessionMode() {
    String status = Util.null2String(baseBean.getPropValue("weaver_new_session", "status")).trim();
    isNewSession = "1".equals(status);
  }

  private KQLog kqLog = new KQLog();

  public void getShiftInfoBean(String serialid, String isresttimeopen, String worktime,
                                String punchsettings,
                                ConcurrentHashMap<String, ShiftInfoCominfoBean> shiftInfoBeanMap,
      String halfcalrule,String halfcalpoint,String halfcalpoint2cross) throws Exception {

    Map<String,Object> workTimeMap = new HashMap<>();
    int workmins = 0;
    List<Object> workTimes = Collections.synchronizedList(new ArrayList<>());
    List<Object> restTimes = Collections.synchronizedList(new ArrayList<>());
    KQShiftOnOffWorkSectionComInfo kqShiftOnOffWorkSectionComInfo = new KQShiftOnOffWorkSectionComInfo();
    KQShiftRestTimeSectionComInfo kqShiftRestTimeSectionComInfo = new KQShiftRestTimeSectionComInfo();

    workTimes = kqShiftOnOffWorkSectionComInfo.getWorkSectionTimes(serialid);
    if(workTimes != null && !workTimes.isEmpty()){
      if("1".equalsIgnoreCase(isresttimeopen)) {
        //如果开启了才去判断
        restTimes = kqShiftRestTimeSectionComInfo.getRestSectionTimes(serialid);
      }
      if(NumberUtils.isNumber(worktime)){
        if(worktime.indexOf('.') == -1){
          workmins = Util.getIntValue(worktime,0);
        }else {
          worktime = worktime.substring(0,worktime.indexOf('.'));
          workmins = Util.getIntValue(worktime,0);
        }

      }else{
        workmins = 0;
        kqLog.info("班次有问题，serialid:"+serialid+"工作时长为:"+worktime);
      }
      workTimeMap.put("workTime", workTimes);
      workTimeMap.put("restTime", restTimes);
      workTimeMap.put("serialid", serialid);
      kqLog.writeLog("workTime:"+JSON.toJSONString(workTimes)+"::restTime:"+JSON.toJSONString(restTimes));
      //工作时长分钟数
      workTimeMap.put("workmins", workmins+"");
      workTimeMap.put("punchsettings", punchsettings);
      workTimeMap.put("isresttimeopen", isresttimeopen);
      workTimeMap.put("halfcalrule", halfcalrule);
      workTimeMap.put("halfcalpoint", halfcalpoint);
      workTimeMap.put("halfcalpoint2cross", halfcalpoint2cross);

      ShiftInfoCominfoBean shiftInfoCominfoBean = setShiftInfoBean(workTimeMap);
      shiftInfoBeanMap.put(serialid, shiftInfoCominfoBean);
    }

  }

  public ShiftInfoCominfoBean setShiftInfoBean(Map<String,Object> workTimeMap) throws Exception {

    ShiftComIndex workComIndex = new WorkShiftComIndex(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005306,weaver.general.ThreadVarLanguage.getLang())+"",workTimeMap);
    ShiftComIndex restComIndex = new RestShiftComIndex(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005307,weaver.general.ThreadVarLanguage.getLang())+"",workTimeMap);
    ShiftComIndex halfComIndex = new HalfShiftComIndex(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005308,weaver.general.ThreadVarLanguage.getLang())+"",workTimeMap);
    //创建执行链
    //前一天的都执行完了，去获取当天工作时段，再执行半天的规则
    workComIndex.setDuration(restComIndex);
    //执行完了半天的规则，再最后判断休息的时段
    restComIndex.setDuration(halfComIndex);

    ShiftInfoCominfoBean shiftInfoCominfoBean = new ShiftInfoCominfoBean();

    workComIndex.handleDuration(shiftInfoCominfoBean);
    return shiftInfoCominfoBean;
  }

  public ConcurrentHashMap<String,ShiftInfoCominfoBean> getShiftInfoBeanMap() {
    ConcurrentHashMap<String,ShiftInfoCominfoBean> shiftInfoBeanMap = new ConcurrentHashMap<>();
    kqLog = new KQLog();
    try {

      RecordSet rs = new RecordSet();
      String getShiftInfo  = "select * from kq_ShiftManagement where 1=1 order by id ";
      rs.executeQuery(getShiftInfo);
      while(rs.next()){
        String serialid = rs.getString("id");
        try {
          String isresttimeopen = rs.getString("isresttimeopen");
          String worktime = rs.getString("worktime");
          String punchsetting = "1";
          String halfcalrule = rs.getString("halfcalrule");
          halfcalrule = Util.null2String(halfcalrule).length() == 0 ? "0" : halfcalrule;
          String halfcalpoint = rs.getString("halfcalpoint");
          String halfcalpoint2cross = rs.getString("halfcalpoint2cross");
          getShiftInfoBean(serialid,isresttimeopen,worktime,punchsetting,shiftInfoBeanMap,halfcalrule,halfcalpoint,halfcalpoint2cross);
        } catch (Exception e) {
          kqLog.info("班次缓存报错:getShiftInfoBeanMap:serialid:"+serialid);
          StringWriter errorsWriter = new StringWriter();
          e.printStackTrace(new PrintWriter(errorsWriter));
          kqLog.info(errorsWriter.toString());
        }
      }

    } catch (Exception e) {
      kqLog.info("班次缓存报错:getShiftInfoBeanMap:");
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
    }
    return shiftInfoBeanMap;
  }


  public ShiftInfoCominfoBean getShiftInfoBeanMapBySql(String serialid) {
    ConcurrentHashMap<String,ShiftInfoCominfoBean> shiftInfoBeanMap = new ConcurrentHashMap<>();
    kqLog = new KQLog();
    try {
      RecordSet rs = new RecordSet();
      String getShiftInfo  = "select * from kq_ShiftManagement where 1=1 and id=? order by id ";
      rs.executeQuery(getShiftInfo,serialid);
      if(rs.next()){
        String isresttimeopen = rs.getString("isresttimeopen");
        String worktime = rs.getString("worktime");
        String punchsetting = "1";
        String halfcalrule = rs.getString("halfcalrule");
        halfcalrule = Util.null2String(halfcalrule).length() == 0 ? "0" : halfcalrule;
        String halfcalpoint = rs.getString("halfcalpoint");
        String halfcalpoint2cross = rs.getString("halfcalpoint2cross");
        getShiftInfoBean(serialid,isresttimeopen,worktime,punchsetting,shiftInfoBeanMap,halfcalrule,halfcalpoint,halfcalpoint2cross);
      }
      if(shiftInfoBeanMap.containsKey(serialid)){
        return shiftInfoBeanMap.get(serialid);
      }

    } catch (Exception e) {
      kqLog.info("班次缓存报错:getShiftInfoBeanMapBySql:");
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
    }
    return null;
  }

  /**
   * 三套缓存，redis staticobj 和当前本身自制的map
   * 把缓存数据重置下
   */
  public void resetShiftValWithRedis(){
    boolean isCluster = StaticObj.getInstance().isCluster();
    Map<String, ShiftInfoCominfoBean> shiftInfoBeanMap = getShiftInfoBeanMap();
    if(isCluster) {
      if(isNewSession) {
        try {
          Util_DataCache.setObjValWithRedis(KQ_SHIFT_COMINFO_STATUS, shiftInfoBeanMap);
        } catch (IOException e) {
          baseBean.writeLog(e);
          StaticObj staticObj = StaticObj.getInstance();
          staticObj.putObject(KQ_SHIFT_COMINFO_STATUS, shiftInfoBeanMap);
          sendNotificationToCache(CacheManager.ACTION_UPDATE, KQ_SHIFT_COMINFO_STATUS);
        }
      }else{
        StaticObj staticObj = StaticObj.getInstance();
        staticObj.putObject(KQ_SHIFT_COMINFO_STATUS, shiftInfoBeanMap);
        sendNotificationToCache(CacheManager.ACTION_UPDATE, KQ_SHIFT_COMINFO_STATUS);
      }
    } else {
      KQ_SHIFT_COMINFO_STATUS_MAP.put(KQ_SHIFT_COMINFO_STATUS, shiftInfoBeanMap);
    }
  }

  public ShiftInfoCominfoBean getShiftInfoBean(String serialid){
    ShiftInfoCominfoBean shiftInfoCominfoBean = null;
    Map<String, ShiftInfoCominfoBean> shiftInfoBeanMap = getShiftBeanMapValWithRedis();
    if(shiftInfoBeanMap == null){
      resetShiftValWithRedis();
      shiftInfoBeanMap = getShiftBeanMapValWithRedis();
    }
    if(shiftInfoBeanMap != null && shiftInfoBeanMap.containsKey(serialid)){
      shiftInfoCominfoBean = shiftInfoBeanMap.get(serialid);
    }
    return shiftInfoCominfoBean;
  }

  public Map<String, ShiftInfoCominfoBean> getShiftBeanMapValWithRedis() {
    Map<String, ShiftInfoCominfoBean> shiftInfoBeanMap = null;
    boolean isCluster = StaticObj.getInstance().isCluster();

    if(isCluster) { // 判断是否是集群环境。true 是集群，false非集群
      if(isNewSession){
        try {
          if(Util_DataCache.containsKeyWithRedis(KQ_SHIFT_COMINFO_STATUS)) {
            shiftInfoBeanMap = (Map<String, ShiftInfoCominfoBean>) Util_DataCache.getObjValWithRedis(KQ_SHIFT_COMINFO_STATUS);
          }
        }catch (Exception e){
          StaticObj staticObj = StaticObj.getInstance();
          shiftInfoBeanMap = (Map<String, ShiftInfoCominfoBean>) staticObj.getObject(KQ_SHIFT_COMINFO_STATUS);
        }
      }else{
        StaticObj staticObj = StaticObj.getInstance();
        shiftInfoBeanMap = (Map<String, ShiftInfoCominfoBean>) staticObj.getObject(KQ_SHIFT_COMINFO_STATUS);
      }
    } else {
      shiftInfoBeanMap = (Map<String, ShiftInfoCominfoBean>) KQ_SHIFT_COMINFO_STATUS_MAP.get(KQ_SHIFT_COMINFO_STATUS);
    }
    return shiftInfoBeanMap;
  }

  /**
   * 通知其他集群环境其他节点进行更新等操作.
   * @param optType
   * @param cacheKey
   */
  public static void sendNotificationToCache(String optType, String cacheKey) {
    StaticObj staticObj = StaticObj.getInstance();
    if(staticObj.isCluster()) {
      CacheMessage msg = new CacheMessage();
      msg.setAction(optType);
      msg.setCacheType(cacheKey);
      staticObj.sendNotification(msg);
    }
  }
}
