package com.api.customization.qc2452784.Util;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.util.ConditionFactory;
import com.api.browser.util.ConditionType;
import com.api.govern.util.FieldUtil;
import com.engine.hrm.biz.HrmSanyuanAdminBiz;
import com.engine.meeting.util.MeetingNoRightUtil;
import com.engine.meeting.util.MeetingSelectOptionsUtil;
import weaver.general.Util;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.meeting.defined.MeetingFieldComInfo;
import weaver.meeting.defined.MeetingFieldManager;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class RemindUtil {

    /**
     * 获取自定义邮件提醒列表
     * @return
     */
    public List<Object> getRemindList(User user){
        RecordSet rs = new RecordSet();
        String sql = "";
        List<Object> datasList = new ArrayList<>();
        Map<String,Object> data = null;

        try{
            String tableName = new BaseBean().getPropValue("qc2452784","tableName");
            sql = "select  b.name,c.name as 'modename',d.name as 'meetingtype',a.body,a.id from uf_remind_custom a" +
                    " left join meeting_remind_type b on a.type = b.id" +
                    " left join meeting_remind_mode c on a.modetype = c.type"+
                    " left join meeting_type d on a.meetingtype = d.id";
            new BaseBean().writeLog("==zj==(获取自定义邮件提醒列表sql)" + sql);
            rs.executeQuery(sql);
            while (rs.next()){
                data = new HashMap<>();
                String id = Util.null2String(rs.getString("id"));   //提醒方式
                String name = Util.null2String(rs.getString("name"));   //提醒方式
                String modename = Util.null2String(rs.getString("modename"));   //提醒类型
                String meetingType = Util.null2String(rs.getString("meetingtype"));   //会议类型
                String body = Util.null2String(rs.getString("body"));   //模板内容

                data.put("id",id);
                data.put("name",name);
                data.put("modename",modename);
                data.put("meetingname",meetingType);
                data.put("body",body);

                datasList.add(data);
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(自定义邮件提醒列表错误)" + e);
        }
        return datasList;
    }

    /**
     * 删除自定义邮件列表
     * @param user
     * @param params
     * @return
     */
    public Map<String,Object> deleteRemindCustom(User user,Map<String, Object> params){
        RecordSet rs = new RecordSet();
        Map<String,Object> result = new HashMap<>();
        String ids = JSON.toJSONString(params.get("ids"));   //获取删除列表id
        String tableName = new BaseBean().getPropValue("qc2452784","tableName");
        Boolean isDelete = false;

        try{
            if (!"".equals(ids)){
                String[] idsSplit = ids.split(",");
                for (int i = 0; i < idsSplit.length; i++) {
                    String sql = "delete from " + tableName + " where id = "+idsSplit[i];
                    isDelete = rs.executeUpdate(sql);
                }
                if (isDelete){
                    result.put("code","1");
                    result.put("message","删除成功");
                }
            }else {
                result.put("code","-1");
                result.put("message","获取id为空，删除失败");
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(删除自定义邮件提醒列表错误)" + e);
        }
        return result;
    }

    /**
     * 获取弹窗基本信息接口
     * @return
     */

    public Map<String,Object> getremindField(User user,Map<String, Object> params){
        new BaseBean().writeLog("==zj==(方法进入)");
        ConditionFactory conditionFactory = new ConditionFactory(user);
        Map resMap = new HashMap();
        try {
            int languageid = user.getLanguage();
            String id = Util.null2String(params.get("id"));
            String type = "";
            String mode = "";
            String titlemsg = "";
            String body = "";
            String meetingType = "";
            if (!id.isEmpty()) {
                RecordSet rs = new RecordSet();
                String tableName = new BaseBean().getPropValue("qc2452784","tableName");
                rs.execute("select * from "+tableName+" where id = " + id);
                if (rs.next()) {
                    type = Util.null2String(rs.getString("type"));
                    mode = Util.null2String(rs.getString("modetype"));
                    titlemsg= Util.null2String(rs.getString("title"));;
                    body = Util.null2String(rs.getString("body"));
                    meetingType = Util.null2String(rs.getString("meetingtype"));
                }
            }else {
                type = "3";
                mode = "create";
            }
            //会议类型浏览按钮设置
                SearchConditionItem sci = conditionFactory.createCondition(ConditionType.BROWSER, 2104, "meetingtype", 89+"");
                sci.getBrowserConditionParam().setHideAdvanceSearch(false);

            MeetingSelectOptionsUtil meetingSelectOptionsUtil = new MeetingSelectOptionsUtil();
            List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
            Map<String, Object> groupitem = new HashMap<String, Object>();
            List itemlist = new ArrayList();

            //提醒方式
            Map typeMap = FieldUtil.getFormItemForSelect("type", SystemEnv.getHtmlLabelName(18713, languageid), type, 1, meetingSelectOptionsUtil.getRemindTypeOption(false, languageid));
            typeMap.put("hasBorder", true);
            itemlist.add(typeMap);
            //提醒模式
            Map modeMap = FieldUtil.getFormItemForSelect("mode", SystemEnv.getHtmlLabelName(82212, languageid), mode, 1, meetingSelectOptionsUtil.getRemindModeOption(false, languageid));
            modeMap.put("hasBorder", true);
            itemlist.add(modeMap);
            //会议类型
            Map meetingMap = FieldUtil.getFormItemForBrowser(user,sci, "meetingtype", SystemEnv.getHtmlLabelNames("2104", user.getLanguage()), 89 + "", meetingType,2);
            meetingMap.put("hasBorder", true);
            itemlist.add(meetingMap);

            new BaseBean().writeLog("==zj==(itemList)" + JSON.toJSONString(itemlist));
            new BaseBean().writeLog("==zj==(titlemsg)" +JSON.toJSONString(titlemsg));

            //这里把标题判断放开
            /*if (!"".equals(titlemsg)) {*/
                Map titleField =  FieldUtil.getFormItemForInput("titlemsg", SystemEnv.getHtmlLabelName(386540, languageid), titlemsg, 2);
                titleField.put("ref","title");
                titleField.put("onFocus","{value => console.log('1234')}");
                itemlist.add(titleField);
            /*}*/
            groupitem.put("title", SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
            groupitem.put("defaultshow", true);
            groupitem.put("items", itemlist);
            grouplist.add(groupitem);
            resMap.put("fields", grouplist);

            new BaseBean().writeLog("==zj==(获取弹窗基本信息接口)" + JSON.toJSONString(resMap));
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取弹窗基本信息接口错误)" + e);
        }
        return resMap;
    }

    /**
     * 获取弹窗模板内容
     * @param user
     * @param params
     * @return
     */
    public  Map<String, Object> getVariable(User user,Map<String, Object> params){
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
                int fieldlabel = weaver.general.Util.getIntValue(meetingFieldComInfo.getLabel(fieldid));
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
            String id = weaver.general.Util.null2String(params.get("id"));
            boolean seatTemplate=false;
            if (!id.isEmpty()) {
                RecordSet rs = new RecordSet();
                String tableName = new BaseBean().getPropValue("qc2452784","tableName");
                rs.execute("select * from "+tableName + " where id = " + id);
                if (rs.next()) {
                    body = weaver.general.Util.null2String(rs.getString("body"));
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
            bodyItemlist.add(com.api.meeting.util.FieldUtil.getFormItemForTextArea("bodymsg", SystemEnv.getHtmlLabelName(28053, user.getLanguage()), body, 2));
            bodyGroupitem.put("title", SystemEnv.getHtmlLabelName(18693, weaver.general.Util.getIntValue(user.getLanguage())));
            bodyGroupitem.put("defaultshow", true);
            bodyGroupitem.put("items", bodyItemlist);
            grouplist.add(bodyGroupitem);
            resMap.put("fields", grouplist);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resMap;
    }

    /**
     * 新建、修改模板内容
     * @param user
     * @param params
     * @return
     */
    public Map<String,Object> editRemindCustom(User user,Map<String,Object> params){
        new BaseBean().writeLog("==zj==(新建、修改模板内容数据):" + JSON.toJSONString(params));
        Map map=new HashMap();
        if(!HrmUserVarify.checkUserRight("Meeting:Remind",user)) {
            return MeetingNoRightUtil.getNoRightMap();
        }
        int id= Util.getIntValue(params.get("id").toString(),0);
        String titlemsg= Util.null2String(params.get("titlemsg"));  //模板标题
        String desc_n= Util.null2String(params.get("desc_n"));      //模板描述
        String bodymsg= Util.null2String(params.get("bodymsg"));    //模板内容
        bodymsg = bodymsg.replaceAll("\n","<br />");
        String meetingType = Util.null2String(params.get("meetingtype"));//会议类型
        ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
        boolean mtidetachable = manageDetachComInfo.isUseMtiManageDetach();
        int subid = Util.getIntValue(Util.null2String(params.get("subcompanyid")),0);
        if(mtidetachable){
            if(subid < 0){
                subid = 0;
            }
        }else{
            subid =0;
        }

        ArrayList subcompanylist = null;
        try {
            String subStr = subCompanyComInfo.getRightSubCompany(user.getUID(), "Meeting:Remind",-1);
            subcompanylist = weaver.general.Util.TokenizerString(subStr,",");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //当开启分权,并且未设置机构权限，进入直接返回无权限。
        if (mtidetachable && subcompanylist.size() < 1) {
            return MeetingNoRightUtil.getNoRightMap();
        }
        if( mtidetachable && subid > 0){
            int operatelevel= checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"Meeting:Remind",subid);
            if(operatelevel < 1){
                return MeetingNoRightUtil.getNoRightMap();
            }

        }else if(mtidetachable && subid < 1){
            if(!HrmSanyuanAdminBiz.hasRight(user)){
                return MeetingNoRightUtil.getNoRightMap();
            }
        }
        RecordSet recordSet = new RecordSet();
        String tableName = new BaseBean().getPropValue("qc2452784","tableName");
        if (id <= 0){
            //id为0走新增
            recordSet.executeUpdate("insert into " + tableName+" (type,desc_n,title,body,modetype,meetingtype,subcompanyid) values(?,?,?,?,?,?,?)",3,desc_n,titlemsg,bodymsg,"create",meetingType,subid);
        }else {
            //id不为0走修改
            recordSet.executeUpdate("update " +tableName+" set title='"+titlemsg+"',body='"+bodymsg+"',desc_n='"+desc_n+"' where id= ? and subcompanyid = ?",id, subid);
        }
        map.put("ret", "true");
        return map;
    }
    }

