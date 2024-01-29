package com.engine.kq.cmd.shiftmanagement;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQConfigComInfo;
import com.engine.kq.biz.KQGroupBiz;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQShiftManagementComInfo;
import com.engine.kq.biz.KQShiftOnOffWorkSectionComInfo;
import com.engine.kq.biz.KQShiftRestTimeSectionComInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.conn.RecordSetTrans;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.Reminder.KQAutoCardTask;
import weaver.hrm.User;
import weaver.hrm.common.database.dialect.DbDialectFactory;
import weaver.hrm.common.database.dialect.IDbDialectSql;
import weaver.systeminfo.SystemEnv;

/**
 * 保存班次管理基本信息表单
 * @author pzy
 *
 */
public class SaveShiftManagementBaseFormCmd extends AbstractCommonCommand<Map<String, Object>>{

  private SimpleBizLogger logger;

  public SaveShiftManagementBaseFormCmd() {
  }

  public SaveShiftManagementBaseFormCmd(Map<String, Object> params, User user) {
    this.user = user;
    this.params = params;
    this.logger = new SimpleBizLogger();
    BizLogContext logContext = new BizLogContext();
    logContext.setDateObject(new Date());
    logContext.setLogType(BizLogType.HRM_ENGINE);
    logContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_SHIFTMANAGER);
    logContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_SHIFTMANAGER);
    logContext.setParams(params);
    logger.setUser(user);//当前操作人
    if(params != null && params.containsKey("data")){
      String datas = Util.null2String(params.get("data"));
      JSONObject jsonObj = JSON.parseObject(datas);
      String serialid = Util.null2String(jsonObj.get("id"));

      if(serialid.length() > 0){
        String mainSql = " select * from kq_ShiftManagement where id= "+serialid +" ";
        logger.setMainSql(mainSql);//主表sql
        logger.setMainPrimarykey("id");//主日志表唯一key
        logger.setMainTargetNameColumn("serial");

        SimpleBizLogger.SubLogInfo subLogInfo1 = logger.getNewSubLogInfo();
        String subSql1 = "select * from kq_ShiftOnOffWorkSections where serialid="+serialid;
        subLogInfo1.setSubTargetNameColumn("times");
        subLogInfo1.setGroupId("0");  //所属分组， 按照groupid排序显示在详情中， 不设置默认按照add的顺序。
        subLogInfo1.setSubGroupNameLabel(27961); //在详情中显示的分组名称，不设置默认显示明细x
        subLogInfo1.setSubSql(subSql1);
        logger.addSubLogInfo(subLogInfo1);

        SimpleBizLogger.SubLogInfo subLogInfo = logger.getNewSubLogInfo();
        String subSql = " select * from kq_ShiftRestTimeSections where serialid = "+serialid;
        subLogInfo.setSubSql(subSql);
        subLogInfo.setSubTargetNameColumn("time");
        subLogInfo.setGroupId("1");  //所属分组， 按照groupid排序显示在详情中， 不设置默认按照add的顺序。
        subLogInfo.setSubGroupNameLabel(505603); //在详情中显示的分组名称，不设置默认显示明细x
        logger.addSubLogInfo(subLogInfo);
        logger.before(logContext);
      }
    }

  }

  @Override
  public BizLogContext getLogContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<BizLogContext> getLogContexts() {
    return logger.getBizLogContexts();
  }

  /**
   * 获取日志对象的名称
   * @param id
   * @param para2
   * @return
   */
  public String getTargetName(String id,String para2){
    try {
      return para2;
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String,Object> retmap = new HashMap<String,Object>();
    String datas = Util.null2String(params.get("data"));
    JSONObject jsonObj = JSON.parseObject(datas);
    String serialid = Util.null2String(jsonObj.get("id"));
    if(!HrmUserVarify.checkUserRight("KQClass:Management",user)) {
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
      return retmap;
    }

    try{
      if(serialid.length() > 0){
        edit(retmap,jsonObj);
      }else{
        add(retmap,jsonObj);
      }
      KQShiftManagementComInfo kqShiftManagementComInfo = new KQShiftManagementComInfo();
      kqShiftManagementComInfo.removeShiftManagementCache();

      if(retmap.containsKey("id")){
        KQConfigComInfo kqConfigComInfo = new KQConfigComInfo();
        String auto_card_cominfo = Util.null2String(kqConfigComInfo.getValue("auto_card_cominfo"),"0");
        if("1".equalsIgnoreCase(auto_card_cominfo)){
          String serial_id = Util.null2String(retmap.get("id"));
          KQGroupBiz kqGroupBiz = new KQGroupBiz();
          List<String> groupList = kqGroupBiz.getGroupIdByUesedSerialId(serial_id);
          KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
          if(!groupList.isEmpty()){
            for(String groupId : groupList){
              String auto_checkin = kqGroupComInfo.getAuto_checkin(groupId);
              String auto_checkout = kqGroupComInfo.getAuto_checkout(groupId);
              if("1".equalsIgnoreCase(auto_checkout) || "1".equalsIgnoreCase(auto_checkin)){
                //当前班次存在自动打卡设置，修改班次后，不影响当天的自动打卡时间，变更后的班次自动打卡需要第二天才起作用
                retmap.put("message",  "当前班次存在自动打卡设置，修改班次后，不影响当天的自动打卡时间，变更后的班次自动打卡需要第二天才起作用");
                break;
              }
            }
          }
        }
      }

    }catch (Exception e){
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      writeLog(e);
    }

    return retmap;
  }

  /**
   * 编辑班次基本信息
   * @param retmap
   * @param jsonObj
   */
  public void edit(Map<String,Object> retmap,JSONObject jsonObj) throws Exception{
    RecordSetTrans rst = new RecordSetTrans();
    rst.setAutoCommit(true);
    RecordSet rs = new RecordSet();
    String serialid = Util.null2String(jsonObj.get("id"));
    String serial = Util.null2String(jsonObj.get("serial"));//班次名称
    String subcompanyid = Util.null2o(Util.null2String(jsonObj.get("subcompanyid")));
    String shiftOnOffWorkCount = Util.null2o(Util.null2String(jsonObj.get("shiftonoffworkcount")));//一天内上下班次数
    String punchSettings = "1";//打卡时段是否开启 1表示开启
    String isOffDutyFreeCheck = Util.null2o(Util.null2String(jsonObj.get("isoffdutyfreecheck")));//允许下班不打卡 1表示开启
    String isRestTimeOpen = Util.null2o(Util.null2String(jsonObj.get("isresttimeopen")));//排除休息时间是否开启  1表示开启
    String worktime = Util.null2o(Util.null2String(jsonObj.get("worktime")));//工作时长
//    String color = Util.null2o(Util.null2String(jsonObj.get("color")));//工作时长
    String color = "#000";
    String cardRemind = Util.null2s(jsonObj.getString("cardRemind"),"0");//是否开启打卡提醒：0-不开启、1-开启。默认不开启
    String cardRemOfSignIn = Util.null2s(jsonObj.getString("cardRemOfSignIn"),"1");//上班打卡提醒：0-不提醒、1-自定义提前提醒分钟数。默认为1
    String minsBeforeSignIn = Util.null2s(jsonObj.getString("minsBeforeSignIn"),"10");//自定义提前提醒分钟数。默认10分钟
    String cardRemOfSignOut = Util.null2s(jsonObj.getString("cardRemOfSignOut"),"1");//下班打卡提醒：0-不提醒、1-自定义延后提醒分钟数。默认为1
    String minsAfterSignOut = Util.null2s(jsonObj.getString("minsAfterSignOut"),"0");//自定义延后提醒分钟数。默认0分钟
    String remindMode = Util.null2s(jsonObj.getString("remindMode"),"1");//提醒方式：1-消息中心提醒、2-邮件提醒、3-短信提醒。默认消息中心提醒
    String remindOnPC = Util.null2s(jsonObj.getString("remindOnPC"),"0");//登陆PC端弹窗提醒：0-不开启、1-开启

    String halfcalrule = Util.null2s(jsonObj.getString("halfcalrule"),"0");//半天计算规则
    String halfcalpoint = Util.null2s(jsonObj.getString("halfcalpoint"),"");//半天分界点
    String halfcalpoint2cross = Util.null2s(jsonObj.getString("halfcalpoint2cross"),"0");//当日


    if(duplicationCheck(serial,serialid)){
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(389019,user.getLanguage()));
      return ;
    }
    String[] works = new String[]{"start","end"};
    JSONArray workSections = (JSONArray)jsonObj.get("workSections");
    checkRule(retmap,workSections,works);

    if(!retmap.isEmpty()){
      return ;
    }

    String mainSql = "update kq_ShiftManagement set serial=?,subcompanyid=?,shiftonoffworkcount=?,punchsettings=?,isoffdutyfreecheck=?,isresttimeopen=?,worktime=?,color=?," +
        "cardRemind=? ,cardRemOfSignIn=? ,minsBeforeSignIn=? ,cardRemOfSignOut=? ,minsAfterSignOut=? ,remindMode=? ,remindOnPC=?,halfcalrule=?,halfcalpoint=?,halfcalpoint2cross=? where id = ? ";

    boolean isUpdated = rst.executeUpdate(mainSql, serial,subcompanyid,shiftOnOffWorkCount,punchSettings,isOffDutyFreeCheck,isRestTimeOpen,worktime,color,
        cardRemind ,cardRemOfSignIn ,minsBeforeSignIn ,cardRemOfSignOut ,minsAfterSignOut ,remindMode ,remindOnPC,halfcalrule,halfcalpoint,halfcalpoint2cross,serialid);

    if(isUpdated){
      //对于休息时间和工作时间，直接删除后重新创建
      String delRestSql = "delete from kq_ShiftRestTimeSections where serialid = ? ";
      rs = new RecordSet();
      rst.executeUpdate(delRestSql, serialid);
      String delWorkSql = "delete from kq_ShiftOnOffWorkSections where serialid = ? ";
      rs = new RecordSet();
      rst.executeUpdate(delWorkSql, serialid);

      //休息时间 resttype:start开始时间,end结束时间
      JSONArray restTimeSections = (JSONArray)jsonObj.get("restTimeSections");
      //工作时间 across是否跨天，1表示跨天;beginMin上班前分钟数开始签到，endMin下班后分钟数停止签退;times具体上下班时间;onOffWorkType:start开始时间,end结束时间

      String restSql = "insert into kq_ShiftRestTimeSections(serialid,resttype,time,across,record1,orderId) values(?,?,?,?,?,?)";
      int restCount = restTimeSections.size();
      rs = new RecordSet();
      for(int i = 0 ; i < restCount ; i++){
        JSONObject jsonRest = ((JSONObject)restTimeSections.get(i));
        if(jsonRest.containsKey("start") && jsonRest.containsKey("end")){
          String record=Util.null2String(jsonRest.get("record"));
          String orderId=Util.null2String(jsonRest.get("orderId"));

          JSONObject start_jsonRest = (JSONObject) jsonRest.get("start");
          String time=Util.null2String(start_jsonRest.get("time"));
          String resttype="start";
          String across=Util.null2String(start_jsonRest.get("accross"));
          rst.executeUpdate(restSql, serialid,resttype,time,across,record,orderId);

          JSONObject end_jsonRest = (JSONObject) jsonRest.get("end");
          time=Util.null2String(end_jsonRest.get("time"));
          resttype="end";
          across=Util.null2String(end_jsonRest.get("accross"));
          rst.executeUpdate(restSql, serialid,resttype,time,across,record,orderId);
        }
      }
      rs = new RecordSet();
      String workSql = "insert into kq_ShiftOnOffWorkSections(serialid,across,mins,times,onoffworktype,record,mins_next) values(?,?,?,?,?,?,?)";
      int workCount = workSections.size();
      for(int i = 0 ; i < workCount ; i++){
        JSONObject jsonWork = ((JSONObject)workSections.get(i));
        String record = Util.null2String(jsonWork.get("record"));
        for(int j = 0 ; j < works.length ; j++){
          String onOffWorkType=works[j];
          JSONObject inWork=(JSONObject)jsonWork.get(onOffWorkType);
          String across=Util.null2String(inWork.get("across"));
          String mins = Util.null2s(Util.null2String(inWork.get("mins")),"0");
          String times=Util.null2String(inWork.get("times"));
          String mins_next=Util.null2String(inWork.get("mins_next"));
          rst.executeUpdate(workSql, serialid,across,mins,times,onOffWorkType,record,mins_next);
        }
      }
      retmap.put("id", serialid);
      retmap.put("status", "1");
      retmap.put("message", SystemEnv.getHtmlLabelName(18758, user.getLanguage()));
    }else{
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
    }

  }

  /**
   * 新增班次基本信息
   * @param retmap
   * @param jsonObj
   */
  public void add(Map<String,Object> retmap,JSONObject jsonObj) throws Exception{
    RecordSetTrans rst = new RecordSetTrans();
    rst.setAutoCommit(true);
    RecordSet rs = new RecordSet();
    String subcompanyid = Util.null2o(Util.null2String(jsonObj.get("subcompanyid")));
    String serial = Util.null2String(jsonObj.get("serial"));//班次名称
    String shiftOnOffWorkCount = Util.null2o(Util.null2String(jsonObj.get("shiftonoffworkcount")));//一天内上下班次数
    String punchSettings = "1";//打卡时段是否开启 1表示开启
    String isOffDutyFreeCheck = Util.null2o(Util.null2String(jsonObj.get("isoffdutyfreecheck")));//允许下班不打卡 1表示开启
    String isRestTimeOpen = Util.null2o(Util.null2String(jsonObj.get("isresttimeopen")));//排除休息时间是否开启  1表示开启
    String worktime = Util.null2o(Util.null2String(jsonObj.get("worktime")));//工作时长
//    String color = Util.null2o(Util.null2String(jsonObj.get("color")));//工作时长
    String color = "#000";
    String uuid = UUID.randomUUID().toString();//uuid供查询使用
    String cardRemind = Util.null2s(jsonObj.getString("cardRemind"),"0");//是否开启打卡提醒：0-不开启、1-开启。默认不开启
    String cardRemOfSignIn = Util.null2s(jsonObj.getString("cardRemOfSignIn"),"1");//上班打卡提醒：0-不提醒、1-自定义提前提醒分钟数。默认为1
    String minsBeforeSignIn = Util.null2s(jsonObj.getString("minsBeforeSignIn"),"10");//自定义提前提醒分钟数。默认10分钟
    String cardRemOfSignOut = Util.null2s(jsonObj.getString("cardRemOfSignOut"),"1");//下班打卡提醒：0-不提醒、1-自定义延后提醒分钟数。默认为1
    String minsAfterSignOut = Util.null2s(jsonObj.getString("minsAfterSignOut"),"0");//自定义延后提醒分钟数。默认0分钟
    String remindMode = Util.null2s(jsonObj.getString("remindMode"),"1");//提醒方式：1-消息中心提醒、2-邮件提醒、3-短信提醒。默认消息中心提醒
    String remindOnPC = Util.null2s(jsonObj.getString("remindOnPC"),"0");//登陆PC端弹窗提醒：0-不开启、1-开启

    String halfcalrule = Util.null2s(jsonObj.getString("halfcalrule"),"0");//半天计算规则
    String halfcalpoint = Util.null2s(jsonObj.getString("halfcalpoint"),"");//半天分界点
    String halfcalpoint2cross = Util.null2s(jsonObj.getString("halfcalpoint2cross"),"0");//当日

    if(duplicationCheck(serial,"")){
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(389019,user.getLanguage()));
      return ;
    }
    //工作时间 across是否跨天，1表示跨天;beginMin上班前分钟数开始签到，endMin下班后分钟数停止签退;times具体上下班时间;onOffWorkType:start开始时间,end结束时间

    JSONArray workSections = (JSONArray)jsonObj.get("workSections");
    String[] works = new String[]{"start","end"};
    checkRule(retmap,workSections,works);

    if(!retmap.isEmpty()){
      return ;
    }
    //color改为前台获取
