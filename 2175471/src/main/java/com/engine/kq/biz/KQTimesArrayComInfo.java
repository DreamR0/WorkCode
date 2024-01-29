package com.engine.kq.biz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.cache.CacheBase;
import weaver.cache.CacheColumn;
import weaver.cache.CacheColumnType;
import weaver.cache.CacheItem;
import weaver.cache.PKColumn;
import weaver.conn.RecordSet;
import weaver.general.Util;

public class KQTimesArrayComInfo extends CacheBase{
	protected static String TABLE_NAME = "kq_timesarray";
  /** sql中的where信息，不要以where开始 */
  /**
   * 这个缓存类里只需要存储一条数据供虚拟列使用就可以了
   */
  protected static String TABLE_WHERE = "arrayindex=0";

	@PKColumn(type = CacheColumnType.NUMBER)
	protected static String PK_NAME = "arrayindex";

	@CacheColumn(name = "times")
	protected static int times;

  /**
   * 主要是用这个虚拟列存储map 可以 通过时间获取下标
   */
  @CacheColumn(isVirtual = true)
  protected static int timesArrayindexMap;


  /**
   * 主要是用这个虚拟列存储map 可以 通过下标获取时间
   */
  @CacheColumn(isVirtual = true)
  protected static int arrayindexTimesMap;

  /**
   * 原始数组
   */
  @CacheColumn(isVirtual = true)
  protected static int initArray;


	public String getId(){
		return (String)getRowValue(PK_INDEX);
	}

  public String getTimes() { return (String)getRowValue(times); }

  public String getTimes(String key) {
    return (String)getValue(times,key);
  }

  public String getTimesArrayindexMap() { return (String)getRowValue(timesArrayindexMap); }

  public String getTimesArrayindexMap(String key) {
    return (String)getValue(timesArrayindexMap,key);
  }

  public String getArrayindexTimesMap() { return (String)getRowValue(arrayindexTimesMap); }

  public String getArrayindexTimesMap(String key) {
    return (String)getValue(arrayindexTimesMap,key);
  }

  @Override
  protected void modifyCacheItem(String key, CacheItem cacheItem) {
    String sql = "select times,arrayindex from kq_timesarray";
    Map<String, String> timeMap = new HashMap<String, String>();
    Map<String, String> arrayMap = new HashMap<String, String>();
    // 列值转换
    RecordSet rstemp=new RecordSet();
    rstemp.executeQuery(sql);
    int[] initArrays = new int[rstemp.getCounts()];
    int i = 0;
    while(rstemp.next()){
      String times = rstemp.getString("times");
      String arrayindex = rstemp.getString("arrayindex");
      timeMap.put(times, arrayindex);
      arrayMap.put(arrayindex, times);
      initArrays[i] = -1;
      i++;
    }

    // 虚拟列赋值
    cacheItem.set(timesArrayindexMap, timeMap);
    // 虚拟列赋值
    cacheItem.set(arrayindexTimesMap, arrayMap);
    // 虚拟列赋值
    cacheItem.set(initArray, initArrays);
  }

  /**
   * 根据传过来的时间获取数组下标
   *
   * @param times
   * @return 无设置时返回Null
   */
  public int getArrayindexByTimes(String times) {
    if(times.length()>5){
      times = times.substring(0,5);
    }
    //sqlwhere里决定了pk只能是0
    Map<String, String> timeMap = (Map)getObjValue(timesArrayindexMap,"0");
    if (timeMap == null){
      return -1;
    }

    return Util.getIntValue(timeMap.get(times));
  }

  /**
   * 根据传过来的数组下标获取时间
   *
   * @param arrayIndex
   * @return 无设置时返回Null
   */
  public String getTimesByArrayindex(int arrayIndex) {
    //sqlwhere里决定了pk只能是0
    Map<String, String> arrayMap = (Map)getObjValue(arrayindexTimesMap,"0");
    if (arrayMap == null){
      return "";
    }

    return arrayMap.get(""+arrayIndex);
  }

