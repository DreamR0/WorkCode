package com.engine.meeting.cmd.remindertemplate;

import com.api.meeting.util.FieldUtil;
import com.engine.common.biz.AbstractBizLog;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.Command;
import com.engine.core.interceptor.CommandContext;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.meeting.defined.MeetingFieldComInfo;
import weaver.meeting.defined.MeetingFieldManager;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetRemindVariableFieldsCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetRemindVariableFieldsCmd(User user, Map<String, Object> params) {
        this.user = user;
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map resMap = new HashMap();
        try {
            MeetingFieldManager hfm = new MeetingFieldManager(1);
            MeetingFieldComInfo meetingFieldComInfo = new MeetingFieldComInfo();
            List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
            Map<String, Object> fieldGroupitem = new HashMap<String, Object>();
            List fieldItemlist = new ArrayList();
            List<String> fieldList = hfm.getTemplateField();
            for (String fieldid : fieldList) {
                Map map = new HashMap();
                int fieldlabel = Util.getIntValue(meetingFieldComInfo.getLabel(fieldid));
                String template = "#[" + meetingFieldComInfo.getFieldname(fieldid) + "]";
                //map.put(SystemEnv.getHtmlLabelName(fieldlabel, user.getLanguage()) + "-" + template, template);
                map.put("domekey", template);
                map.put("value", SystemEnv.getHtmlLabelName(fieldlabel, user.getLanguage()));
                fieldItemlist.add(map);
            }
            fieldGroupitem.put("title", SystemEnv.getHtmlLabelName(500143, user.getLanguage()));
            fieldGroupitem.put("defaultshow", true);
            fieldGroupitem.put("items", fieldItemlist);
            grouplist.add(fieldGroupitem);
            String body = "";
            String id = Util.null2String(params.get("id"));
            boolean seatTemplate=false;
            if (!id.isEmpty()) {
                RecordSet rs = new RecordSet();
                rs.execute("select * from meeting_remind_template where id = " + id);
                if (rs.next()) {
                    body = Util.null2String(rs.getString("body"));
                    body = body.replaceAll("<br />","\n");
                    if("seat".equals(rs.getString("modetype"))){
                        seatTemplate=true;
                    }
                }
            }
            if(seatTemplate){//座位号提醒模板
                Map map = new HashMap();
                map.put("domekey", "#[seatNum]");
                map.put("value", SystemEnv.getHtmlLabelName(15801, user.getLanguage()));//座位号
                fieldItemlist.add(0,map);
            }
            List bodyItemlist = new ArrayList();
            Map<String, Object> bodyGroupitem = new HashMap<String, Object>();
            bodyItemlist.add(FieldUtil.getFormItemForTextArea("bodymsg", SystemEnv.getHtmlLabelName(28053, user.getLanguage()), body, 2));
            bodyGroupitem.put("title", SystemEnv.getHtmlLabelName(18693, Util.getIntValue(user.getLanguage())));
            bodyGroupitem.put("defaultshow", true);
            bodyGroupitem.put("items", bodyItemlist);
            grouplist.add(bodyGroupitem);
            resMap.put("fields", grouplist);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resMap;
    }
}
