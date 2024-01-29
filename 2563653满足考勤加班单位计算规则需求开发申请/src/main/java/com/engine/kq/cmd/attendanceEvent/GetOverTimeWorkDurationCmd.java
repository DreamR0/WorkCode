package com.engine.kq.cmd.attendanceEvent;

import com.alibaba.fastjson.JSON;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.util.KQDurationCalculatorUtil;
import java.util.HashMap;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

/**
 * 加班用的时长计算
 */
public class GetOverTimeWorkDurationCmd extends AbstractCommonCommand<Map<String, Object>> {
    public static ThreadLocal<String> threadLocal = new ThreadLocal<>();

	public GetOverTimeWorkDurationCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{
      String resourceId = Util.null2String(params.get("resourceId"));
      String fromDate = Util.null2String(params.get("fromDate"));
      String toDate = Util.null2String(params.get("toDate"));
      String fromTime = Util.null2String(params.get("fromTime"));
      String toTime = Util.null2String(params.get("toTime"));
      String overtime_type = Util.null2String(params.get("overtime_type"));
      String timestamp = Util.null2String(params.get("timestamp"));
      //qc2563653 这里加班单位根据建模表配置来设置，没有再走标准
            threadLocal.set(resourceId);
      int minimumUnit = KQOvertimeRulesBiz.getMinimumUnit();
      new BaseBean().writeLog("==zj==(minimumUnit)" + JSON.toJSONString(minimumUnit));

      //
      KQWorkTime kqWorkTime = new KQWorkTime();
      WorkTimeEntity kqWorkTimeEntity = kqWorkTime.getWorkTime(resourceId,fromDate);
      if(kqWorkTimeEntity != null){
        String kqType = Util.null2String(kqWorkTimeEntity.getKQType());
        if("3".equalsIgnoreCase(kqType)){
          writeLog("自由班制不计算加班");
          retmap.put("status", "1");
          retmap.put("message",  ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005330,weaver.general.ThreadVarLanguage.getLang())+"");
          return retmap;
        }
      }
      ResourceComInfo rci = new ResourceComInfo();
      KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
      String groupid = kqGroupMemberComInfo.getKQGroupId(resourceId,fromDate);
      if(resourceId.length() > 0 && groupid.length() == 0){
        retmap.put("status", "-1");
        retmap.put("message",  rci.getLastname(resourceId)+","+fromDate+""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005329,weaver.general.ThreadVarLanguage.getLang())+"");
        return retmap;
      }


      //加班默认是工作日加班
      KQDurationCalculatorUtil kqDurationCalculatorUtil =new KQDurationCalculatorUtil.DurationParamBuilder(resourceId).
          fromDateParam(fromDate).toDateParam(toDate).fromTimeParam(fromTime).toTimeParam(toTime).computingModeParam("1").
          durationRuleParam(minimumUnit+"").durationTypeEnumParam(DurationTypeEnum.OVERTIME).
          overtime_typeParam(overtime_type).build();

      Map<String,Object> durationMap = kqDurationCalculatorUtil.getWorkDuration();

      retmap.put("duration", Util.null2String(durationMap.get("duration")));
      retmap.put("timestamp", timestamp);
      retmap.put("status", "1");
    }catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

}
