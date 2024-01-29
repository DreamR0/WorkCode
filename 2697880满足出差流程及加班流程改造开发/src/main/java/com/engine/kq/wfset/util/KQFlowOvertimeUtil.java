package com.engine.kq.wfset.util;

import com.engine.kq.biz.KQFLowEventLogBiz;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
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
            if("3".equalsIgnoreCase(kqType)){
              kqLog.info("自由班制不计算加班:resourceId:"+resourceId);
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
    String isOpenRest = Util.null2s(rs1.getString(prefix+"isOpenRest"), "-1");
    String minimumUnit = Util.null2s(Util.null2String(KQOvertimeRulesBiz.getMinimumUnit()),"-1");
    String computingMode = "1";
    splitBean.setDurationrule(minimumUnit);
    splitBean.setComputingMode(computingMode);
    splitBean.setOvertime_type(overtime_type);
    splitBean.setIsOpenRest(isOpenRest);
  }

}
