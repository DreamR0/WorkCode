package com.customization.qc2471294;

import com.alibaba.fastjson.JSON;
import com.engine.core.cfg.annotation.CommandDynamicProxy;
import com.engine.core.interceptor.AbstractCommandProxy;
import com.engine.core.interceptor.Command;
import com.engine.meeting.cmd.meetingbase.MeetingChangeCmd;
import weaver.general.BaseBean;

import java.util.Map;

@CommandDynamicProxy(target = MeetingChangeCmd.class,desc = "变更附件信息")
public class MeetingChangeCmdProxy extends AbstractCommandProxy<Map<String,Object>> {

    @Override
    public Map<String, Object> execute(Command<Map<String, Object>> targetCommand) {
       MeetingChangeCmd meetingChangeCmd =(MeetingChangeCmd) targetCommand;
       Map<String,Object> params = meetingChangeCmd.getParams();

       String customFields = new BaseBean().getPropValue("qc2078820","customFields");
       params.put("customFields",customFields);
       new BaseBean().writeLog("==zj==(changProxy)" + JSON.toJSONString(params));
        meetingChangeCmd.setParams(params);
        Map<String,Object> targetmap = nextExecute(targetCommand);
        return targetmap;
    }
}
