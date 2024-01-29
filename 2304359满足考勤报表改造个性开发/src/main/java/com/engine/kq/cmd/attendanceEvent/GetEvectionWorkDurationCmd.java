package com.engine.kq.cmd.attendanceEvent;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2304359.KqReportUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQTravelRulesBiz;
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
 * 出差用的时长计算
 */
public class GetEvectionWorkDurationCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetEvectionWorkDurationCmd(Map<String, Object> params, User user) {
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
      String timestamp = Util.null2String(params.get("timestamp"));

      String durationrule = Util.null2String(KQTravelRulesBiz.getMinimumUnit());
      String computingMode = Util.null2String(KQTravelRulesBiz.getComputingMode());
      ResourceComInfo rci = new ResourceComInfo();
      KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
      String groupid = kqGroupMemberComInfo.getKQGroupId(resourceId,fromDate);
      if(resourceId.length() > 0 && groupid.length() == 0){
        retmap.put("status", "-1");
        retmap.put("message",  rci.getLastname(resourceId)+","+fromDate+""+ SystemEnv.getHtmlLabelName(10005329,weaver.general.ThreadVarLanguage.getLang())+"");
        return retmap;
      }

      KQDurationCalculatorUtil kqDurationCalculatorUtil =new KQDurationCalculatorUtil.DurationParamBuilder(resourceId).
          fromDateParam(fromDate).toDateParam(toDate).fromTimeParam(fromTime).toTimeParam(toTime).
          durationRuleParam(durationrule).computingModeParam(computingMode).
          durationTypeEnumParam(DurationTypeEnum.EVECTION).build();

      Map<String,Object> durationMap = kqDurationCalculatorUtil.getWorkDuration();
      new BaseBean().writeLog("==zj==(出差durationMap)" + JSON.toJSONString(durationMap));
            //==zj 出差时长向下取整(0.5h最小单位)
            KqReportUtil kqReportUtil = new KqReportUtil();
            String min_duration = Util.null2String(durationMap.get("min_duration"));
            String duration = kqReportUtil.halfHourCal(min_duration,0);

            retmap.put("duration",duration /*Util.null2String(durationMap.get("duration"))*/);
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
