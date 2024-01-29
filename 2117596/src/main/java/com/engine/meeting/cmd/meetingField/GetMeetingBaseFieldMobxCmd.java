package com.engine.meeting.cmd.meetingField;

import com.api.browser.bean.BrowserBean;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.*;
import com.api.cube.service.CubeFieldService;
import com.api.govern.manager.GovernActionService;
import com.api.govern.manager.impl.GovernActionServiceImpl;
import com.api.meeting.util.FieldUtil;
import com.api.meeting.util.MeetingPrmUtil;
import com.api.workplan.util.TimeZoneCastUtil;
import com.cloudstore.dev.api.bean.SplitMobileDataBean;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.entity.BizLogContext;
import com.engine.common.service.HrmCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.common.util.ServiceUtil;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.meeting.service.MeetingFieldService;
import com.engine.meeting.service.impl.MeetingFieldServiceImpl;
import com.engine.meeting.util.MeetingEncryptUtil;
import org.apache.commons.lang.StringUtils;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.Maint.MeetingSetInfo;
import weaver.meeting.Maint.MeetingTransMethod;
import weaver.meeting.MeetingBrowser;
import weaver.meeting.MeetingShareUtil;
import weaver.meeting.MeetingUtil;
import weaver.meeting.defined.MeetingFieldComInfo;
import weaver.meeting.defined.MeetingFieldGroupComInfo;
import weaver.meeting.defined.MeetingFieldManager;
import weaver.systeminfo.SystemEnv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GetMeetingBaseFieldMobxCmd extends AbstractCommonCommand<Map<String, Object>> {

    public static String hrmColumns = "";
    static{
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery("select fieldid from meeting_formfield where fieldname = ? or fieldname = ?","hrmDepartments","hrmSubCompanys");
        while (recordSet.next()){
            hrmColumns += hrmColumns.equals("")?recordSet.getString(1):","+recordSet.getString(1);
        }
    }
    private SimpleBizLogger logger;

    public GetMeetingBaseFieldMobxCmd(User user, Map<String, Object> params) {
        this.user = user;
        this.params = params;
        this.logger = new SimpleBizLogger();
    }

    public List<BizLogContext> getLogContexts() {
        return logger.getBizLogContexts();
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        MeetingPrmUtil mpu = null;
        boolean isInterval = false;
        if (params.get("isInterval") != null) {
            isInterval = Util.getIntValue(params.get("isInterval").toString(), 0) == 1;
        }
        String meetingid = Util.null2String(params.get("meetingid"));
        boolean viewMeeting = false;
        if (params.get("viewMeeting") != null) {
            viewMeeting = Boolean.parseBoolean(params.get("viewMeeting").toString());
        }
        params.put("viewMeeting",viewMeeting);
        boolean isChange = false;
        if (params.get("isChange") != null) {
            isChange =  Util.getIntValue(params.get("isChange").toString(), 0) == 1;
        }
        boolean isEdit = true;
        if (params.get("isEdit") != null) {
            isEdit = Boolean.parseBoolean(params.get("isEdit").toString());
        }
        if (params.get("mpu") != null) {
            mpu = (MeetingPrmUtil) params.get("mpu");
        }
        long datetime = new Date().getTime();
        MeetingFieldManager hfm = null;
        try {
            hfm = new MeetingFieldManager(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MeetingFieldComInfo meetingFieldComInfo = new MeetingFieldComInfo();
        MeetingFieldGroupComInfo meetingFieldGroupComInfo = new MeetingFieldGroupComInfo();
        MeetingSetInfo meetingSetInfo = new MeetingSetInfo();
        Map ret = new HashMap();
        List<String> groupList = hfm.getLsGroup();
        List<String> fieldList = null;
        boolean flag = false;
        RecordSet meetingRs = new RecordSet();
        String meetingname = "";
        String meetingstatus = "";
        String meetingtype = "";
        String enddate = "";
        String endtime = "";
        String begindate = "";
        String begintime = "";
        String enddateTemp = "";
        String endtimeTemp = "";
        String begindateTemp = "";
        String begintimeTemp = "";
        String isdecision = "";
        String repeatbegindate = "";
        String repeatenddate = "";
        String repeatType = "";
        String creater = "";
        String address = "";
        String meetingEndDate = "";
        int requestid = 0;
        //PC前端新建/编辑记录secretLevel使用
        String secretLevel = "";//MeetingUtil.DEFAULT_SECRET_LEVEL;
        String secretDeadline = "";
        List baseBrowserColumn = new ArrayList();
        List baseUploadColumn = new ArrayList();
        if (!meetingid.isEmpty()) {
            if(!MeetingEncryptUtil.setMeetingDaoInfo(meetingid, viewMeeting, user, params, meetingRs)){
                ret.put("status", true);
                ret.put("error", "noright");
                return ret;
            }
            meetingRs.executeQuery("select * from meeting where id=?", meetingid);
            meetingRs.next();
            meetingstatus = meetingRs.getString("meetingstatus");
            meetingname = meetingRs.getString("name");
            meetingtype = meetingRs.getString("meetingtype");
            if (Util.getIntValue(meetingRs.getString("repeattype")) > 0) {
                isInterval = true;
            } else {
                isInterval = false;
            }
            flag = true;

            enddate = meetingRs.getString("enddate");
            endtime = meetingRs.getString("endtime");
            begindate = meetingRs.getString("begindate");
            begintime = meetingRs.getString("begintime");
            address = meetingRs.getString("address");
            requestid = meetingRs.getInt("requestid");
            String[] temp = address.split(",");
            List<String> list = Arrays.asList(temp);
            List<String> listA = new ArrayList<String>(list);
            //判断该address是否被封存
            if(!address.equals("") && !viewMeeting){
                List valueParams = new ArrayList();
                RecordSet rs1 = new RecordSet();
                rs1.executeQuery("select id from meetingRoom where id in ("+ DBUtil.getParamReplace(address)+") and (status = '2')  ",DBUtil.trasToList(valueParams,address));
                String addressTemp = "";
                while(rs1.next()){
                    //修改为将封存的会议室从列表中删除，保证复制会议时会议室顺序不发送变化
//                    addressTemp += addressTemp.equals("")?rs1.getString("id"):","+rs1.getString("id");
                    listA.remove(rs1.getString("id"));
                }
//                address = addressTemp;
                address = String.join(",", listA);
            }
            repeatbegindate = meetingRs.getString("repeatbegindate");
            repeatenddate = meetingRs.getString("repeatenddate");
            /* ----------新增日期转换 start ----------------*/
            String changeToB[] = TimeZoneCastUtil.FormatDateLocal((isInterval ? repeatbegindate : begindate) + " " + begintime, 0);
            String changeToE[] = TimeZoneCastUtil.FormatDateLocal((isInterval ? repeatenddate : enddate) + " " + endtime, 1);
            begindateTemp = changeToB[0];
            begintimeTemp = changeToB[1];
            enddateTemp = changeToE[0];
            endtimeTemp = changeToE[1];
            params.put("begindate",begindateTemp);
            params.put("repeatbegindate",begindateTemp);
            params.put("begintime",begintimeTemp);
            params.put("enddate",enddateTemp);
            params.put("repeatenddate",enddateTemp);
            params.put("endtime",endtimeTemp);
            /* ----------新增日期转换 end ----------------*/
            isdecision = meetingRs.getString("isdecision");

            repeatType = meetingRs.getString("repeatType");
            secretLevel = meetingRs.getString("secretLevel");
            secretDeadline = meetingRs.getString("secretDeadline");
            creater = meetingRs.getString("creater");
            if (mpu == null) {
                mpu = new MeetingPrmUtil(user, meetingid);
            }
        }else{
            //新建状态获取默认值
            MeetingUtil meetingUtil = new MeetingUtil();
            Map defaultParams = meetingUtil.getMeetingDefaultValue(user);
            defaultParams.putAll(params);
            params = defaultParams;//兼容params历史数据，为之前二开传参而留

            //获取当前用户有权限的会议类型
            RecordSet set = new RecordSet();
            Set<String> typeSet = new HashSet();
            String typeShareSql = MeetingShareUtil.getTypeShareSql(user);
            set.executeQuery("select a.id,a.name from Meeting_Type a where 1=1 "+typeShareSql +" order by a.dsporder,a.name");
            while(set.next()){
                typeSet.add(Util.null2String(set.getString(1)));
            }
            boolean isShareType = typeSet.contains(Util.null2String(params.get("meetingtype")));
            if(!isShareType){
                params.put("meetingtype","");
            }

            if(params.containsKey("address")){
                address = Util.null2String(params.get("address"));
                //判断该address是否被封存
                if(!address.equals("")){
                    List valueParams = new ArrayList();
                    meetingRs.executeQuery("select id from meetingRoom where id in ("+ DBUtil.getParamReplace(address)+") and (status = '1' or status is null or status = '')  ",DBUtil.trasToList(valueParams,address));
                    String addressTemp = "";
                    while(meetingRs.next()){
                        addressTemp += addressTemp.equals("")?meetingRs.getString("id"):","+meetingRs.getString("id");
                    }
                    address = addressTemp;
                    params.put("address",address);
                }
            }
            //根据会议类型决定召集人/参会人员
            if(params.containsKey("meetingtype")){
                String meetingtypeDefaultValue = Util.null2String(params.get("meetingtype"));
                if(!meetingtypeDefaultValue.equals("")){
                    meetingtype = meetingtypeDefaultValue;
                    //处理召集人
                    Set callerValue = meetingUtil.getMeetingCallers(meetingtypeDefaultValue);
                    if(params.containsKey("caller")){
                        String callerDefaultValue = Util.null2String(params.get("caller"));
                        if(!callerDefaultValue.equals("")){
                            String callerDefaultValueArr[] = callerDefaultValue.split(",");
                            callerValue.addAll(Arrays.asList(callerDefaultValueArr).stream().filter(item->!item.equals("")).collect(Collectors.toSet()));
                        }
                    }
                    if(callerValue.size() == 1){
                        params.put("caller",callerValue.stream().collect(Collectors.joining()));
                    }else{
                        params.put("caller","");
                    }
                    //处理参会人员
                    String hrmMembersValue[] = meetingUtil.getMeetingHrmMembers(meetingtypeDefaultValue);
                    Set hrmMembersSet = new HashSet();
                    hrmMembersSet = Arrays.asList(hrmMembersValue).stream().filter(item->!item.equals("")).collect(Collectors.toSet());
                    if(params.containsKey("hrmmembers")){
                        String hrmmembersDefaultValue = Util.null2String(params.get("hrmmembers"));
                        if(!hrmmembersDefaultValue.equals("")){
                            String hrmmembersDefaultValueArr[] = hrmmembersDefaultValue.split(",");
                            hrmMembersSet.addAll(Arrays.asList(hrmmembersDefaultValueArr).stream().filter(item->!item.equals("")&&MeetingUtil.right4Resource(user,item+"")).collect(Collectors.toSet()));
                        }
                    }
                    if(hrmMembersSet.size() > 0){
                        params.put("hrmmembers",hrmMembersSet.stream().collect(Collectors.joining(",")));
                    }
                    //参会客户
                    Set crmMembersSet = new HashSet();
                    char procFlag=Util.getSeparator() ;
                    meetingRs.executeProc("Meeting_Member_SelectByType",meetingtypeDefaultValue+procFlag+"2");
                    while(meetingRs.next()){
                        crmMembersSet.add(meetingRs.getString("memberid"));
                    }
                    if(params.containsKey("crmmembers")){
                        String crmmembersDefaultValue = Util.null2String(params.get("crmmembers"));
                        if(!crmmembersDefaultValue.equals("")){
                            String hrmmembersDefaultValueArr[] = crmmembersDefaultValue.split(",");
                            crmMembersSet.addAll(Arrays.asList(hrmmembersDefaultValueArr).stream().filter(item->!item.equals("")).collect(Collectors.toSet()));
                        }
                    }
                    if(crmMembersSet.size() > 0){
                        params.put("crmmembers",crmMembersSet.stream().collect(Collectors.joining(",")));
                    }
                }
            }

        }
        String meetingDate = Util.null2String(params.get("meetingDate"));
        if(!meetingDate.equals("") && meetingid.equals("")){
            params.put("begindate",meetingDate);
            params.put("enddate",meetingDate);
        }
        //获取默认值
        boolean ismobile = Util.null2String(params.get("ismobile")).equals("1") ? true : false;
        ConditionFactory conditionFactory = new ConditionFactory(user);
        List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
        Map<String, Object> hideFieldMap = new HashMap<String, Object>();
        List hideColumnlist = new ArrayList();
        Map item;
        //因为手机端不能单独处理browserType=1和meetingCaller的转换,所以手机端设置两个caller组件
        Map itemCallerHrmBrowser = new HashMap();
        for (String groupid : groupList) {
            String grouplabel = SystemEnv.getHtmlLabelName(Util.getIntValue(meetingFieldGroupComInfo.getLabel(groupid)), user.getLanguage());
            if(!meetingFieldGroupComInfo.getShow(groupid).equals("1")){
                continue;
            }
            Map<String, Object> groupitem = new HashMap<String, Object>();
            List<Object> itemlist = new ArrayList<Object>();
            fieldList = hfm.getUseField(groupid);
            if(groupid.equals("1") && viewMeeting && requestid > 0){// 在流程生成会议的情况下在基本信息最下面添加审批流程
                fieldList.add("-16"); // 添加的虚拟字段fieldid根据对应的浏览按钮type决定
            }
            if (fieldList != null && fieldList.size() > 0) {
                groupitem.put("title", grouplabel);
                groupitem.put("defaultshow", true);
                for (String fieldid : fieldList) {
                    if (isInterval) {//周期会议
                        if ("0".equals(meetingFieldComInfo.getIsrepeat(fieldid))) continue;
                    } else {//非周期会议
                        if ("1".equals(meetingFieldComInfo.getIsrepeat(fieldid))) continue;
                    }
                    String fieldname = "";
                    String fieldlabel = "";
                    int fieldhtmltype = 0;
                    String fielddbtype = "";
                    String fieldShowTypes = "";
                    int fieldtype = 3;
                    if(fieldid.equals("-16")){
                        fieldname = "requestid";
                        fieldlabel = "15058";
                        fieldhtmltype = 3;
                        fielddbtype = "int";
                        fieldShowTypes = "";
                        fieldtype = 16;
                        params.put("requestid",requestid);
                    }else{
                        fieldname = meetingFieldComInfo.getFieldname(fieldid);
                        fieldlabel = meetingFieldComInfo.getLabel(fieldid);
                        fieldhtmltype = Integer.parseInt(meetingFieldComInfo.getFieldhtmltype(fieldid));
                        fielddbtype = meetingFieldComInfo.getFielddbtype(fieldid);
                        fieldShowTypes = meetingFieldComInfo.getFieldShowTypes(fieldid);
                        fieldtype = Util.getIntValue(meetingFieldComInfo.getFieldType(fieldid));
                    }
                    if ("remindHoursBeforeStart".equals(fieldname) || "remindTimesBeforeStart".equals(fieldname) || "remindHoursBeforeEnd".equals(fieldname) || "remindTimesBeforeEnd".equals(fieldname)) {
                        continue;
                    }

                    String fieldValue = "";
                    List listValue = new ArrayList();
                    if (params.keySet().contains(fieldname)) {
                        fieldValue = Util.null2String(params.get(fieldname));
                    }
                    if (flag) {
                        fieldValue = Util.null2String(meetingRs.getString(fieldname));
                        if(fieldname.equals("begindate") ||
                                fieldname.equals("repeatbegindate") ||
                                fieldname.equals("begintime") ||
                                fieldname.equals("enddate") ||
                                fieldname.equals("repeatenddate") ||
                                fieldname.equals("endtime")){
                            fieldValue = Util.null2String(params.get(fieldname));
                        }
                        //会议室需要考虑到是否封存的情况，所以单独处理
                        if(fieldname.equals("address")){
                            fieldValue = address;
                        }
                    }

                    //新规则，当前时间，超过30，取后面一个点整点，未超过30，取当前整点的30分。
                    //特殊，当前时间为23.30以前，那么取30-59分。如果为以后，取当前时间-59分。不存在跨天数据即可。
                    String timest = MeetingUtil.getInitialTime();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    Calendar cal = Calendar.getInstance();
                    try {
                        cal.setTime(sdf.parse(timest));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    cal.add(Calendar.HOUR, 1);// 24小时制
                    Date timeed = cal.getTime();
                    String endst = sdf.format(timeed);
                    //如果开始时间取的是23.30分，那么说明，当前时间为23点钟，未过30分，那么去30-59分。
                    if("23:30".equals(timest)){
                        endst = "23:59";
                    }
                    //如果说开始时间取得是00.00分，那么说明，当前时间为23点钟，过了30分，那么取当前时间-59分。
                    if("00:00".equals(timest)){
                        timest = sdf.format(new Date());
                        endst = "23:59";
                    }
                    if(fieldname.equals("begintime") && StringUtils.isBlank(fieldValue)){
                        fieldValue = Util.null2String(timest);
                    }
                    if(fieldname.equals("endtime") && StringUtils.isBlank(fieldValue)){
                        fieldValue = Util.null2String(endst);
                    }
                    item = new HashMap();

                    int viewAttr = 2; //1只读 2可编辑 3 必填
                    if (viewMeeting) {
                        viewAttr = 1;
                        if ("name".equals(fieldname)) {
                            String extendHtml = null;
                            try {
                                extendHtml = new MeetingTransMethod().getMeetingStatus4User(meetingstatus, user.getLanguage() + "+" + enddate + "+" + endtime + "+" + isdecision + "+" + repeatenddate + "+" + repeatType + "+" + requestid,user);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            fieldValue += "&nbsp;&nbsp;&nbsp;(" + extendHtml + ")";
                        }

                    } else if ("1".equals(meetingFieldComInfo.getIsmand(fieldid))) {
                        viewAttr = 3;
                    }
                    if (isChange) {
                        viewAttr = 1;
                        String customFields = Util.null2String(params.get("customFields"));
                        /*String editFieldId = ",5,6,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,"+customFields+","+hrmColumns+",";*/

                        //==zj==修改会议表单控件是否可修改
                        String editFieldId = ",5,6,7,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,35,"+customFields+","+hrmColumns+",";
                        if (editFieldId.indexOf("," + fieldid + ",") > -1) {
                            if ("1".equals(meetingFieldComInfo.getIsmand(fieldid))) {
                                viewAttr = 3;
                            } else {
                                viewAttr = 2;
                            }
                        }
                    }

                    String isIntervalField = ",9,10,11,12,13,";
                    //针对周期会议9,10,12,13设置默认值以及min
                    boolean isNumSelect = false;
                    if (isIntervalField.indexOf("," + fieldid + ",") > -1) {
                        isNumSelect = true;
                        if (viewMeeting && !isChange) {
                            viewAttr = 1;
                        } else {
                            viewAttr = 2;
                        }
                    }

                    if (fieldhtmltype == 1) {
                        if (fieldtype == 1) {
                            item = FieldUtil.getFormItemForInput(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr);
                        } else if (fieldtype == 2) {
                            item = FieldUtil.getFormItemForInt(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr);
                            if (isNumSelect) {
                                if (Util.getIntValue(fieldValue) < 0) {
                                    item.put("value", "1");
                                }
                                item.put("min", "1");
                            }
                            //作用:新建会议默认将创建人员带出当做参会人员,所以totalmember=1
                            if (!flag && fieldname.equals("totalmember")) {
                                //新建的情况下根据参会人员来设置
                                if(params.containsKey("hrmmembers")){
                                    String valueTemp = Util.null2String(params.get("hrmmembers")) ;
                                    String valueTempArr[] = valueTemp.split(",");
                                    if(valueTempArr.length > 0){
                                        item.put("value", valueTempArr.length);
                                    }else{
                                        item.put("value", 0);
                                    }
                                }else{
                                    item.put("value", 0);
                                }
                            }
                            if (!flag && fieldname.equals("crmtotalmember")) {
                                //新建的情况下根据参会人员来设置
                                if(params.containsKey("crmmembers")){
                                    String valueTemp = Util.null2String(params.get("crmmembers")) ;
                                    String valueTempArr[] = valueTemp.split(",");
                                    if(valueTempArr.length > 0){
                                        item.put("value", valueTempArr.length);
                                    }else{
                                        item.put("value", 0);
                                    }
                                }else{
                                    item.put("value", 0);
                                }
                            }
                            if (fieldname.equals("totalmember") || fieldname.equals("crmtotalmember")) {
                                item.put("min", "0");
                            }
                        } else if (fieldtype == 3) {
                            item = FieldUtil.getFormItemForFloat(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr, Util.getIntValue(fielddbtype.substring(fielddbtype.indexOf(",") + 1, fielddbtype.length() - 1), 1));
                        }
                    } else if (fieldhtmltype == 2) {
                        //兼容E8数据
                        fieldValue = Util.StringReplace(fieldValue, "<br>", "" + '\n');
                        fieldValue = Util.StringReplace(fieldValue, "</p>", "" + '\n');
                        fieldValue = fieldValue.replaceAll("&nbsp;"," ");
                        if(fieldtype == 2){
//                            if(ismobile){
//                                item = FieldUtil.getFormItemForRichText(GovernCommonUtils.richTextConverterToPage(user,fieldValue), user.getLanguage(), viewAttr, fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()));
//                            }else{
//                                item = FieldUtil.getFormItemForRichText(fieldValue, user.getLanguage(), viewAttr, fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()));
//                            }
                            item = FieldUtil.getFormItemForRichText(fieldValue, user.getLanguage(), viewAttr, fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()));
                            List leftConfigList = new ArrayList();
                            Map leftConfigMap = new HashMap();
                            leftConfigMap.put("leftConfig",leftConfigList);

                            Map replyPropsMap = new HashMap();
                            replyPropsMap.put("replyProps",leftConfigMap);
                            Map richTextPropsMap = new HashMap();
                            richTextPropsMap.put("richTextProps",replyPropsMap);
                            item.put("otherParams",richTextPropsMap);
                            item.put("startupFocus",false);
                        }else{
                            item = FieldUtil.getFormItemForTextArea(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr);

                        }
                    } else if (fieldhtmltype == 3) {
                        if (fieldtype == 9 || fieldtype == 37 || fieldtype == 16 || fieldtype == 152 || fieldtype == 28) {
                            baseBrowserColumn.add(fieldname);
                        }
                        if(fieldtype == 2){
                            item = FieldUtil.getFormItemForDate(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr);
                        } else if(fieldtype == 19){
                            item = FieldUtil.getFormItemForTime(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr);
                        } else if(fieldtype == 402){
                            fieldValue = Util.getIntValue(fieldValue,-1)>0? fieldValue:"";
                            item = FieldUtil.getFormItemForYear(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr,ismobile);
                        } else if(fieldtype == 403){
                            item = FieldUtil.getFormItemForYearAndMonth(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr,ismobile);
                        } else if(fieldtype == 290){
                            item = FieldUtil.getFormItemForDate(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr, true);
                        } else {
                            if (fieldtype == 269 && fieldValue.isEmpty()) {
                                RecordSet rs = new RecordSet();
                                rs.executeQuery("select * from meeting_remind_type where isuse=? ", "1");
                                if (rs.getCounts() < 1) {
                                    continue;
                                }

                            }
                            if ("address".equals(fieldname) && !address.isEmpty()) {
                                fieldValue = address;
                            }
                            SearchConditionItem sci = conditionFactory.createCondition(ConditionType.BROWSER, fieldlabel, fieldname, fieldtype + "");
                            sci.getBrowserConditionParam().setHideAdvanceSearch(false);
                            if (fieldtype == 9 || fieldtype == 37) {
                                String linkUrl = sci.getBrowserConditionParam().getLinkUrl();
                                if (linkUrl != null && !linkUrl.equals("")) {
                                    sci.getBrowserConditionParam().setLinkUrl("/docs/docs/DocDsp.jsp?meetingid=" + meetingid + "&id=");
                                }

                            }
                            if (fieldtype == 161 || fieldtype == 162) {
                                BrowserInitUtil browserInitUtil = new BrowserInitUtil();
                                BrowserBean browserProp = new BrowserBean(fieldtype + "");
                                if (fieldtype == 161) {
                                    browserInitUtil.initCustomizeBrow(browserProp, fielddbtype, 161, user.getLanguage());
                                } else if (fieldtype == 162) {
                                    browserInitUtil.initCustomizeBrow(browserProp, fielddbtype, 162, user.getLanguage());
                                }
                                sci.setBrowserConditionParam(browserProp);
                            }

                            if (fieldtype == 152 || fieldtype == 16) {
                                String linkUrl = sci.getBrowserConditionParam().getLinkUrl();
                                if (linkUrl != null && !linkUrl.equals("")) {
                                    sci.getBrowserConditionParam().setLinkUrl("/workflow/request/ViewRequestForwardSPA.jsp?fromModul=meeting&modulResourceId=" + meetingid + "&isrequest=1&requestid=");
                                }

                            }
                            if (fieldtype == 256 || fieldtype == 257) {
                                // 暂不支持自定义数型单选多选
                                sci = conditionFactory.createCondition(ConditionType.BROWSER, fieldlabel, fieldname, fieldtype + "");
                                Map<String, Object> otherParams = new HashMap<String,Object>();
                                otherParams.put("detailtype","3");
                                otherParams.put("supportCancel",true);
                                otherParams.put("quickSearch",true);
                                otherParams = new HashMap<String,Object>();
                                int dbtype = Util.getIntValue(fielddbtype);
                                Map<String, Object> dataParams = new HashMap<String,Object>();
                                dataParams.put("cube_treeid",fielddbtype+"_searchType");
                                otherParams.put("completeParams",dataParams);
                                CubeFieldService cubeFieldService=new CubeFieldService();

                                sci.setOtherParams(otherParams);
                                BrowserBean browserConditionParam = sci.getBrowserConditionParam();
                                browserConditionParam.getDataParams().put("cube_treeid", dbtype);
                                browserConditionParam.setType(fieldtype+"");
                                browserConditionParam.setTitle(fieldname);
                                sci.setLabel(fieldlabel);
                                new BrowserInitUtil().initBrowser(browserConditionParam,user.getLanguage());
                                browserConditionParam.getDataParams().put("cube_treeid", dbtype+"_searchType");
                                browserConditionParam.getCompleteParams().put("cube_treeid", dbtype+"_searchType");
                                sci.setBrowserConditionParam(browserConditionParam);
                                sci.setViewAttr(viewAttr);
                                if(fieldtype == 257){
                                    sci.getBrowserConditionParam().setIsSingle(false);
                                }
                            }
                            if (!fieldValue.isEmpty()) {
                                String[] arr = fieldValue.split(",");
                                //每周几的字段做特殊处理,weile
                                if (fieldtype == 268) {
                                    Arrays.sort(arr);
                                }
                                for (int i = 0; i < arr.length; i++) {
                                    if (arr[i].isEmpty()) {
                                        continue;
                                    }
                                    if (fieldtype == 1 || fieldtype == 17) {
                                        if(arr[i].equals("0") || !MeetingUtil.right4Resource(user,arr[i]+"")){
                                            continue;
                                        }
                                    }
                                    String showname = null;
                                    try {
                                        showname = hfm.getFieldvalue(user, Util.getIntValue(fieldid), fieldhtmltype, fieldtype, arr[i], 0, fielddbtype);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    Map tmp = new HashMap();
                                    if (showname.isEmpty() && !"0".equals(arr[i])) {//显示名为空，取id值
                                        showname = arr[i];
                                    }
                                    if (!showname.isEmpty()) {//显示名和id都不为空才组装返回值
                                        tmp.put("id", arr[i]);
                                        tmp.put("name", showname);
                                    }
                                    listValue.add(tmp);
                                }
                            }
//                            if (fieldname.equals("contacter") && listValue.size() == 0 && fieldValue.isEmpty()&&!params.containsKey(fieldname)) {
//                                Map tmp = new HashMap();
//                                tmp.put("id", "" + user.getUID());
//                                tmp.put("name", user.getLastname());
//                                listValue.add(tmp);
//                            }
                            if (fieldname.equals("caller") && !viewMeeting) {
                                sci.getBrowserConditionParam().setType("meetingCaller");
                            }
                            if (fieldname.equals("meetingtype") && fieldValue.isEmpty()) {
                                Map params = new HashMap<String, Object>();
                                params.put("isInterval", "0");
                                sci.getBrowserConditionParam().setDataParams(params);
                            }
                            if (isEdit && HrmUserVarify.checkUserRight("MeetingType:Maintenance", user)) {
                                if (fieldname.equals("meetingtype")) {
                                    sci.getBrowserConditionParam().setHasAddBtn(true);
                                    sci.getBrowserConditionParam().setAddOnClick("()=>{window.open('/meeting/Maint/MeetingTypeAdd.jsp?dialog=1')}");
                                } else if (fieldname.equals("address")) {
                                    sci.getBrowserConditionParam().setHasAddBtn(true);
                                    sci.getBrowserConditionParam().setAddOnClick("()=>{window.open('/meeting/Maint/MeetingRoomAddTab.jsp?dialog=1&fromN=1')}");
                                }
                            }
                            sci.getBrowserConditionParam().setReplaceDatas(listValue);
                            sci.getBrowserConditionParam().setViewAttr(viewAttr);
                            if (fieldname.equals("caller")) {
                                List listValue_caller = new ArrayList();
                                //新建的时候给默认值当前人员
//                                //兼容二开赋空值
//                                if (!flag && fieldValue.isEmpty()&&!params.containsKey(fieldname)) {
//                                    fieldValue = user.getUID() + "";
//                                }

                                if (!fieldValue.isEmpty()) {
                                    String[] arr = fieldValue.split(",");
                                    for (int i = 0; i < arr.length; i++) {
                                        if (arr[i].isEmpty()) {
                                            continue;
                                        }
                                        String showname = null;
                                        try {
                                            showname = hfm.getFieldvalue(user, Util.getIntValue(fieldid), fieldhtmltype, fieldtype, arr[i], 0, fielddbtype);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        Map tmp = new HashMap();
                                        if (showname.isEmpty() && !"0".equals(arr[i])) {//显示名为空，取id值
                                            showname = arr[i];
                                        }
                                        if (!showname.isEmpty()) {//显示名和id都不为空才组装返回值
                                            tmp.put("id", arr[i]);
                                            tmp.put("name", showname);
                                        }
                                        listValue_caller.add(tmp);
                                    }
                                }
                                SearchConditionItem sci_hrm = conditionFactory.createCondition(ConditionType.BROWSER, fieldlabel, fieldname, fieldtype + "");
                                sci_hrm.getBrowserConditionParam().setReplaceDatas(listValue_caller);
                                sci_hrm.getBrowserConditionParam().setViewAttr(viewAttr);
                                itemCallerHrmBrowser = FieldUtil.getFormItemForBrowser(sci_hrm, "caller_hrm", SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldtype + "", fieldValue);
                            }
//                            if (fieldname.equals("contacter") && fieldValue.isEmpty()) {
//                                //新建的时候给默认值当前人员
//                                if (!flag&&!params.containsKey(fieldname)) {
//                                    fieldValue = user.getUID() + "";
//                                }
//                            }
//                            if (fieldname.equals("hrmmembers") && fieldValue.isEmpty()) {
//                                if (!flag&&!params.containsKey(fieldname)) {
//                                    fieldValue = user.getUID() + "";
//                                }
//                            }
                            if (isChange) {
                                sci.getBrowserConditionParam().setHasBorder(true);
                            }
                            if (fieldname.equals("remindTypeNew") && fieldValue.equals("") && viewMeeting) {
                                item = FieldUtil.getFormItemForInput(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), SystemEnv.getHtmlLabelName(19782, user.getLanguage()), viewAttr);
                            } else if(fieldname.equals("remindTypeNew") && viewMeeting && ismobile && !fieldValue.equals("")){
                                RecordSet recordSet = new RecordSet();
                                Map meetingRemind=new HashMap();
                                List idParamValueList=DBUtil.trasToList(fieldValue);
                                recordSet.executeQuery("SELECT * FROM meeting_remind_type where id in ("+DBUtil.getParamReplace(fieldValue)+")",idParamValueList);
                                String tempValue = "";
                                while(recordSet.next()){
                                    tempValue += tempValue.equals("")?recordSet.getString("label").equals("")?recordSet.getString("name")
                                            :SystemEnv.getHtmlLabelName(recordSet.getInt("label"),user.getLanguage()):
                                    ","+(recordSet.getString("label").equals("")?recordSet.getString("name")
                                            :SystemEnv.getHtmlLabelName(recordSet.getInt("label"),user.getLanguage()));
                                }
                                item = FieldUtil.getFormItemForInput(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), tempValue, viewAttr);
                            }else if(fieldname.equals("remindTypeNew") && !viewMeeting){
                                //将提醒方式修改为多选checkbox形式
                                Map<String,String>  reminds = MeetingBrowser.getRemindMap();
                                Iterator<String> it =reminds.keySet().iterator();
                                List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
                                while(it.hasNext()){
                                    String id = it.next();
                                    String name  = MeetingBrowser.getRemindName(Util.getIntValue(id),user.getLanguage());
                                    options.add(new SearchConditionOption(id,name));
                                }
                                item = FieldUtil.getFormItemForSelect(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr,2, options);
                            }else {
                                item = FieldUtil.getFormItemForBrowser(user,sci, fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldtype + "", fieldValue,viewAttr);
                            }

                            if (fieldname.equals("address")) {
                                item.put("fieldcol", 15);
                            }
                        }
                    } else if (fieldhtmltype == 4) {
                        if ("remindImmediately".equals(fieldname)) {
                            item = FieldUtil.getFormItemForSwitch(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue.isEmpty() ? "0" : fieldValue, viewAttr);
                        } else if ("remindBeforeStart".equals(fieldname)) {
                            fieldname += ",remindHoursBeforeStart,remindTimesBeforeStart";
                            listValue.add(fieldValue.isEmpty() ? "0" : fieldValue);
                            if (meetingid.isEmpty()) {
                                listValue.add(Util.null2String(params.get("remindHoursBeforeStart")).equals("")?"0":Util.null2String(params.get("remindHoursBeforeStart")));
                                listValue.add(Util.null2String(params.get("remindTimesBeforeStart")).equals("")?"0":Util.null2String(params.get("remindTimesBeforeStart")));
                            } else {
                                listValue.add(Util.null2String(meetingRs.getString("remindHoursBeforeStart"), "0"));
                                listValue.add(Util.null2String(meetingRs.getString("remindTimesBeforeStart"), "0"));
                            }
                            item = FieldUtil.getRemindItem(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), listValue, viewAttr);
                        } else if ("remindBeforeEnd".equals(fieldname)) {
                            fieldname += ",remindHoursBeforeEnd,remindTimesBeforeEnd";
                            listValue.add(fieldValue.isEmpty() ? "0" : fieldValue);
                            if (meetingid.isEmpty()) {
                                listValue.add(Util.null2String(params.get("remindHoursBeforeEnd")).equals("")?"0":Util.null2String(params.get("remindHoursBeforeEnd")));
                                listValue.add(Util.null2String(params.get("remindTimesBeforeEnd")).equals("")?"0":Util.null2String(params.get("remindTimesBeforeEnd")));
                            } else {
                                listValue.add(Util.null2String(meetingRs.getString("remindHoursBeforeEnd"), "0"));
                                listValue.add(Util.null2String(meetingRs.getString("remindTimesBeforeEnd"), "0"));
                            }
                            item = FieldUtil.getRemindItem(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), listValue, viewAttr);
                        } else {
                            item = FieldUtil.getFormItemForCheckbox(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr);
                        }
                    } else if (fieldhtmltype == 5) {
                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
                        String tempValue = "";
                        if((viewMeeting || isChange) && ismobile ){
                            if (fieldname.equals("secretLevel")) {
                                HrmClassifiedProtectionBiz hrmClassifiedProtectionBiz = new HrmClassifiedProtectionBiz();
                                //是否启用分级保护
                                boolean isOpenSecret = HrmClassifiedProtectionBiz.isOpenClassification();
                                if (isOpenSecret) {
                                    //根据人员id查询可以选择的密级级别预留接口
                                    //(废弃)会议查看的时候有一种情况那么就是监控人员级别很低但是会议资源很高的情况下,他也是可以查看的,,那么查看的情况下就要取得所有资源密级
                                    //692235中监控人如果没有权限也不能查看
                                    options = hrmClassifiedProtectionBiz.getResourceOptionListByUser(user);
                                } else {
                                    continue;
                                }

//                                if (fieldValue.isEmpty()) {
//                                    //默认选中内部级别
//                                    fieldValue = MeetingUtil.DEFAULT_SECRET_LEVEL;
//                                }
                                if (StringUtils.isBlank(tempValue)){
                                    tempValue = Util.null2String(hrmClassifiedProtectionBiz.getResourceSecLevelShowName(fieldValue, Util.null2String(user.getLanguage())));
                                }
                            }else{
                                RecordSet rs = new RecordSet();
                                rs.executeQuery("select selectlabel,selectvalue,selectname,isdefault from meeting_selectitem  where fieldid = ? order by listorder,id", fieldid);
                                while (rs.next()) {
                                    String tmpselectvalue = Util.null2String(rs.getString("selectvalue"));
                                    String selectlabel = Util.null2String(rs.getString("selectlabel"));
                                    String isDefault = Util.null2String(rs.getString("isdefault"));
                                    String tmpselectname = "";
                                    if (!"".equals(selectlabel)) {
                                        tmpselectname = SystemEnv.getHtmlLabelName(Util.getIntValue(selectlabel), user.getLanguage());
                                    } else {
                                        tmpselectname = Util.toScreen(rs.getString("selectname"), user.getLanguage());
                                    }
                                    if ((","+fieldValue+",").indexOf(","+tmpselectvalue+",") > -1) {
                                        tempValue += tempValue.equals("")?tmpselectname:","+tmpselectname;
                                    }
                                }
                            }
                            item = FieldUtil.getFormItemForInput(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), tempValue, viewAttr);
                        }else{
                            //密级字段单独处理,因为要根据人力资源那边来取option数据
                            if (fieldname.equals("secretLevel")) {
                                HrmClassifiedProtectionBiz hrmClassifiedProtectionBiz = new HrmClassifiedProtectionBiz();
                                //是否启用分级保护
                                boolean isOpenSecret = HrmClassifiedProtectionBiz.isOpenClassification();
                                if (isOpenSecret) {
                                    //根据人员id查询可以选择的密级级别预留接口
                                    //(废弃)会议查看的时候有一种情况那么就是监控人员级别很低但是会议资源很高的情况下,他也是可以查看的,,那么查看的情况下就要取得所有资源密级
                                    //692235中监控人如果没有权限也不能查看
                                    options = hrmClassifiedProtectionBiz.getResourceOptionListByUser(user);

                                } else {
                                    continue;
                                }
//                                if (fieldValue.isEmpty()) {
//                                    //默认选中内部级别
//                                    fieldValue = MeetingUtil.DEFAULT_SECRET_LEVEL;
//                                }
                            } else {
                                RecordSet rs = new RecordSet();
                                rs.executeQuery("select selectlabel,selectvalue,selectname,isdefault from meeting_selectitem where fieldid = ?  order by listorder,id", fieldid);
                                String defaultValues = "";
                                while (rs.next()) {
                                    String tmpselectvalue = Util.null2String(rs.getString("selectvalue"));
                                    String selectlabel = Util.null2String(rs.getString("selectlabel"));
                                    String isDefault = Util.null2String(rs.getString("isdefault"));
                                    String tmpselectname = "";
                                    if (!"".equals(selectlabel)) {
                                        tmpselectname = SystemEnv.getHtmlLabelName(Util.getIntValue(selectlabel), user.getLanguage());
                                    } else {
                                        tmpselectname = Util.toScreen(rs.getString("selectname"), user.getLanguage());
                                    }
                                    if (fieldValue.isEmpty() && isDefault.equals("y")) {
                                        defaultValues += defaultValues.equals("")?tmpselectvalue:","+tmpselectvalue;
                                    }
                                    options.add(new SearchConditionOption(tmpselectvalue, tmpselectname));
                                }
                                //修改逻辑：只读可编辑状态下，都加上一个空选项
                                options.add(0,new SearchConditionOption("", ""));

                                if(fieldValue.equals("") && meetingid.equals("")){//新建的情况下
                                    fieldValue = defaultValues;
                                }
                            }
                            int detailtype = 1;
                            if(fieldtype == 1){
                                detailtype = 1;
                            }else if(fieldtype == 2 ){
                                detailtype = 2;
                            }else if(fieldtype == 3 ){
                                detailtype = 3;
                            }
                            item = FieldUtil.getFormItemForSelect(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, viewAttr,detailtype, options);
                            item.put("fieldshowtypes",fieldShowTypes);
                        }
                    } else if (fieldhtmltype == 6) {
                        baseUploadColumn.add(fieldname);
                        if(fieldtype == 2){
                            item = MeetingUtil.getCommonAccessoryInfo(user,meetingid,meetingtype,fieldname,fieldlabel,viewAttr,fieldValue,ismobile,"","jpg,gif,png,jpeg","img");
                        }else{
                            item = MeetingUtil.getCommonAccessoryInfo(user,meetingid,meetingtype,fieldname,fieldlabel,viewAttr,fieldValue,ismobile,"");
                        }
                    } else if (fieldhtmltype == 7) {
                        RecordSet rs = new RecordSet();
                        rs.executeQuery("select * from meeting_specialfield where fieldid = ?",fieldid);
                        String href = "";
                        String hrefDesc = "";
                        String desc = "";
                        if(rs.next()){
                            href = rs.getString("linkaddress");
                            hrefDesc = rs.getString("displayname");
                            desc = rs.getString("descriptivetext");
                        }
                        if(fieldtype == 1){
                            fieldValue = "<a href="+href+" target=\"_blank\">"+hrefDesc+"</a>";
                            item = FieldUtil.getFormItemForInput(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, 1);
                        }else if(fieldtype == 2){
                            fieldValue = desc;
                            item = FieldUtil.getFormItemForTextArea(fieldname, SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()), fieldValue, 1);
                        }

//                        item = MeetingUtil.getCommonAccessoryInfo(user,meetingid,meetingtype,fieldname,fieldlabel,viewAttr,fieldValue,ismobile,"");
                    }
                    if (viewAttr == 3 && !"repeatdays".equals(fieldname) &&
                            !"repeatweeks".equals(fieldname) && !"rptWeekDays".equals(fieldname) && !"repeatmonths".equals(fieldname) && !"repeatmonthdays".equals(fieldname)) {
                        item.put("rules", "required");
                    }
                    if (isChange) {
                        item.put("hasBorder", true);
                    }
                    if (item.containsKey("browserType") && item.get("browserType").equals("meetingCaller") && !viewMeeting
                            && ismobile && itemCallerHrmBrowser.size() > 0) {
                        itemlist.add(itemCallerHrmBrowser);
                    }
                    itemlist.add(item);
                }
                groupitem.put("items", itemlist);
            }
            if (groupitem.size() > 0) {
                grouplist.add(groupitem);
            }

        }
        int index = 0;
        int typeIndex = 0;
        int basesize = 0;
        for(int n=0;n<grouplist.size();n++){
            Map<String,Object> dataTemp = grouplist.get(n);
            List<Map> items = (List<Map>)dataTemp.get("items");
            for(int i=0;i<items.size();i++){
                Map<String, Object> info = (Map<String, Object>)items.get(i);
                String domkey = "";
                if(info.get("domkey") instanceof String){
                    domkey =(String)info.get("domkey");
                }else if(info.get("domkey") instanceof List){
                    List<String> domkeyArr = (List<String>)info.get("domkey");
                    domkey = domkeyArr.get(0);
                }
                if("secretLevel".equals(domkey)){
                    basesize = items.size();
                    index = i;
                    typeIndex = n;
                    break;
                }
            }
        }
        String secretLevelType = "";
        if(HrmClassifiedProtectionBiz.isOpenClassification()){
            //判断密级展示方式
            if(!"".equals(secretLevel)){
                secretLevelType = HrmClassifiedProtectionBiz.getResourceClassificationValidityDefaultValue(secretLevel);
            }
            if("".equals(secretLevelType)){
                secretLevelType = "0";
            }

            //增加保密期限字段
            int viewAttrTemp = 2;
            Map<String,Object> map = grouplist.get(typeIndex);
            List<Map> item1 = (List<Map>)map.get("items");
            if (viewMeeting || isChange) {
                viewAttrTemp = 1;
                if(basesize == index){
                    item1.add( FieldUtil.getFormItemForInput("secretDeadline", SystemEnv.getHtmlLabelName(131188, user.getLanguage()), secretDeadline, viewAttrTemp));
                    HrmClassifiedProtectionBiz hrmClassifiedProtection = new HrmClassifiedProtectionBiz();
                    String secretLevelView = hrmClassifiedProtection.getResourceSecLevelShowName(secretLevel,user.getLanguage()+"");
                    item1.add( FieldUtil.getFormItemForInput("secretLevelView", SystemEnv.getHtmlLabelName(500520, user.getLanguage()), secretLevelView, viewAttrTemp));
                }else{
                    item1.add(index+1, FieldUtil.getFormItemForInput("secretDeadline", SystemEnv.getHtmlLabelName(131188, user.getLanguage()), secretDeadline, viewAttrTemp));
                    HrmClassifiedProtectionBiz hrmClassifiedProtection = new HrmClassifiedProtectionBiz();
                    String secretLevelView = hrmClassifiedProtection.getResourceSecLevelShowName(secretLevel,user.getLanguage()+"");
                    item1.add(index+2, FieldUtil.getFormItemForInput("secretLevelView", SystemEnv.getHtmlLabelName(500520, user.getLanguage()), secretLevelView, viewAttrTemp));
                }
            }else{
                viewAttrTemp = 2;
                if(basesize == index){
                    item1.add( FieldUtil.getFormItemForInput("secretDeadline", SystemEnv.getHtmlLabelName(131188, user.getLanguage()), secretDeadline, viewAttrTemp));
                }else{
                    item1.add(index+1, FieldUtil.getFormItemForInput("secretDeadline", SystemEnv.getHtmlLabelName(131188, user.getLanguage()), secretDeadline, viewAttrTemp));
                }
            }

            //保密期限
            Map secretDeadlineMap = new HashMap();
            secretDeadlineMap = FieldUtil.getFormItemForInput("secretDeadline", SystemEnv.getHtmlLabelName(131188, user.getLanguage()), secretDeadline, viewAttrTemp);
            secretDeadlineMap.put("colSpan", 1);
            secretDeadlineMap.put("needHide", true);
            secretDeadlineMap.put("linkageColumn", "secretLevel");
            hideFieldMap.put("secretDeadline", secretDeadlineMap);
            hideColumnlist.add("secretDeadline");
        }

        ret.put("datas", grouplist);
        ret.put("meetingstatus", meetingstatus);
        boolean isfromchatshare = Util.null2String(params.get("isfromchatshare")).equals("1")?true:false;
        int share = Util.getIntValue((String)params.get("sharer"));
        if(!isfromchatshare && meetingstatus.equals("2") && !isInterval){
            Map msgInfoMap = new HashMap();
            List msgInfoList = new ArrayList();
            msgInfoMap.put("sharetitle",meetingname);
            msgInfoMap.put("linkurl","/common/chatResource/view.jsp?resourcetype=28&resourceid="+meetingid+"&isfromchatshare=1&sharer="+user.getUID()+"&firstSharer="+user.getUID());
            msgInfoMap.put("objectName","FW:CustomShareMsg");
            msgInfoMap.put("canforward",0);
            msgInfoMap.put("callbackurl","/api/common/chatResource/addshare?resourcetype=28&resourceid="+user.getUID()+"&isfromchatshare=1&firstSharer="+user.getUID());
            msgInfoMap.put("sharetype","meeting");
            msgInfoMap.put("shareid",meetingid);
            msgInfoMap.put("opentype",1);
            msgInfoMap.put("opentype_pc",2);
            msgInfoList.add(msgInfoMap);
            ret.put("msgInfo",msgInfoList);
        }
        if (HrmClassifiedProtectionBiz.isOpenClassification()) {
            ret.put("secretLevel", secretLevel);
        } else {
            ret.put("secretLevel", "");
        }
        ret.put("secretLevelType", secretLevelType);
        ret.put("isOpenSecret", HrmClassifiedProtectionBiz.isOpenClassification());
        ret.put("hideField", hideFieldMap);
        ret.put("hideColumn", hideColumnlist);
        ret.put("baseDocFlowColumn",baseBrowserColumn);
        ret.put("baseUploadColumn",baseUploadColumn);

        if (isInterval) {
            int repeatMaxLength = meetingSetInfo.getZqhyzdkd();
            if (repeatMaxLength > 0) {
                Calendar today = Calendar.getInstance();
                String nowdate = Util.add0(today.get(Calendar.YEAR), 4) + "-" +
                        Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                        Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
                ret.put("maxRepeatDate", TimeUtil.dateAdd(nowdate, repeatMaxLength));
            } else {
                ret.put("maxRepeatDate", "");
            }

        }
        MeetingUtil meetingUtil = new MeetingUtil();
        List btnArrs = meetingUtil.getBtnArr(meetingid,user,mpu,ismobile);
        if (ismobile) {
            btnArrs.remove("exportExcel");
            btnArrs.remove("doViewLog");
        }
        //不支持多级分享
        if(isfromchatshare){
            btnArrs.remove("doChatShare");
            btnArrs.remove("doEncryptChatShare");
        }
        ret.put("btns", btnArrs);
        //决议督办
        GovernActionService governActionService = new GovernActionServiceImpl();
        if(("2".equals(meetingstatus )) && (mpu.getIsmanager()) && "0".equals(repeatType) && governActionService.isOpenMeeting("8")||true)
        {
            ret.put("decisionToGovernBtn",true);
        }
        int roomconflictchk = new MeetingSetInfo().getRoomConflictChk();

        //PC端代表是有应用设置权限的人员
        ret.put("canset", HrmUserVarify.checkUserRight("meetingmanager:all", user));

        //mobile端专用,如果会议类型存在那么就需要判断是否有召集人,来决定手机前端是需要显示meetingCaller或者1的浏览按钮
        if (meetingtype.equals("")) {
            ret.put("hasCaller", false);
        } else {
            try {
                ret.put("hasCaller", getHasCallerByType(meetingtype));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ret.put("roomcheck", roomconflictchk);
        if (!meetingid.isEmpty() && !ismobile) {
            Map topic = null;
            Map service = null;
            try {
                topic = getMeetingTopicField(meetingid, user, true, mpu, ismobile);
                service = getMeetingServiceField(meetingid, user, ismobile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ret.put("topic", topic);
            ret.put("service", service);
        } else if (!meetingid.isEmpty() && ismobile) {
            //在手机端查看详细时如果是普通参会人员那么就需要判断是否可以直接提交回执
            Map showReceiptBtn = showReceiptBtnForMobile(meetingid, begindate, begintime, user, isdecision,meetingstatus);
            ret.put("showReceiptBtn", showReceiptBtn);
            if (isChange) {
                Map ret1 = new HashMap();
                ret1.put("base", true);
                ret.put("tabs", ret1);
            }else{
                ret.put("tabs", meetingUtil.getShowTab(meetingid, user, mpu,ismobile));
            }
        } else if (meetingid.isEmpty() && ismobile) {
            ret.put("tabs", meetingUtil.getShowTab("", user, mpu,ismobile));
        }
        //获得api结束时间
        long afterTime = new Date().getTime();
        ret.put("apiCost", afterTime - datetime);
        ret.put("meetingname", meetingname);
        ret.put("openAccess", new BaseBean().getPropValue("meeting_accessTemp", "open"));
        ret.put("accessoryBaseInfo", MeetingUtil.getAccessoryBaseInfo(user, meetingid, meetingtype));

        //新建流程带出参数所用
        ret.put("beginDate", params.keySet().contains("begindate")?params.get("begindate"):!meetingDate.isEmpty()?meetingDate:"");
        ret.put("endDate", params.keySet().contains("enddate")?params.get("enddate"):!meetingDate.isEmpty()?meetingDate:"");
        ret.put("beginTime", params.keySet().contains("begintime")?params.get("begintime"):Util.add0(meetingSetInfo.getTimeRangeStart(), 2) + ":00");
        ret.put("endTime", params.keySet().contains("endtime")?params.get("endtime"):Util.add0(meetingSetInfo.getTimeRangeEnd(), 2) + ":59");
        ret.put("address", params.keySet().contains("address")?params.get("address"):"");
        //是否开启可以新建会议功能
        ret.putAll(MeetingUtil.canCreate(user,params));
        //取一下议程和服务的默认设置
        int meetingServiceDefaultValue = 0;
        int meetingTopicDefaultValue = 0;
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery("select * from meetingset");
        if(recordSet.next()){
            meetingServiceDefaultValue = Util.getIntValue(recordSet.getString("meetingServiceDefaultValue"));
            meetingTopicDefaultValue = Util.getIntValue(recordSet.getString("meetingTopicDefaultValue"));
        }
        ret.put("meetingTopicDefaultValue",meetingTopicDefaultValue);
        ret.put("meetingServiceDefaultValue",meetingServiceDefaultValue);
        return ret;
    }

    /**
     * 切换会议类型，变更会议召集人
     *
     * @param
     * @param meetingType
     * @param
     * @param
     * @return
     */
    public boolean getHasCallerByType(String meetingType) throws Exception {
        boolean hasCaller = false;
        ResourceComInfo rci = new ResourceComInfo();
        if (!meetingType.isEmpty()) {
            RecordSet recordSet = new RecordSet();
            recordSet.executeProc("MeetingCaller_SByMeeting", meetingType);
            String whereclause = "where ( ";
            String qswhere = "";
            int ishead = 0;
            while (recordSet.next()) {
                String callertype = recordSet.getString("callertype");
                int seclevel = Util.getIntValue(recordSet.getString("seclevel"), 0);
                String rolelevel = recordSet.getString("rolelevel");
                String thisuserid = recordSet.getString("userid");
                String departmentid = recordSet.getString("departmentid");
                String roleid = recordSet.getString("roleid");
                String subcompanyid = recordSet.getString("subcompanyid");
                int seclevelMax = Util.getIntValue(recordSet.getString("seclevelMax"), 0);
                int jobtitleid = Util.getIntValue(recordSet.getString("jobtitleid"), 0);
                int joblevel = Util.getIntValue(recordSet.getString("joblevel"), 0);
                String joblevelvalue = recordSet.getString("joblevelvalue");

                if (callertype.equals("1")) {
                    if (ishead == 0) {
                        whereclause += " t1.id=" + thisuserid;
                    }
                    if (ishead == 1) {
                        whereclause += " or t1.id=" + thisuserid;
                    }
                }
                if (callertype.equals("2")) {
                    if (ishead == 0) {
                        whereclause += " t1.id in (select id from hrmresource where departmentid=" + departmentid + " and seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + " )";
                    }
                    if (ishead == 1) {
                        whereclause += " or t1.id in (select id from hrmresource where departmentid=" + departmentid + " and seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + " )";
                    }
                }
                if (callertype.equals("3")) {
                    HrmCommonService hcs = new HrmCommonServiceImpl();
                    List roleMemberList = hcs.getRoleMembers(Util.getIntValue(roleid), rolelevel);
                    if (roleMemberList.size() > 0) {
                        if (ishead == 0) {
                            whereclause += " t1.id in (select id from hrmresource where id in (" + roleMemberList.toString().substring(1, roleMemberList.toString().length() - 1) + ") and seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + ")";
                        }
                        if (ishead == 1) {
                            whereclause += " or t1.id in (select id from hrmresource where id in (" + roleMemberList.toString().substring(1, roleMemberList.toString().length() - 1) + ") and seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + ")";
                        }
                    }
                }
                if (callertype.equals("4")) {
                    if (ishead == 0) {
                        whereclause += " t1.id in (select id from hrmresource where seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + " )";
                    }
                    if (ishead == 1) {
                        whereclause += " or t1.id in (select id from hrmresource where seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + " )";
                    }
                }
                if (callertype.equals("5")) {
                    if (ishead == 0) {
                        whereclause += " t1.id in (select id from hrmresource where subcompanyid1=" + subcompanyid + " and seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + " )";
                    }
                    if (ishead == 1) {
                        whereclause += " or t1.id in (select id from hrmresource where subcompanyid1=" + subcompanyid + " and seclevel >=" + seclevel + " and seclevel <= " + seclevelMax + " )";
                    }
                }
                if (callertype.equals("8")) {
                    if (ishead == 0) {
                        whereclause += " t1.id in (select id from hrmresource where jobtitle=" + jobtitleid;
                        if (joblevel == 1) {
                            whereclause += " and subcompanyid1 in (" + joblevelvalue + ")";
                        } else if (joblevel == 2) {
                            whereclause += " and departmentid in (" + joblevelvalue + ")";
                        }
                        whereclause += ")";
                    }
                    if (ishead == 1) {
                        whereclause += " or t1.id in (select id from hrmresource where jobtitle=" + jobtitleid;
                        if (joblevel == 1) {
                            whereclause += " and subcompanyid1 in (" + joblevelvalue + ")";
                        } else if (joblevel == 2) {
                            whereclause += " and departmentid in (" + joblevelvalue + ")";
                        }
                        whereclause += ")";
                    }
                }
                if (ishead == 0) ishead = 1;
            }

            //召集人查询条件
            if (!whereclause.equals("where ( ") && whereclause.length() > 5) {
                whereclause += " )";
                qswhere = whereclause.substring(5);
                recordSet.execute("select t1.id from hrmresource t1,hrmdepartment t2 where t1.departmentid = t2.id and (t1.status = 0 or t1.status = 1 or t1.status = 2 or t1.status = 3) and " + qswhere);
                if (recordSet.getCounts() > 0) {
                    hasCaller = true;
                }
            }
        }
        return hasCaller;
    }

    /**
     * 获取会议议程字段信息
     *
     * @param meetingid
     * @param user
     * @param mpu       当前人的针对会议的角色权限
     * @return
     * @throws Exception
     */
    public Map getMeetingTopicField(String meetingid, User user, boolean isView, MeetingPrmUtil mpu, boolean ismobile) throws Exception {
        Map ret = new HashMap();
        MeetingFieldService meetingFieldService = (MeetingFieldServiceImpl) ServiceUtil.getService(MeetingFieldServiceImpl.class, user);
        params.put("isView",true);
        ret = meetingFieldService.getMeetingTopicField(params);
        return ret;
    }

    /**
     * 获取会议服务字段信息
     *
     * @param meetingid
     * @param user
     * @return
     * @throws Exception
     */
    public Map getMeetingServiceField(String meetingid, User user, Boolean ismobile) throws Exception {
        Map ret = new HashMap();
        MeetingFieldService meetingFieldService = (MeetingFieldServiceImpl) ServiceUtil.getService(MeetingFieldServiceImpl.class, user);
        ret = meetingFieldService.getMeetingServiceField(params);
        return ret;
    }

    /**
     * 是否在手机端会议信息页面显示回执按钮
     *
     * @param meetingId
     * @param begindate
     * @param begintime
     * @param user
     * @param isdecision
     * @return
     */
    public Map showReceiptBtnForMobile(String meetingId, String begindate, String begintime, User user, String isdecision,String meetingstatus) {
        Calendar today = Calendar.getInstance();
        String nowdate = Util.add0(today.get(Calendar.YEAR), 4) + "-" +
                Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
        String nowtime = Util.add0(today.get(Calendar.HOUR_OF_DAY), 2) + ":" +
                Util.add0(today.get(Calendar.MINUTE), 2);
        Map retMap = new HashMap();
        RecordSet rs = new RecordSet();
        boolean canEdit = false;
        String allUser = MeetingShareUtil.getAllUser(user);
        boolean flag = false;
        MeetingSetInfo meetingSetInfo = new MeetingSetInfo();

        MeetingPrmUtil mpu = new MeetingPrmUtil(user, meetingId);
        int userPrm = mpu.getUserPrm();

        if( meetingSetInfo.getOnlyFlowReceipt() == 1){
            flag = userPrm == 3 || mpu.getIsmanager() || mpu.getIscaller() ;
        }else{
            flag= true;
        }
        if (flag && (!isdecision.equals("1") && !isdecision.equals("2")) && (begindate + ":" + begintime).compareTo(nowdate + ":" + nowtime) > 0 && meetingstatus.equals("2")) {
            canEdit = true;
        }
        rs.executeQuery("select id,isAttend from meeting_member2 where memberType=1 and membermanager = ? and meetingid = ?", user.getUID(), meetingId);
        if (rs.next()) {
            retMap.put("recepitId", rs.getString(1));
            //如果不为空的情况下那么就说明是提交过回执的,那么就可以查看回执
            if (!rs.getString(2).equals("")) {
                if (canEdit) {
                    retMap.put("canEdit", true);
                } else {
                    retMap.put("canEdit", false);
                }
                retMap.put("canView", true);
                retMap.put("canSubmit", false);
            } else {
                if (canEdit) {
                    retMap.put("canEdit", true);
                    retMap.put("canSubmit", true);
                } else {
                    retMap.put("canEdit", false);
                    retMap.put("canSubmit", false);
                }
                retMap.put("canView", false);
            }

        }

        return retMap;
    }


    private boolean containUser(String allUser, List userids) {
        for (int i = 0; i < userids.size(); i++) {
            if ("".equals(userids.get(i))) continue;
            if (("," + allUser + ",").indexOf("," + userids.get(i) + ",") > -1) {
                return true;
            }
        }
        return false;
    }

    private List<SplitMobileDataBean> getJonsConfig() {
        List<SplitMobileDataBean> list = new ArrayList<SplitMobileDataBean>();
        MobileJsonConfigUtil.addKey(list, "col1.col1_row1.subject");
        MobileJsonConfigUtil.addKey(list, "col1.col1_row2.hrmids");
        return list;
    }

}

