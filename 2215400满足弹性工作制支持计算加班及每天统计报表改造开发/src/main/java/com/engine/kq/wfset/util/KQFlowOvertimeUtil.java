package com.engine.kq.wfset.util;

import com.alibaba.fastjson.JSON;
import com.customization.qc2215400.FreeOverTimeFlowUtil;
import com.customization.qc2215400.SplitBeanSplit;
import com.engine.kq.biz.KQFLowEventLogBiz;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.biz.chain.duration.NonDayUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonHalfUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonHourUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonWholeUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonWorkDurationChain;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Maps;

import java.time.format.DateTimeFormatter;
import java.util.*;

import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;

public class KQFlowOvertimeUtil {
  private KQLog kqLog = new KQLog();

  /**
   * 拆分加班数据 生成splitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @param uuid
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQOvertimeAction(Map<String, String> sqlMap,
      List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
      int requestId, ResourceComInfo rci, String main_uuid) throws Exception{
    KQFlowUtil kqFlowUtil = new KQFlowUtil();
    KQFLowEventLogBiz kqfLowEventLogBiz = new KQFLowEventLogBiz();

    KQWorkTime kqWorkTime = new KQWorkTime();
    RecordSet rs1 = new RecordSet();
    Map<String,String> result = new HashMap<>();

    if(!sqlMap.isEmpty()){
      for(Map.Entry<String,String> me : sqlMap.entrySet()){
        String key = me.getKey();
        String value = me.getValue();
        String concort = "###" ;
        int usedetail =  Util.getIntValue(key.split(concort)[2], 0);
        String prefix = "";
        if(usedetail == 1){
          prefix = "detail_";
        }

        rs1.execute(value);

        while (rs1.next()) {

          String resourceId = Util.null2s(rs1.getString(prefix+"resourceId"), "");
          String fromDate = Util.null2s(rs1.getString(prefix+"fromDate"), "");
          WorkTimeEntity kqWorkTimeEntity = kqWorkTime.getWorkTime(resourceId,fromDate);

          String actionKey = resourceId+"_"+fromDate;
          Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();
          workTimeEntityLogMap.put("resourceid", resourceId);
          workTimeEntityLogMap.put("splitDate", fromDate);
          workTimeEntityLogMap.put("workTimeEntity", kqWorkTimeEntity);
          String uuid = kqfLowEventLogBiz.logDetailWorkTimeEntity(resourceId,workTimeEntityLogMap,main_uuid,"handleKQOvertimeAction|加班生成调休|key|"+actionKey);

          if(kqWorkTimeEntity != null){
            String kqType = Util.null2String(kqWorkTimeEntity.getKQType());

            //==zj 自由班制的加班流程就在这里处理
            if("3".equalsIgnoreCase(kqType)){
              //==zj 自由班次加班直接插入加班表
              SplitBean splitBean = new SplitBean();
              kqFlowUtil.fillSplitBean(splitBean, rs1, "" + requestId, rci, "" + workflowId, DurationTypeEnum.OVERTIME, key, result, datetimeFormatter, "");
              //自由班次单独获取onedayhour
              String newLeaveType = splitBean.getNewLeaveType();
              double oneDayHour = KQFlowUtil.getOneDayHour(DurationTypeEnum.OVERTIME,newLeaveType);
              splitBean.setOneDayHour(oneDayHour);
              new BaseBean().writeLog("==zj==(oneDayHour测试)"+oneDayHour);

              //自由班次获取groupid
              /*RecordSet rs = new RecordSet();
              String GroupIdSql = "select * from kq_groupmember where (isDelete is null or isDelete !=1) and typevalue=" + resourceId;
              new BaseBean().writeLog("==zj==(自由班制获取考勤组sql)" + GroupIdSql);
              rs.executeQuery(GroupIdSql);
              if (rs.next()){
                String groupid = String.valueOf(rs.getInt("groupid"));
                new BaseBean().writeLog("==zj==(获取groupid)"+groupid);
                splitBean.setGroupid(groupid);
              }*/

              KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
              String kqGroupId = kqGroupMemberComInfo.getKQGroupId(resourceId, DateUtil.getCurrentDate());
              new BaseBean().writeLog("==zj==(人员id--获取考勤组id)" +resourceId + " | " +  kqGroupId);
              if (kqGroupId.length()>0){
                splitBean.setGroupid(kqGroupId);
              }

              FreeOverTimeFlowUtil freeOverTimeFlowUtil = new FreeOverTimeFlowUtil();
              //将表单初始数据进行拆分
              SplitBeanSplit splitBeanSplit = new SplitBeanSplit();

              List<SplitBean> splitBeanList = splitBeanSplit.split(splitBean);
              int count = 0;//这个是用来看是否跨天
              new BaseBean().writeLog("==zj==(这里是拆分好的数据集合)" + JSON.toJSONString(splitBeanList));
              count = splitBeanList.size(); //如果为1说明没有跨天，如果大于1说明有跨天
              new BaseBean().writeLog("==zj==(统计跨天)" + count);
              for (int i = 0; i <splitBeanList.size(); i++) {
                freeOverTimeFlowUtil.Add(splitBeanList.get(i),count);
              }

              //这是不拆分的
//              freeOverTimeFlowUtil.Add(splitBean);
              continue;
            }
          }
          SplitBean splitBean = new SplitBean();
          boolean isFillRight = kqFlowUtil.fillSplitBean(splitBean, rs1, ""+requestId, rci, ""+workflowId, DurationTypeEnum.OVERTIME,key,result,datetimeFormatter,"");
          if(!isFillRight){
            return result;
          }
          Map<String,Object> eventMap = Maps.newHashMap();
          eventMap.put("sql", value);
          eventMap.put("sql拼装后的bean|splitBean", splitBean);
          kqfLowEventLogBiz.logDetailEvent(resourceId,eventMap,uuid,"handleKQOvertimeAction|加班生成调休|key|"+actionKey);
          List<SplitBean> tmp_splitBeans = new ArrayList<>();
          doNonWorkSplitChain(splitBean, tmp_splitBeans);
          splitBeans.addAll(tmp_splitBeans);
        }

      }
    }
    return result;
  }

  /**
   * 计算非工作时长拆分
   * @param splitBean
   * @param splitBeans
   * @throws Exception
   */
  public void doNonWorkSplitChain(SplitBean splitBean,List<SplitBean> splitBeans) throws Exception{

    NonWorkDurationChain hourUnitSplitChain = new NonHourUnitSplitChain(splitBeans);
    NonWorkDurationChain dayUnitSplitChain = new NonDayUnitSplitChain(splitBeans);
    NonWorkDurationChain halfUnitSplitChain = new NonHalfUnitSplitChain(splitBeans);
    NonWorkDurationChain wholeUnitSplitChain = new NonWholeUnitSplitChain(splitBeans);

    //设置执行链
    hourUnitSplitChain.setDurationChain(dayUnitSplitChain);
    dayUnitSplitChain.setDurationChain(halfUnitSplitChain);
    halfUnitSplitChain.setDurationChain(wholeUnitSplitChain);
    //把初始数据设置进去
    hourUnitSplitChain.handleDuration(splitBean);

  }


  /**
   * 加班流程单独需要赋值的数据
   * @param prefix
   * @param rs1
   * @param splitBean
   */
  public static void bean4Overtime(String prefix, RecordSet rs1, SplitBean splitBean) {
    String overtime_type = Util.null2s(rs1.getString(prefix+"overtime_type"), "-1");
    String minimumUnit = Util.null2s(Util.null2String(KQOvertimeRulesBiz.getMinimumUnit()),"-1");
    String computingMode = "1";
    splitBean.setDurationrule(minimumUnit);
    splitBean.setComputingMode(computingMode);
    splitBean.setOvertime_type(overtime_type);
  }

}