  /**
   * 获取总的下标长度
   * @return
   */
  public int getIndexSize(){
    Map<String, String> timeMap = (Map)getObjValue(timesArrayindexMap,"0");
    return timeMap.size();
  }

  /**
   * 获取原始的数组
   * @return
   */
  public int[] getInitArr(){
    int[] initArraysCom = (int[])getObjValue(initArray,"0");
    int length = 2881;
    if(initArraysCom != null){
      length = initArraysCom.length;
    }
    int[] initArrays = new int[length];

    Arrays.fill(initArrays, -1);
    return initArrays;
  }

  /**
   * 获取分钟数 数组
   * @param initArrays 需要遍历的数组
   * @param startIndex 遍历数组开始下标
   * @param endIndex 遍历数组结束下标
   * @param checkVal 需要判断的值
   */
  public int[] getCnts(int[] initArrays,int startIndex,int endIndex,int checkVal){
    int cnt = 0;
    int[] cntArray = new int[3];
    for(int i = startIndex ; i < endIndex ; i++){
      if(initArrays[i] == checkVal){
        if(cnt == 0){
          cntArray[0] = i;
        }else{
          cntArray[1] = i;
        }
        cnt++;
      }
    }
    cntArray[2] = cnt;
    return cntArray;
  }

  /**
   * 获取分钟数
   * @param initArrays 需要遍历的数组
   * @param startIndex 遍历数组开始下标
   * @param endIndex 遍历数组结束下标
   * @param checkVal 需要判断的值
   */
  public int getCnt(int[] initArrays,int startIndex,int endIndex,int checkVal){
    int cnt = 0;
    for(int i = startIndex ; i < endIndex ; i++){
      if(initArrays[i] == checkVal){
        cnt++;
      }
    }
    return cnt;
  }

  /**
   * 把24小时制的时间下标转换成48小时制的
   * @param timeIndex
   * @return
   */
  public int turn24to48TimeIndex(int timeIndex){
    int longtimeIndex = timeIndex;
    try{
      if(timeIndex > 1440){
        return longtimeIndex;
      }
      longtimeIndex = timeIndex+1440;
    }catch (Exception e){
      e.printStackTrace();
    }
    return longtimeIndex;
  }

  /**
   * 把24小时制的时间转换成48小时制的
   * @param time
   * @return
   */
  public String turn24to48Time(String time){
    String longtime = time;
    try{
      int timeIndex = getArrayindexByTimes(time);
      if(timeIndex > 1440){
        return time;
      }
      int timeIndexTmp = turn24to48TimeIndex(timeIndex);
      longtime = getTimesByArrayindex(timeIndexTmp);
    }catch (Exception e){
      e.printStackTrace();
    }
    return longtime;
  }

  /**
   * 把48小时制度时间下标转换成24小时制的
   * @param timeIndex
   * @return
   */
  public int turn48to24TimeIndex(int timeIndex){
    int longtimeIndex = timeIndex;
    try{
      if(timeIndex > 1439){
        longtimeIndex = timeIndex-1440;
        longtimeIndex = longtimeIndex > 0 ? longtimeIndex : 0;
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    return longtimeIndex;
  }

  /**
   * 把48小时制度时间转换成24小时制的
   * @param time
   * @return
   */
  public String turn48to24Time(String time){
    String longtime = time;
    try{
      int timeIndex = getArrayindexByTimes(time);
      int timeIndexTmp = turn48to24TimeIndex(timeIndex);
      longtime = getTimesByArrayindex(timeIndexTmp);
    }catch (Exception e){
      e.printStackTrace();
    }
    return longtime;
  }

  /**
   * 获取一天的工作总时长1439分钟
   * @return
   */
  public static int getOneDayArraySize(){
    return 1439;
  }
}