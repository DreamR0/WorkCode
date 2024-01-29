package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.engine.kq.log.KQLog;
import weaver.cache.*;
import weaver.conn.RecordSet;
import weaver.general.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 班次休息时段设置缓存类
 */
public class KQShiftRestTimeSectionComInfo extends CacheBase implements Serializable {
  /*
      * 需要关注 : 如果完全在 initCache 自己定义数据初始化， 这个字段可以不定义
      */
  protected static String TABLE_NAME = "";

  /*
   * 需要关注 : 如果完全在 initCache 自己定义数据初始化，或者不需要指定固定的条件， 这个字段可以不定义
   * sql中的where信息，不要以where开始
   */
  protected static String TABLE_WHERE = null;

  /*
   * 需要关注 : 如果完全在 initCache 自己定义数据初始化，或者不需要指定顺序， 这个字段可以不定义 sql中的order
   * by信息，不要以order by开始
   *
   */
  protected static String TABLE_ORDER = null;

  @PKColumn(type = CacheColumnType.NUMBER)
  protected static String PK_NAME = "id";

  @CacheColumn
  protected static int serial;

  private KQLog kqLog = new KQLog();

  @Override
  protected boolean autoInitIfNotFound() {
    return false;
  }

  @Override
  public CacheMap initCache() {
    CacheMap localData = createCacheMap();
    RecordSet rs = new RecordSet();
    kqLog = new KQLog();

    List<Object> restSectionList = new ArrayList<>();
    List<Object> sectionList = new ArrayList<>();
    Map<String,Object> sectionMap = new HashMap<>();

    Map<String,Object> serialMaps = new HashMap<>();//分组用的map
    try {
      String getRestSections = "select * from kq_ShiftRestTimeSections where 1=1 ";
      rs.executeQuery(getRestSections);
      while(rs.next()){
        String id = Util.null2String(rs.getString("id"));
        sectionMap = new HashMap<>();
        String serialid = Util.null2String(rs.getString("serialid"));
        String resttype = Util.null2String(rs.getString("resttype"));
        String across = Util.null2String(rs.getString("across"));
        String times = Util.null2String(rs.getString("time"));
        sectionMap.put("across", across);
        sectionMap.put("times", times);

        if(serialMaps.get(serialid) != null){
          List<Object> tmpList = (List<Object>)serialMaps.get(serialid);
          if("start".equalsIgnoreCase(resttype)){
            tmpList.set(0, sectionMap);
          }else if("end".equalsIgnoreCase(resttype)){
            tmpList.set(1, sectionMap);
          }
        }else{
          restSectionList = new ArrayList<>();
          sectionList = new ArrayList<>();
          sectionList.add("");
          sectionList.add("");
          if("start".equalsIgnoreCase(resttype)){
            sectionList.set(0, sectionMap);
          }else if("end".equalsIgnoreCase(resttype)){
            sectionList.set(1, sectionMap);
          }
          restSectionList.addAll(sectionList);
          serialMaps.put(serialid, restSectionList);

        }
      }

      for(Map.Entry<String,Object> me : serialMaps.entrySet()){
        String id = me.getKey();
        List<Object> valList = (List<Object>)me.getValue();
        CacheItem cacheItem = createCacheItem();
        cacheItem.set(PK_INDEX, id);
        cacheItem.set(serial, valList);
        modifyCacheItem(id, cacheItem);
        localData.put(id, cacheItem);
      }
    }catch (Exception e){
      writeLog(e);
    }
    return localData;
  }