//    String color = getRandomColor();
    boforeLog(uuid);

    String mainSql = "insert into kq_ShiftManagement(serial,subcompanyid,shiftonoffworkcount,punchsettings,isoffdutyfreecheck,isresttimeopen,worktime,uuid,color," +
        "cardRemind ,cardRemOfSignIn ,minsBeforeSignIn ,cardRemOfSignOut ,minsAfterSignOut ,remindMode ,remindOnPC,halfcalrule,halfcalpoint,halfcalpoint2cross)"
        + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    boolean isUpdated = rst.executeUpdate(mainSql, serial,subcompanyid,shiftOnOffWorkCount,punchSettings,isOffDutyFreeCheck,isRestTimeOpen,worktime,uuid,color,
        cardRemind ,cardRemOfSignIn ,minsBeforeSignIn ,cardRemOfSignOut ,minsAfterSignOut ,remindMode ,remindOnPC,halfcalrule,halfcalpoint,halfcalpoint2cross);

    if(isUpdated){
      int serialid = 0;
      String idSql = "select id from kq_ShiftManagement where uuid=? and (isdelete is null or  isdelete <> '1') ";
      rs = new RecordSet();
      rs.executeQuery(idSql,uuid);
      if(rs.next()) {
        serialid = rs.getInt("id");
      }

      if(serialid > 0){

        //休息时间 resttype:start开始时间,end结束时间
        JSONArray restTimeSections = (JSONArray)jsonObj.get("restTimeSections");

        String restSql = "insert into kq_ShiftRestTimeSections(serialid,resttype,time,across,record1,orderId) values(?,?,?,?,?,?)";
        int restCount = restTimeSections.size();
        rs = new RecordSet();
        for(int i = 0 ; i < restCount ; i++){
          JSONObject jsonRest = ((JSONObject)restTimeSections.get(i));
          if(jsonRest.containsKey("start") && jsonRest.containsKey("end")){
            String record=Util.null2String(jsonRest.get("record"));
            String orderId=Util.null2String(jsonRest.get("orderId"));
            JSONObject start_jsonRest = (JSONObject) jsonRest.get("start");
            String time=Util.null2String(start_jsonRest.get("time"));
            String resttype="start";
            String across=Util.null2String(start_jsonRest.get("accross"));
            rst.executeUpdate(restSql, serialid,resttype,time,across,record,orderId);

            JSONObject end_jsonRest = (JSONObject) jsonRest.get("end");
            time=Util.null2String(end_jsonRest.get("time"));
            resttype="end";
            across=Util.null2String(end_jsonRest.get("accross"));
            rst.executeUpdate(restSql, serialid,resttype,time,across,record,orderId);
          }
        }
        rs = new RecordSet();
        String workSql = "insert into kq_ShiftOnOffWorkSections(serialid,across,mins,times,onoffworktype,record,mins_next) values(?,?,?,?,?,?,?)";
        int workCount = workSections.size();
        for(int i = 0 ; i < workCount ; i++){
          JSONObject jsonWork = ((JSONObject)workSections.get(i));
          String record = Util.null2String(jsonWork.get("record"));
          for(int j = 0 ; j < works.length ; j++){
            String onOffWorkType=works[j];
            JSONObject inWork=(JSONObject)jsonWork.get(onOffWorkType);
            String across=Util.null2String(inWork.get("across"));
            String mins = Util.null2s(Util.null2String(inWork.get("mins")),"1");
            String times=Util.null2String(inWork.get("times"));
            String mins_next=Util.null2String(inWork.get("mins_next"));
            rst.executeUpdate(workSql, serialid,across,mins,times,onOffWorkType,record,mins_next);
          }
        }
        retmap.put("status", "1");
        retmap.put("id", serialid);
        retmap.put("message", SystemEnv.getHtmlLabelName(18758, user.getLanguage()));
      }else{
        retmap.put("status", "-1");
        retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      }
    }else{
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
    }
  }

  private void checkRule(Map<String, Object> retmap,
      JSONArray workSections, String[] works) {
    for(int i = 0 ; i < workSections.size() ; i++) {
      JSONObject jsonWork = ((JSONObject) workSections.get(i));
      for(int j = 0 ; j < works.length ; j++){
        String onOffWorkType=works[j];
        JSONObject inWork=(JSONObject)jsonWork.get(onOffWorkType);
        String mins = Util.null2String(inWork.get("mins"));
        if(mins.length() == 0 || Util.getIntValue(mins) == 0){
          retmap.put("status", "-1");
          retmap.put("message",  ""+ SystemEnv.getHtmlLabelName(10005343,weaver.general.ThreadVarLanguage.getLang())+"");
          break;
        }
      }
    }
  }

  /**
   * 判断是否重名
   * @param serial
   * @param serialid 为空表示新增
   * @return
   */
  private boolean duplicationCheck(String serial,String serialid){
    boolean isDuplicated = false;
    RecordSet rs = new RecordSet();
    String checkSql = "select 1 from kq_ShiftManagement where serial=? and (isdelete is null or  isdelete <> '1') ";
    if(serialid.length() > 0){
      checkSql += " and id != "+serialid;
    }
    rs.executeQuery(checkSql, Util.null2s(serial, "").trim());
    if(rs.next()){
      isDuplicated = true;
    }
    return isDuplicated;
  }

  /**
   * 生成随机的颜色
   * @return
   */
  private String getRandomColor(){

    RecordSet rs = new RecordSet();
    List<String> colorLists = new ArrayList<>();
    String hasSameColor = "select color from kq_ShiftManagement group by color ";
    rs.executeQuery(hasSameColor);
    while (rs.next()){
      colorLists.add(rs.getString("color"));
    }

    String color = "";
    Random random = null;

    int i = 0 ;
//    while(true){
//      random = new Random();
//      //颜色就要深色的
//      String[] colors = new String[]{"0","1","2","3","4","5","6"};
//      int not_r = random.nextInt(16);
//      int not_g = random.nextInt(16);
//      int not_b = random.nextInt(16);
//      int not_r1 = random.nextInt(16);
//      int not_g1 = random.nextInt(16);
//      int not_b1 = random.nextInt(16);
//      color = "#"+colors[not_r]+colors[not_g]+colors[not_b]+colors[not_r1]+colors[not_g1]+colors[not_b1];
//      //以防死锁
//      if(i > 1000){
//        break;
//      }
//      if(!colorLists.contains(color)){
//        break;
//      }
//      i++;
//    }

    return color;
  }

  public void boforeLog(String uuid){
    BizLogContext logContext = new BizLogContext();
    logContext.setDateObject(new Date());
    logContext.setLogType(BizLogType.HRM_ENGINE);
    logContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_SHIFTMANAGER);
    logContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_SHIFTMANAGER);
    logContext.setParams(params);

    String log_mainSql = " select * from kq_ShiftManagement where uuid in('"+uuid+"')";
    logger.setMainSql(log_mainSql);//主表sql
    logger.setMainPrimarykey("id");//主日志表唯一key
    logger.setMainTargetNameColumn("serial");

    SimpleBizLogger.SubLogInfo subLogInfo1 = logger.getNewSubLogInfo();
    String subSql1 = "select * from kq_ShiftOnOffWorkSections where serialid in (select id from kq_ShiftManagement where uuid in('"+uuid+"'))" ;
    subLogInfo1.setSubTargetNameColumn("times");
    subLogInfo1.setGroupId("0");  //所属分组， 按照groupid排序显示在详情中， 不设置默认按照add的顺序。
    subLogInfo1.setSubGroupNameLabel(27961); //在详情中显示的分组名称，不设置默认显示明细x
    subLogInfo1.setSubSql(subSql1);
    logger.addSubLogInfo(subLogInfo1);

    SimpleBizLogger.SubLogInfo subLogInfo = logger.getNewSubLogInfo();
    String subSql = " select * from kq_ShiftRestTimeSections where serialid in (select id from kq_ShiftManagement where uuid in('"+uuid+"'))" ;
    subLogInfo.setSubSql(subSql);
    subLogInfo.setSubTargetNameColumn("time");
    subLogInfo.setGroupId("1");  //所属分组， 按照groupid排序显示在详情中， 不设置默认按照add的顺序。
    subLogInfo.setSubGroupNameLabel(505603); //在详情中显示的分组名称，不设置默认显示明细x
    logger.addSubLogInfo(subLogInfo);
    logger.before(logContext);
  }
}