  /**
   * 根据班次id获取休息时段集合
   * @param serialid
   * @return
   */
  public List<String[]> getRestSectionTimesList(String serialid){

    String startSql = "select * from kq_ShiftRestTimeSections where (isdelete is null or  isdelete <> '1')  and resttype='start'  and serialid = ? ";
    String endSql = "select * from kq_ShiftRestTimeSections where (isdelete is null or  isdelete <> '1') and resttype='end' and serialid = ? ";
    List<String[]> restList = new ArrayList<>();
    RecordSet rstemp=new RecordSet();
    rstemp.executeQuery(startSql, serialid);
    while(rstemp.next()){
      String times = rstemp.getString("time");
      String[] timeArr = new String[2];
      timeArr[0] = times;
      restList.add(timeArr);
    }

    int i = 0;
    rstemp=new RecordSet();
    rstemp.executeQuery(endSql, serialid);
    while(rstemp.next()){
      String times = rstemp.getString("time");
      String[] timeArr = restList.get(i);
      timeArr[1] = times;
      i++;
    }

    return restList;
  }
  /**
   * 根据班次获取休息时段
   * @param serialid
   * @return
   */
  public List<Object> getRestSectionTimes(String serialid){
    //return (List<Object>) getObjValue(serial, serialid);
    RecordSet rs = new RecordSet();
    kqLog = new KQLog();
    List<Object> valList = new ArrayList<>();
    List<Object> restSectionList = new ArrayList<>();
    List<Object> sectionList = new ArrayList<>();
    Map<String,Object> sectionMap = new HashMap<>();

    Map<String,Object> serialMaps = new HashMap<>();//分组用的map
    Map<String,Object> groupSectionMaps = new HashMap<>();//分组用的map
    try {
      String getRestSections = "select * from kq_ShiftRestTimeSections where serialid=? order by orderId ";
      rs.executeQuery(getRestSections,serialid);
      while(rs.next()) {
        String id = Util.null2String(rs.getString("id"));
        sectionMap = new HashMap<>();
        String resttype = Util.null2String(rs.getString("resttype"));
        String across = Util.null2String(rs.getString("across"));
        String times = Util.null2String(rs.getString("time"));
        String record = Util.null2String(rs.getString("record1"));
        sectionMap.put("across", across);
        sectionMap.put("times", times);

        if (serialMaps.get(serialid) != null) {
          Map<String, Object> tmpgroupSectionMaps = (Map<String, Object>) serialMaps.get(serialid);
          if (tmpgroupSectionMaps.get(record) != null) {
            List<Object> tmpWorkSection = (List<Object>) tmpgroupSectionMaps.get(record);
            List<Object> tmpSection = ((List<Object>) tmpWorkSection.get(tmpWorkSection.size() - 1));
            if ("start".equalsIgnoreCase(resttype)) {
              tmpSection.set(0, sectionMap);
            } else if ("end".equalsIgnoreCase(resttype)) {
              tmpSection.set(1, sectionMap);
            }
          } else {
            restSectionList = new ArrayList<>();
            sectionList = new ArrayList<>();
            sectionList.add("");
            sectionList.add("");
            if ("start".equalsIgnoreCase(resttype)) {
              sectionList.set(0, sectionMap);
            } else if ("end".equalsIgnoreCase(resttype)) {
              sectionList.set(1, sectionMap);
            }
            restSectionList.add(sectionList);
            tmpgroupSectionMaps.put(record, restSectionList);
          }
        } else {
          serialMaps.put(serialid, groupSectionMaps);
          if (groupSectionMaps.get(record) != null) {
            List<Object> tmpWorkSection = (List<Object>) groupSectionMaps.get(record);
            List<Object> tmpSection = ((List<Object>) tmpWorkSection.get(tmpWorkSection.size() - 1));
            if ("start".equalsIgnoreCase(resttype)) {
              tmpSection.set(0, sectionMap);
            } else if ("end".equalsIgnoreCase(resttype)) {
              tmpSection.set(1, sectionMap);
            }
          } else {
            sectionList = new ArrayList<>();
            sectionList.add("");
            sectionList.add("");
            if ("start".equalsIgnoreCase(resttype)) {
              sectionList.set(0, sectionMap);
            } else if ("end".equalsIgnoreCase(resttype)) {
              sectionList.set(1, sectionMap);
            }
            restSectionList.add(sectionList);
            groupSectionMaps.put(record, restSectionList);

          }
        }
      }
      for(Map.Entry<String,Object> me : serialMaps.entrySet()){
        valList = new ArrayList<>();
        Map<String,Object> val = (Map<String,Object>)me.getValue();
        for(Map.Entry<String,Object> mee : val.entrySet()){
          valList.addAll((List<Object>)mee.getValue());
        }
      }
    }catch (Exception e){
      writeLog(e);
    }
    kqLog.writeLog("vallist:"+JSON.toJSONString(valList));
    return valList;
  }

  /**
   * 删除缓存信息
   */
  public void removeShiftRestTimeSectionCache() {
    removeCache();
  }
}
