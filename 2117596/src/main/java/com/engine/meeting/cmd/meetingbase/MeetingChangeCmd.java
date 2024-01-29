package com.engine.meeting.cmd.meetingbase;

import com.api.meeting.util.MeetingPrmUtil;
import com.api.workplan.util.TimeZoneCastUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Meeting;
import com.engine.common.constant.BizLogType;
import com.engine.common.constant.ParamConstant;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.core.interceptor.CommandContext;
import com.engine.meeting.constant.MeetingMonitorConst;
import com.engine.meeting.entity.MonitorSetBean;
import com.engine.meeting.service.MeetingBaseService;
import com.engine.meeting.service.impl.MeetingBaseServiceImpl;
import com.engine.meeting.util.MeetingMonitorUtil;
import com.engine.meeting.util.MeetingSeatUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.BatchRecordSet;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.dateformat.UnifiedConversionInterface;
import weaver.general.BaseBean;
import weaver.general.ThreadPoolUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.Maint.MeetingComInfo;
import weaver.meeting.Maint.MeetingInterval;
import weaver.meeting.Maint.MeetingRoomComInfo;
import weaver.meeting.Maint.MeetingSetInfo;
import weaver.meeting.MeetingLog;
import weaver.meeting.MeetingUtil;
import weaver.meeting.MeetingViewer;
import weaver.meeting.defined.MeetingFieldComInfo;
import weaver.meeting.defined.MeetingFieldManager;
import weaver.meeting.remind.MeetingRemindUtil;
import weaver.meeting.util.exchange.MeetingExchangeUtil;
import weaver.system.SysRemindWorkflow;
import weaver.systeminfo.SystemEnv;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class MeetingChangeCmd extends AbstractCommonCommand<Map<String, Object>> {

    private SimpleBizLogger logger;

    public MeetingChangeCmd(User user, Map<String, Object> params) {
        this.user = user;
        this.params = params;
        this.logger = new SimpleBizLogger();
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    public List<BizLogContext> getLogContexts() {
        return logger.getBizLogContexts();
    }

    private void beforeLog(String targetId) {
        BizLogContext bizLogContext = new BizLogContext();
        bizLogContext.setLogType(BizLogType.MEETING);//模块类型
        bizLogContext.setBelongType(BizLogSmallType4Meeting.MEETING_BASE);//所属类型
        bizLogContext.setBelongTypeTargetId(targetId);//所属类型id
        bizLogContext.setBelongTypeTargetName("meeting");//所属类型名称
        bizLogContext.setLogSmallType(BizLogSmallType4Meeting.MEETING_BASE);//当前小类型
        bizLogContext.setOperateType(BizLogOperateType.UPDATE);
        bizLogContext.setParams(params);
        logger.setUser(user);//当前操作人
        logger.setParams(params);//request请求参数
        String mainSql = "select * from meeting where id in(" + targetId + ")";
        logger.setMainSql(mainSql, "id");
        logger.setMainTargetNameColumn("name");
        logger.before(bizLogContext);
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        String userid = user.getUID() + "";
        String meetingid = Util.null2String(params.get("meetingid"));
        String isFrom = Util.null2String(params.get("isFrom"));
        Map ret = new HashMap();
        if ("".equals(meetingid)) {
            ret.put("status", false);
            ret.put("error", SystemEnv.getHtmlLabelName(132255, user.getLanguage()));
            ret.put("showMsg", SystemEnv.getHtmlLabelName(132255, user.getLanguage()));
            return ret;
        }
        UnifiedConversionInterface uci = new UnifiedConversionInterface();
        boolean needTimeZone = uci.getTimeZoneStatus();
        String timeZoneShow = "";
        if(needTimeZone && TimeZoneCastUtil.canCastZone){
            timeZoneShow = "(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")";
        }else{
            timeZoneShow = "";
        }
        Date newdate = new Date();
        long datetime = newdate.getTime();
        Timestamp timestamp = new Timestamp(datetime);
        String CurrentDate = (timestamp.toString()).substring(0, 4) + "-" + (timestamp.toString()).substring(5, 7) + "-" + (timestamp.toString()).substring(8, 10);
        String CurrentTime = (timestamp.toString()).substring(11, 13) + ":" + (timestamp.toString()).substring(14, 16) + ":" + (timestamp.toString()).substring(17, 19);
        RecordSet recordSet = new RecordSet();
        recordSet.executeProc("Meeting_SelectByID", meetingid);
        recordSet.next();
        String meetingtype = recordSet.getString("meetingtype");
        String meetingname = recordSet.getString("name");
        String address = recordSet.getString("address");
        String begindate = recordSet.getString("begindate");
        String begintime = recordSet.getString("begintime");

        String caller = recordSet.getString("caller");
        String contacter = recordSet.getString("contacter");
        String creater = recordSet.getString("creater");
        String isdecision = recordSet.getString("isdecision");
        String meetingstatus = recordSet.getString("meetingstatus");
        String secretLevel = recordSet.getString("secretLevel");

        //记录日志新
        beforeLog(meetingid);

        MeetingSetInfo meetingSetInfo = new MeetingSetInfo();
        int repeatType = Util.getIntValue(recordSet.getString("repeatType"), 0);

//        String allUser = MeetingShareUtil.getAllUser(user);
        MeetingPrmUtil mpu=new MeetingPrmUtil(user,meetingid);
        int userPrm = mpu.getUserPrm();

//        if (MeetingShareUtil.containUser(allUser, caller)) {//是召集人 赋权限为3
//            userPrm = meetingSetInfo.getCallerPrm();
//            if (userPrm != 3) userPrm = 3;
//        }
//        if (MeetingShareUtil.containUser(allUser, contacter) && userPrm < 3) {//是联系人 且权限小于3
//            if (userPrm < meetingSetInfo.getContacterPrm()) { //当前权限小于联系人权限
//                userPrm = meetingSetInfo.getContacterPrm(); //赋联系人权限
//            }
//        }
//        if (MeetingShareUtil.containUser(allUser, creater) && userPrm < 3) {//是创建人 且权限小于3
//            if (userPrm < meetingSetInfo.getCreaterPrm()) {//当前权限小于创建人权限
//                userPrm = meetingSetInfo.getCreaterPrm();//赋创建人权限
//            }
//        }
		MeetingMonitorUtil MeetingMonitorUtil = new MeetingMonitorUtil(meetingid);
        MonitorSetBean ms = MeetingMonitorUtil.getMeetingMonitorPermission(meetingtype, creater, user, MeetingMonitorConst.IS_CHANGE);
        boolean isnotstart = false;//会议未开始
        //当前时间小于会议开始时间 即会议未开始
        if ((begindate + ":" + begintime).compareTo(CurrentDate + ":" + CurrentTime) > 0 && !isdecision.equals("2"))
            isnotstart = true;
        boolean canedit = false;
        //如果来自于exchange的变更。可以允许变更
        if ("exchange".equals(isFrom) || (("2".equals(meetingstatus) && isnotstart) && (userPrm == 3 || ms.isIschange()) && repeatType == 0 && new MeetingSetInfo().getCanChange()==1)) {
            canedit = true;
        }

        if (!canedit) {
            ret.put("status", false);
            ret.put("error", SystemEnv.getHtmlLabelName(390613,user.getLanguage()));
            return ret;
        }

        String userId = "" + user.getUID();
        int roomType = 1;
        String address1 = Util.null2String(params.get("address"));//会议地点
        String customizeAddress = Util.null2String(params.get("customizeAddress"));
        if (!"".equals(address1)) {//优先选择会议室
//            customizeAddress = "";
        } else {//自定义会议室
            roomType = 2;
        }
        //==zj==基本信息获取
        String desc_n = Util.null2String(params.get("desc_n"));
        String accessorys = Util.null2String(params.get("accessorys"));
        new BaseBean().writeLog("==zj==accessory返回值:" + accessorys);

        //时间
        String begindate1 = Util.null2String(params.get("begindate"));
        String enddate1 = Util.null2String(params.get("enddate"));
        String begintime1 = Util.null2String(params.get("begintime"));
        String endtime1 = Util.null2String(params.get("endtime"));
        String ewsid = Util.null2String(params.get("ewsid"));
        /* ----------新增日期转换 start ----------------*/
        String changeToB[] = TimeZoneCastUtil.FormatDateServer(begindate1 + " " + begintime1, 0);
        String changeToE[] = TimeZoneCastUtil.FormatDateServer(enddate1 + " " + endtime1, 1);
        begindate1 = changeToB[0];
        begintime1 = changeToB[1];
        enddate1 = changeToE[0];
        endtime1 = changeToE[1];
        /* ----------新增日期转换 end ----------------*/
        //提醒方式和时间
        String remindTypeNew = Util.null2String(params.get("remindTypeNew"));//新的提示方式
        int remindImmediately = Util.getIntValue(Util.null2String(params.get("remindImmediately")), 0);  //是否立即提醒
        int remindBeforeStart = Util.getIntValue(Util.null2String(params.get("remindBeforeStart")), 0);  //是否开始前提醒
        int remindBeforeEnd = Util.getIntValue(Util.null2String(params.get("remindBeforeEnd")), 0);  //是否结束前提醒
        int remindHoursBeforeStart = Util.getIntValue(Util.null2String(params.get("remindHoursBeforeStart")), 0);//开始前提醒小时
        int remindTimesBeforeStart = Util.getIntValue(Util.null2String(params.get("remindTimesBeforeStart")), 0);  //开始前提醒时间
        int remindHoursBeforeEnd = Util.getIntValue(Util.null2String(params.get("remindHoursBeforeEnd")), 0);//结束前提醒小时
        int remindTimesBeforeEnd = Util.getIntValue(Util.null2String(params.get("remindTimesBeforeEnd")), 0);  //结束前提醒时间
        //参会人员
        String hrmmembers = Util.null2String(params.get("hrmmembers"));//参会人员
        String tempHrmmembers = hrmmembers;
        String hrmDepartments = Util.null2String(params.get("hrmDepartments"));//参会部门
        String hrmSubCompanys = Util.null2String(params.get("hrmSubCompanys"));//参会分部
        if (hrmmembers.isEmpty() && hrmSubCompanys.isEmpty() && hrmDepartments.isEmpty()) {//参会人为空时 默认设置为当前人员
            hrmmembers = userid;
        }
        //将参会部门参会分部集合到参会人员中方便下面比较人员是否增加删除
        String depAndSubHrms = MeetingUtil.getDepAndSubHrms(meetingid,hrmDepartments,hrmSubCompanys);
        if(!depAndSubHrms.equals("")){
            hrmmembers += depAndSubHrms;
        }
        int totalmember = Util.getIntValue(Util.null2String(params.get("totalmember")), 0);//参会人数
        String othermembers = Util.fromScreen(Util.null2String(params.get("othermembers")), user.getLanguage());//其他参会人员
        String crmmembers = Util.null2String(params.get("crmmembers"));//参会客户
        int crmtotalmember = Util.getIntValue(Util.null2String(params.get("crmtotalmember")), 0);//参会人数

        recordSet.executeProc("Meeting_SelectByID", meetingid);
        recordSet.next();
        String columnName[] = recordSet.getColumnName();
        Map beforeChangeMeetingInfo = new HashMap();
        Arrays.asList(columnName).stream().forEach(_item->{
            beforeChangeMeetingInfo.put(_item,recordSet.getString(_item));
        });
        String oldbegindate = recordSet.getString("begindate");
        String oldbegintime = recordSet.getString("begintime");
        String oldenddate = recordSet.getString("enddate");
        String oldendtime = recordSet.getString("endtime");
        String oldaddress = recordSet.getString("address");
        String oldcustomizeAddress = recordSet.getString("customizeAddress");
        String oldmembers = recordSet.getString("hrmmembers");//原参会人员
        String oldHrmDepartments = Util.null2String(recordSet.getString("hrmDepartments"));//参会部门
        String oldHrmSubCompanys = Util.null2String(recordSet.getString("hrmSubCompanys"));//参会分部
        //将参会部门参会分部集合到参会人员中方便下面比较人员是否增加删除
        String oldDepAndSubHrms = MeetingUtil.getDepAndSubHrms(meetingid,oldHrmDepartments,oldHrmSubCompanys);
        if(!oldDepAndSubHrms.equals("")){
            oldmembers += oldDepAndSubHrms;
        }
        String oldcrmmembers = recordSet.getString("crmmembers");//原参会客户
        String meetingName = recordSet.getString("name");
        String MeetingContacter = recordSet.getString("contacter");


        String updateSql = "update Meeting set "
                + "   begindate = '" + begindate1 + "' "
                + " , enddate = '" + enddate1 + "' "
                + " , begintime = '" + begintime1 + "' "
                + " , endtime = '" + endtime1 + "' "
                + " , roomType = " + roomType
                + " , address = '" + address1 + "' "
                + " , customizeAddress = '" + customizeAddress + "' "
                + " , remindTypeNew = '" + remindTypeNew + "' "
                + " , remindImmediately = " + remindImmediately
                + " , remindBeforeStart = " + remindBeforeStart
                + " , remindBeforeEnd = " + remindBeforeEnd
                + " , remindHoursBeforeStart = " + remindHoursBeforeStart
                + " , remindTimesBeforeStart = " + remindTimesBeforeStart
                + " , remindHoursBeforeEnd = " + remindHoursBeforeEnd
                + " , remindTimesBeforeEnd = " + remindTimesBeforeEnd;
        if (recordSet.getDBType().equalsIgnoreCase("oracle") &&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")) {
            updateSql += " , hrmmembers = empty_clob() ";
        } else {
            updateSql += " , hrmmembers = '" + tempHrmmembers + "' ";
        }
        //==zj==添加变更基本信息字段desc_n,accessorys
        if (desc_n != null){
            updateSql += " , desc_n = '" + desc_n + "'" ;
        }
        if (accessorys != null){
            updateSql += ",accessorys = '" + accessorys + "'";
        }
        updateSql += " , crmmembers = '" + crmmembers + "' "
                + " , othermembers = '" + othermembers + "' "
                + " , totalmember = " + totalmember
                + " , crmtotalmember = " + crmtotalmember
                + " , hrmSubCompanys = '" + hrmSubCompanys + "' "
                + " , hrmDepartments = '" + hrmDepartments + "' "
                + " where id = " + meetingid;
        //变更会议基本信息
        recordSet.execute(updateSql);
        if (recordSet.getDBType().equalsIgnoreCase("oracle") &&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")) {
            MeetingUtil meetingUtil = new MeetingUtil();
            meetingUtil.updateHrmmembers(meetingid, tempHrmmembers);
        }
        //变更自定义字段
        String customFields = Util.null2String(params.get("customFields"));
        MeetingFieldComInfo meetingFieldComInfo = new MeetingFieldComInfo();
        if(!customFields.equals("")){
            List<String> customFieldsList = Util.TokenizerString(customFields,",");
            List updateColumn = new ArrayList();
            List updateValue = new ArrayList();
            String a = "";
            customFieldsList.stream().filter(item->!item.equals("")).forEach(item->{
                String column = meetingFieldComInfo.getFieldname(item);
                if(column!=null && !column.equals("")){
                    if(params.containsKey(column)){
                        updateColumn.add(column);
                        updateValue.add(params.get(column));
                    }
                }
            });
            if(updateColumn.size()>0){
                updateValue.add(meetingid);
                String columns = "";
                for (int i = 0; i < updateColumn.size(); i++) {
                    columns += columns.equals("")?updateColumn.get(i)+"=?":","+updateColumn.get(i)+"=?";
                }
                recordSet.executeUpdate("update meeting set "+columns+" where id = ?",updateValue);
            }

        }

        //变更参会人员
        String ProcPara = "";
        char flag = 2;
        StringBuffer stringBuffer = new StringBuffer();

        boolean meetingInfoChanged = false;//会议主要信息发生变更
        if (!((begindate1 + begintime1 + enddate1 + endtime1).equals(oldbegindate + oldbegintime + oldenddate + oldendtime) && address1.equals(oldaddress) && oldcustomizeAddress.equals(customizeAddress))) {
            meetingInfoChanged = true;
        }
        //会议变更提醒处理
        if (!oldmembers.equals(hrmmembers) || !oldcrmmembers.equals(crmmembers) || meetingInfoChanged) {//前后人员不相同或客户不相同或主要信息发生变更
            //获取参会客户的客户经理
            ArrayList<String> tmpcrmids = Util.TokenizerString(crmmembers, ",");
            crmmembers = "";
            for (int i = 0; i < tmpcrmids.size(); i++) {
                String membermanager = "";
                recordSet.executeProc("CRM_CustomerInfo_SelectByID", "" + tmpcrmids.get(i));
                if (recordSet.next()) {
                    membermanager = recordSet.getString("manager");
                    if (!membermanager.isEmpty()) {
                        if (!crmmembers.isEmpty()) {
                            crmmembers += ",";
                        }
                        crmmembers += membermanager;
                    }
                }
            }
            //获取原参会客户的客户经理
            ArrayList<String> tmpOldcrmids = Util.TokenizerString(oldcrmmembers, ",");
            oldcrmmembers = "";
            for (int i = 0; i < tmpOldcrmids.size(); i++) {
                String membermanager = "";
                recordSet.executeProc("CRM_CustomerInfo_SelectByID", "" + tmpOldcrmids.get(i));
                if (recordSet.next()) {
                    membermanager = recordSet.getString("manager");
                    if (!membermanager.isEmpty()) {
                        if (!oldcrmmembers.isEmpty()) {
                            oldcrmmembers += ",";
                        }
                        oldcrmmembers += membermanager;
                    }
                }
            }
            // 1：删除其他人员签到信息,
            // 2：更新参会人员签到信息
            recordSet.executeQuery("select othermember,membermanager from meeting_member2 where meetingid=? ", meetingid);
            while (recordSet.next()) {
                //如果存在于参会人员中的话,就不删除
                if (!recordSet.getString(1).equals("")) {
                    String[] othermbr = recordSet.getString(1).split(",");
                    for (int j = 0; j < othermbr.length; j++) {
                        if (("," + hrmmembers + ",").indexOf("," + othermbr[j] + ",") < 0) {
                            //删除签到信息
                            recordSet.executeUpdate("delete from meeting_sign where meetingid=? and userid=? ", meetingid, othermbr[j]);
                        }
                    }
                }
                if (!recordSet.getString(2).equals("")) {
                    recordSet.executeUpdate("update meeting_sign set signtime = '',signReson = '', longitude = 0, latitude = 0, signRemark = '', site = '' ," +
                            " signBackTime = '', signbacklatitude = 0 ,signbacklongitude = 0 , backSite = '' " +
                            " where meetingid=? and userid=? ", meetingid, recordSet.getString(2));
                }
            }
            String[] arrmbr = hrmmembers.split(",");
            String[] arroldmbr = oldmembers.split(",");
            List signLst = new ArrayList();
            RecordSet rss = new RecordSet();
            rss.executeQuery("select * from meeting_sign where meetingid = ?",meetingid);
            while (rss.next()){
                signLst.add(Util.null2String(rss.getString("userid")));
            }
            ArrayList<String> newHrm = new ArrayList<String>();
            ArrayList delHrm = new ArrayList();
            for (int i = 0; i < arrmbr.length; i++) {
                if (Util.getIntValue(arrmbr[i]) > 0 && ("," + oldmembers + ",").indexOf("," + arrmbr[i] + ",") < 0 && ("," + oldcrmmembers + ",").indexOf("," + arrmbr[i] + ",") < 0) {//当前参会人不在原参会人中且不为原参会客户的客户经理 为新加人员
                    newHrm.add(arrmbr[i]);
                    //加入签到表
                    //如果存在该人，那么直接更新为参会人
                    if(signLst.contains(arrmbr[i])){
                        recordSet.executeUpdate("UPDATE meeting_sign SET attendType = 1,flag = 1 WHERE userid = ? AND meetingid = ?",arrmbr[i],meetingid);
                    }else{
                        recordSet.execute("insert into  meeting_sign (meetingid,userid,attendType,flag) values (" + meetingid + "," + arrmbr[i] + ",1,1)");
                    }
                    //加入meeting_member2表中
                    ProcPara = meetingid;
                    ProcPara += flag + "1";
                    ProcPara += flag + "" + arrmbr[i];
                    ProcPara += flag + "" + arrmbr[i];
                    recordSet.executeProc("Meeting_Member2_Insert", ProcPara);

                    //标识会议是否查看过
                    stringBuffer = new StringBuffer();
                    stringBuffer.append("INSERT INTO Meeting_View_Status(meetingId, userId, userType, status) VALUES(");
                    stringBuffer.append(meetingid);
                    stringBuffer.append(", ");
                    stringBuffer.append(arrmbr[i]);
                    stringBuffer.append(", '");
                    stringBuffer.append("1");
                    stringBuffer.append("', '");
                    if (userid.equals(arrmbr[i]))
                    //当前操作用户表示已看
                    {
                        stringBuffer.append("1");
                    } else {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append("')");
                    recordSet.execute(stringBuffer.toString());
                }
            }
            Set<String> delHrmSet=new LinkedHashSet<String>();//用于删除座位信息
            for (int i = 0; i < arroldmbr.length; i++) {
                if (("," + hrmmembers + ",").indexOf("," + arroldmbr[i] + ",") < 0 && ("," + crmmembers + ",").indexOf("," + arroldmbr[i] + ",") < 0) {//原参会人不在当前参会人中且不为当前参会客户的客户经理 为删除人员
                    delHrm.add(arroldmbr[i]);
                    delHrmSet.add(arroldmbr[i]);
                    //删除签到信息
                    recordSet.executeUpdate("delete from meeting_sign where meetingid=? and userid=? ", meetingid, arroldmbr[i]);
                    //删除对应otherMember中的签到数据
                    recordSet.executeQuery("select othermember from meeting_member2 where meetingid=? and memberid=? ", meetingid, arroldmbr[i]);
                    if (recordSet.next()) {
                        //如果存在于参会人员中的话,就不删除
                        if (!recordSet.getString(1).equals("")) {
                            String[] othermbr = recordSet.getString(1).split(",");
                            for (int j = 0; j < othermbr.length; j++) {
                                if (("," + hrmmembers + ",").indexOf("," + othermbr[j] + ",") < 0) {
                                    delHrmSet.add(othermbr[j]);
                                    //删除签到信息
                                    recordSet.executeUpdate("delete from meeting_sign where meetingid=? and userid=? ", meetingid, othermbr[j]);
                                }
                            }
                        }
                    }
                    //删除参会人员对应的回执流程
                    recordSet.executeQuery("select requestid,id from meeting_member2 where meetingid=? and memberid=?", meetingid, arroldmbr[i]);
                    if(recordSet.next()){
                        String requestid = recordSet.getString(1);
                        if(!requestid.equals("")){
                            MeetingUtil.deleteWF(requestid,user,"from_changeMeeting_main");
                        }
                        //删除对应的参会人员邀请的其他人员
                        MeetingUtil.deleteOtherMemberAndWF(meetingid,recordSet.getString(2),user,"from_changeMeeting_child");
                    }


                    //删除meeting_member2中的数据
                    recordSet.executeUpdate("delete from meeting_member2 where meetingid=? and memberid=?", meetingid, arroldmbr[i]);
                    //删除Meeting_View_Status中的数据
                    recordSet.executeUpdate("delete from Meeting_View_Status where meetingid=? and userid=? and usertype=?", meetingid, arroldmbr[i], 1);
                }

            }
            //删除被删除人员的座位信息
            MeetingSeatUtil.deleteMemberSeat(meetingid,new ArrayList(delHrmSet));

            String[] arrcrm = crmmembers.split(",");
            String[] arroldcrm = oldcrmmembers.split(",");
            //根据客户id来判断是否是新添加客户
            for (int i = 0; i < tmpcrmids.size(); i++) {
                if (Util.getIntValue(tmpcrmids.get(i)) > 0) {
                    //为新添加的客户
                    if (!tmpOldcrmids.contains(tmpcrmids.get(i))) {
                        //取得该客户的客户经理
                        String membermanager = "";
                        recordSet.executeProc("CRM_CustomerInfo_SelectByID", "" + tmpcrmids.get(i));
                        if (recordSet.next()) membermanager = recordSet.getString("manager");
                        //加入meeting_member2表中
                        ProcPara = meetingid;
                        ProcPara += flag + "2";
                        ProcPara += flag + "" + tmpcrmids.get(i);
                        ProcPara += flag + membermanager;
                        recordSet.executeProc("Meeting_Member2_Insert", ProcPara);
                        //判断该客户经理是否为参会人员,不为参会人员 就添加到签到表中,如果是参会人员那么之前就添加到签到表中了
                        if (("," + oldcrmmembers + ",").indexOf("," + membermanager + ",") < 0 && ("," + oldmembers + ",").indexOf("," + membermanager + ",") < 0 && !newHrm.contains(membermanager)) {
                            newHrm.add(membermanager);
                            //加入签到表
                            recordSet.execute("insert into  meeting_sign (meetingid,userid,attendType,flag) values (" + meetingid + "," + membermanager + ",1,1)");
                        }
                    }
                }
            }

            for (int i = 0; i < tmpOldcrmids.size(); i++) {
                if (Util.getIntValue(tmpOldcrmids.get(i)) > 0) {
                    //如果不包含在原参会客户中,就代表删除了
                    if (!tmpcrmids.contains(tmpOldcrmids.get(i))) {
                        //取得该客户的客户经理
                        String membermanager = "";
                        recordSet.executeProc("CRM_CustomerInfo_SelectByID", "" + tmpOldcrmids.get(i));
                        if (recordSet.next()) membermanager = recordSet.getString("manager");
                        delHrm.add(membermanager);
                        //删除对应的回执流程
                        recordSet.executeQuery("select requestid,id from meeting_member2 where meetingid=? and memberid=?", meetingid, tmpOldcrmids.get(i));
                        if(recordSet.next()){
                            String requestid = recordSet.getString(1);
                            if(!requestid.equals("")){
                                MeetingUtil.deleteWF(requestid,user,"from_changeMeeting");
                            }
                            //删除对应的参会人员邀请的其他人员
                            MeetingUtil.deleteOtherMemberAndWF(meetingid,recordSet.getString(2),user,"from_changeMeeting_child");
                        }
                        //删除meeting_member2中的数据
                        recordSet.executeUpdate("delete from meeting_member2 where meetingid=? and memberid=?", meetingid, tmpOldcrmids.get(i));
                        //删除签到数据 判断该人员是否在参会人员中并且之前的参会客户经理中是否已经有该人员
                        if (("," + hrmmembers + ",").indexOf("," + membermanager + ",") < 0 && ("," + crmmembers + ",").indexOf("," + membermanager + ",") < 0) {
                            //删除签到信息
                            recordSet.executeUpdate("delete from meeting_sign where meetingid=? and userid=?", meetingid, membermanager);
                        }
                    }
                }

            }
            for (int i = 0; i < arroldcrm.length; i++) {
                if (("," + crmmembers + ",").indexOf("," + arroldcrm[i] + ",") < 0 && ("," + hrmmembers + ",").indexOf("," + arroldcrm[i] + ",") < 0) {//原参会客户经理不在当前参会客户经理中且不为当前参会人员 为删除客户
                    delHrm.add(arroldcrm[i]);
                    //删除对应的回执流程
                    recordSet.executeQuery("select requestid,id from meeting_member2 where meetingid=? and memberid=?", meetingid, arroldcrm[i]);
                    if(recordSet.next()){
                        String requestid = recordSet.getString(1);
                        if(!requestid.equals("")){
                            MeetingUtil.deleteWF(requestid,user,"from_changeMeeting");
                        }
                        //删除对应的参会人员邀请的其他人员
                        MeetingUtil.deleteOtherMemberAndWF(meetingid,recordSet.getString(2),user,"from_changeMeeting_child");
                    }
                    //删除签到信息
                    recordSet.execute("delete from meeting_sign where meetingid=" + meetingid + " and userid=" + arroldcrm[i]);
                    //删除meeting_member2中的数据
                    recordSet.executeUpdate("delete from meeting_member2 where meetingid=? and memberid=?", meetingid, arroldcrm[i]);
                }
            }

            MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
            SysRemindWorkflow sysRemindWorkflow = new SysRemindWorkflow();
            String wfname = "";
            String wfaccepter = "";
            String wfremark = "";
            //设置默认提醒工作流的密级等级
            sysRemindWorkflow.setSecLevel(secretLevel);
            try {
                //处理会议室负责人消息中心提醒通知
                if(!oldaddress.equals(address1)){
                    RecordSet rs = new RecordSet();
                    //判断分权
                    ManageDetachComInfo manageDetachComInfo=new ManageDetachComInfo();
                    //是否开启会议分权
                    boolean detachable=manageDetachComInfo.isUseMtiManageDetach();
                    int subId = Util.getIntValue(new ResourceComInfo().getSubCompanyID(creater)) ;
                    boolean openCustomSet = false;
                    rs.executeQuery(" select isOpen from meeting_remind_detachBaseInfo where subcompanyid = ?",subId);
                    if(rs.next()){
                        openCustomSet = rs.getInt(1) == 1;
                    }
                    if(detachable && openCustomSet){
                        rs.executeQuery("select fieldid from meeting_reminderFields where isOpen = 1 and fieldid = '5' and subcompanyid = ?",subId);
                    }else{
                        rs.executeQuery("select fieldid from meeting_reminderFields where isOpen = 1 and fieldid = '5' and subcompanyid = 0");
                    }

                    if(rs.next()){//包含会议室负责人提醒
                        Set<String> oldAddressHrmIdsSet = MeetingRemindUtil.getAddressHrmids(oldaddress);
                        Set<String> addressHrmIdsSet = MeetingRemindUtil.getAddressHrmids(address1);
                        Map<String,Set> diffMap = MeetingRemindUtil.diffSet(addressHrmIdsSet,oldAddressHrmIdsSet);
                        //添加人员
                        Set<String> addMemberSet = diffMap.get("add");
                        //删除人员
                        Set<String> delMemberSet = diffMap.get("del");
                        //原封不到的人员
                        Set<String> keepMemberSet = diffMap.get("keep");
                        if(addMemberSet.size() > 0){
                            wfaccepter = addMemberSet.stream().collect(Collectors.joining(","));
                            wfname = Util.toMultiLangScreen("518371") + ":";
                            wfname += meetingName;
                            wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                            wfname += begindate1 + " " + begintime1;
                            wfname += timeZoneShow;
                            wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                            wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                            //支持无侵入修改流程信息
                            beforeWfRemind(9,meetingid,wfname,userid,wfremark,wfaccepter,secretLevel,sysRemindWorkflow);
                        }
                        if(delMemberSet.size() > 0){
                            wfaccepter = delMemberSet.stream().collect(Collectors.joining(","));
                            wfname = Util.toMultiLangScreen("518372") + ":";
                            wfname += meetingName;
                            wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                            wfname += begindate1 + " " + begintime1;
                            wfname += timeZoneShow;
                            wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                            wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                            //支持无侵入修改流程信息
                            beforeWfRemind(9,meetingid,wfname,userid,wfremark,wfaccepter,secretLevel,sysRemindWorkflow);
                        }
                        if( keepMemberSet.size() > 0 ){
                            wfaccepter = keepMemberSet.stream().collect(Collectors.joining(","));
                            wfname = Util.toMultiLangScreen("518382") + ":";
                            wfname += meetingName;
                            wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                            wfname += begindate1 + " " + begintime1;
                            wfname += timeZoneShow;
                            wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                            wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                            //支持无侵入修改流程信息
                            beforeWfRemind(9,meetingid,wfname,userid,wfremark,wfaccepter,secretLevel,sysRemindWorkflow);
                        }

                    }
                }
                //删除人员 发送会议取消提醒
                if (delHrm.size() > 0) {
                    wfaccepter = delHrm.toString().substring(1, delHrm.toString().length() - 1).replaceAll("\\s*", "");
                    // 主要作用：将回执的其他人员添加到【发送取消流程】以及【立即提醒】中的人员
                    String delWfAccepter =delHrmSet.stream().collect(Collectors.joining(","));
                    if(delWfAccepter.equals("")){
                        delWfAccepter = wfaccepter;
                    }else{
                        //前面已经判断过如果其他人员中包含参会人员，那么就不添加进去，所以这是不需要过滤
                        delWfAccepter = wfaccepter +","+delWfAccepter;
                    }
                    // 保持原样的人

                    if(meetingSetInfo.getCancelMeetingRemindChk() == 1 ){
                        wfname = Util.toMultiLangScreen("23269") + ":" + meetingName + "-" + new ResourceComInfo().getLastname(user.getUID() + "") + "-" + CurrentDate + timeZoneShow;
                        //sysRemindWorkflow.setMeetingSysRemind(wfname, Util.getIntValue(meetingid), Util.getIntValue(MeetingContacter), wfaccepter, wfremark);
                        //支持无侵入修改流程信息
                        beforeWfRemind(5,meetingid,wfname,userid,wfremark,delWfAccepter,secretLevel,sysRemindWorkflow);
                    }
                    if(remindImmediately == 1 && delHrm.size() > 0 ){
                        remindByThread(meetingid,"cancel",delWfAccepter);
                    }
                    //
                    List params = new ArrayList();
                    Object[] obj = DBUtil.transListIn(wfaccepter,params);
                    params = (List)obj[1];
                    params.add(meetingid);
                    recordSet.executeQuery("select requestid from meeting_member2 where membermanager in ("+obj[0]+") and meetingid = ?",params);
                    while(recordSet.next()){
                        String oldRequestid =recordSet.getString(1);
                        if(!oldRequestid.equals("")){
                            MeetingUtil.deleteWF(oldRequestid,user,"from_changeMeeting");
                        }
                    }
                }


                //新加参会人发送新建会议提醒
                if (newHrm.size() > 0) {
                    wfaccepter = newHrm.toString().substring(1, newHrm.toString().length() - 1).replaceAll("\\s*", "");

                    int subId = Util.getIntValue(new ResourceComInfo().getSubCompanyID(creater)) ;
                    ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
                    boolean Wfdetachable = manageDetachComInfo.isUseWfManageDetach();
                    if(Wfdetachable){
                        if(subId > 0){
                            recordSet.executeQuery("select m.id,m.workflowid from meetingreceipt_bill m,WORKFLOW_BASE w where " +
                                    "m.workflowid = w.id and w.subcompanyid = ? and isOpen = ?",subId,1);
                        }
                    }else{
                        recordSet.executeQuery("select id,workflowid from meetingReceipt_bill where isOpen = ? order by id asc",1);
                    }
                    if(recordSet.next()){
                        String id =recordSet.getString(1);
                        String wfId =recordSet.getString(2);
                        MeetingInterval.createReceiptWf(user,meetingid,wfaccepter,wfId,id,Util.null2String(params.get(ParamConstant.PARAM_IP)),false,false);
                    }else if(meetingSetInfo.getCreateMeetingRemindChk() == 1 ){
                        wfname = Util.toMultiLangScreen("24215") + ":";
                        wfname += meetingName;
                        wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                        wfname += begindate1 + " " + begintime1;
                        wfname += timeZoneShow;
                        wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                        wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                        //sysRemindWorkflow.setMeetingSysRemind(wfname, Util.getIntValue(meetingid), Util.getIntValue(MeetingContacter), wfaccepter, wfremark);
                        //支持无侵入修改流程信息
                        beforeWfRemind(6,meetingid,wfname,userid,wfremark,wfaccepter,secretLevel,sysRemindWorkflow);
                    }
                    if(remindImmediately == 1){
                        remindByThread(meetingid,"create",wfaccepter);
                    }
                }

                //会议主要元素发生变更时 并且开启新建会议提醒时 给未发生变化的人员发送变更提醒
                if (meetingInfoChanged) {
                    wfaccepter = "";
                    for (int i = 0; i < arrmbr.length; i++) {
                        if (newHrm.indexOf(arrmbr[i]) < 0 || newHrm.size() == 0) {//不是新加人员
                            wfaccepter += wfaccepter.equals("") ? arrmbr[i] : "," + arrmbr[i];
                        }
                    }
                    for (int i = 0; i < arrcrm.length; i++) {
                        if (newHrm.indexOf(arrcrm[i]) < 0 || newHrm.size() == 0) {//不是新加客户经理
                            wfaccepter += wfaccepter.equals("") ? arrcrm[i] : "," + arrcrm[i];
                        }
                    }
                    List paramSql = new ArrayList();
                    delHrmSet = new HashSet();
                    Object[] obj = DBUtil.transListIn(wfaccepter,paramSql);
                    paramSql = (List)obj[1];
                    paramSql.add(meetingid);
                    recordSet.executeQuery("select othermember from meeting_member2 where membermanager in ("+obj[0]+") and meetingid = ?",paramSql);
                    while(recordSet.next()){
                        String[] othermbr = recordSet.getString(1).split(",");
                        for (int j = 0; j < othermbr.length; j++) {
                            if (("," + hrmmembers + ",").indexOf("," + othermbr[j] + ",") < 0) {
                                delHrmSet.add(othermbr[j]);
                            }
                        }

                    }
                    //会议变更发送回执流程,对应的流程会在生成的时候对历史requestid进行删除操作的
                    boolean openReceiptBill = false;
                    int subId = Util.getIntValue(new ResourceComInfo().getSubCompanyID(creater)) ;
                    ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
                    boolean Wfdetachable = manageDetachComInfo.isUseWfManageDetach();
                    if(Wfdetachable){
                        if(subId > 0){
                            recordSet.executeQuery("select m.id,m.workflowid from meetingreceipt_bill m,WORKFLOW_BASE w where " +
                                    "m.workflowid = w.id and w.subcompanyid = ? and isOpen = ?",subId,1);
                        }
                    }else{
                        recordSet.executeQuery("select id,workflowid from meetingReceipt_bill where isOpen = ? order by id asc",1);
                    }
                    if(recordSet.next()){
                        openReceiptBill = true;
                        String id =recordSet.getString(1);
                        String wfId =recordSet.getString(2);

                        MeetingInterval.createReceiptWf(user,meetingid,wfaccepter,wfId,id,Util.null2String(params.get(ParamConstant.PARAM_IP)),true,false);
                        String callerAndContacter = "";
                        if((","+wfaccepter+",").indexOf(","+caller+",") < 0){
                            callerAndContacter += callerAndContacter.equals("")? caller : ","+caller;
                        }
                        if((","+wfaccepter+",").indexOf(","+contacter+",") < 0){
                            callerAndContacter += callerAndContacter.equals("")? contacter: ","+contacter;
                        }
                        if(!callerAndContacter.equals("")){
                            wfname = Util.toMultiLangScreen("24574") + ":";
                            wfname += meetingName;
                            wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                            wfname += begindate1 + " " + begintime1;
                            wfname += timeZoneShow;
                            wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                            wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                            //支持无侵入修改流程信息
                            beforeWfRemind(7,meetingid,wfname,userid,wfremark,callerAndContacter,secretLevel,sysRemindWorkflow);
                        }

                    }else if(meetingSetInfo.getCreateMeetingRemindChk() == 1){
                        if((","+wfaccepter+",").indexOf(","+caller+",") < 0){
                            wfaccepter += ","+caller;
                        }
                        if((","+wfaccepter+",").indexOf(","+contacter+",") < 0){
                            wfaccepter += ","+contacter;
                        }
                        wfname = Util.toMultiLangScreen("24574") + ":";
                        wfname += meetingName;
                        wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                        wfname += begindate1 + " " + begintime1;
                        wfname += timeZoneShow;
                        wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                        wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                        //sysRemindWorkflow.setMeetingSysRemind(wfname, Util.getIntValue(meetingid), Util.getIntValue(MeetingContacter), wfaccepter, wfremark);
                        //支持无侵入修改流程信息
                        beforeWfRemind(7,meetingid,wfname,userid,wfremark,wfaccepter,secretLevel,sysRemindWorkflow);
                    }
                    if(!openReceiptBill){
                        //清空人员信息
                        //为什么将方法放入这里，是因为在开启回执流程是在线程中，线程单独处理，这里因为两个线程有执行时间交错原因，所以单独处理
                        MeetingUtil.clearMemberInfo(meetingid,wfaccepter);
                    }
                    if(meetingSetInfo.getCancelMeetingRemindChk() == 1 && delHrmSet.size() > 0){
                        // 现在逻辑：变更后给保持原样的人员选择的其他人员给删除掉，这样就需要将【回执人员】的【回执流程】删除以及发送【取消流程】
                        wfname = Util.toMultiLangScreen("23269") + ":" + meetingName + "-" + new ResourceComInfo().getLastname(user.getUID() + "") + "-" + CurrentDate + timeZoneShow;
                        String cancelAccepter = delHrmSet.stream().collect(Collectors.joining(","));
                        //支持无侵入修改流程信息
                        beforeWfRemind(5,meetingid,wfname,userid,wfremark,cancelAccepter,secretLevel,sysRemindWorkflow);
                    }
                    //给会议服务人员发送默认提醒工作流
                    Set<String> hrmidSet=new HashSet<String>();
                    String hrmid="";
                    String[] hrmids=null;
                    recordSet.executeQuery("select hrmids from Meeting_Service_New where meetingid=?",meetingid);
                    while(recordSet.next()){
                        hrmid=recordSet.getString(1);
                        if(hrmid!=null&&!"".equals(hrmid)){
                            hrmids=hrmid.split(",");
                            for(String tempid:hrmids){
                                if(!"".equals(tempid)) hrmidSet.add(tempid);
                            }
                        }
                    }
                    if(hrmidSet.size()>0){
                        String serviceHrmids = hrmidSet.stream().collect(Collectors.joining(","));
                        wfname = Util.toMultiLangScreen("513102") + ":";
                        wfname += meetingName;
                        wfname += " " + SystemEnv.getHtmlLabelName(81901, user.getLanguage()) + ":";
                        wfname += begindate1 + " " + begintime1;
                        wfname += timeZoneShow;
                        wfname += " " + SystemEnv.getHtmlLabelName(2105, user.getLanguage()) + ":";
                        wfname += meetingRoomComInfo.getMeetingRoomInfoname("" + address1) + customizeAddress;
                        //sysRemindWorkflow.setMeetingSysRemind(wfname, Util.getIntValue(meetingid), Util.getIntValue(MeetingContacter), serviceHrmids, wfremark);
                        //支持无侵入修改流程信息
                        beforeWfRemind(8,meetingid,wfname,userid,wfremark,serviceHrmids,secretLevel,sysRemindWorkflow);
                    }
                    if(remindImmediately == 1){
                        // qc818733:变更时，不能修改自定义字段以及服务字段（议程负责人不需要发送）
                        // 所以在立即提醒时只需要将会议信息自定义提醒字段以及服务字段中人员添加进去就可以
                        // 对当前人员如果在发送人中，那么就不发送
                        // 咱不考虑人员重复的情况
                        Set<String> reminderSet = new LinkedHashSet<String>();
                        Set fieldIdsSet = new HashSet();
                        //获取field和reminder
                        MeetingRemindUtil.SetRemindAndFields(meetingid,fieldIdsSet,reminderSet);
                        fieldIdsSet.remove("29");//移除参会人
                        fieldIdsSet.remove("32");//移除参会客户
                        //处理会议信息字段
                        MeetingRemindUtil.setReminderSet(meetingid,reminderSet,fieldIdsSet,1);
                        //处理会议服务字段
                        MeetingRemindUtil.setReminderSet(meetingid,reminderSet,fieldIdsSet,3);
                        Arrays.asList(wfaccepter.split(",")).stream().forEach(item->{
                            reminderSet.add(item);
                        });
                        remindByThread(meetingid,"change",reminderSet.stream().filter(item->!item.equals(userid)).collect(Collectors.joining(",")));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        try {
            MeetingViewer meetingViewer = new MeetingViewer();
            MeetingComInfo meetingComInfo = new MeetingComInfo();
            MeetingUtil meetingUtil = new MeetingUtil();
            //保存自定义字段
            MeetingFieldManager mfm = new MeetingFieldManager(1);
            mfm.editCustomData(params, Util.getIntValue(meetingid));
            meetingViewer.setMeetingShareById("" + meetingid);
            //文档和附件的共享明细
            meetingUtil.meetingDocShare(meetingid);
            //设置附件等级
            MeetingUtil.setAccessorySecretLevel(meetingid, secretLevel, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        new MeetingComInfo().removeMeetingInfoCache();
        MeetingLog meetingLog = new MeetingLog();
        meetingLog.resetParameter();
        meetingLog.insSysLogInfo(user, Util.getIntValue(meetingid), meetingname, "变更会议", "303", "2", 1, Util.null2String(ParamConstant.PARAM_IP));

        //删除未触发的提醒
        recordSet.execute("delete FROM meeting_remind where meeting='" + meetingid + "'");
//		//删除日程
//		recordSet.execute("select id from workplan where meetingid = '"+meetingid+"'");
//
//		while(recordSet.next()){
//			weaver.WorkPlan.WorkPlanHandler wph = new weaver.WorkPlan.WorkPlanHandler();
//			wph.delete(recordSet.getString("id"));
//		}
        //重新生成新的提醒和日程
        try {
            MeetingInterval.createWPAndRemind(meetingid, null, Util.null2String(ParamConstant.PARAM_IP), false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //exchange相关
        //从exchange过来的，无需再过去
        if (StringUtils.isBlank(ewsid) && !"exchange".equals(isFrom)) {
            ThreadPoolUtil.getThreadPool("MeetingToEWS", "5").execute(() -> {
                MeetingExchangeUtil meetingExchangeUtil = new MeetingExchangeUtil();
                if (meetingExchangeUtil.canUseExchange()) {
                    meetingExchangeUtil.doMeeingToEWS(meetingid, creater, 2);
                }
            });
        }
        MeetingBaseService meetingBaseService =  ServiceUtil.getService(MeetingBaseServiceImpl.class, user);
        params.put("beforeChangeMeetingInfo",beforeChangeMeetingInfo);
        meetingBaseService.afterMeetingChange(params);
        MeetingUtil.updateModifyDateTime(meetingid);

        ret.put("status", true);
        ret.put("meetingid", meetingid);
//        //入库后logger取得
//        List<BizLogContext> bizLogListContext = logger.getBizLogContexts();
//        if (bizLogListContext.size() > 0) {
//            LogUtil.writeBizLog(bizLogListContext.get(0));
//        }
        return ret;
    }

    /**
     * 线程提醒方式
     * @param meetingid
     * @param accepter
     */
    private static void remindByThread(final String meetingid,final String mode,final String accepter){
        new Thread(){
            public void run() {
                MeetingRemindUtil.remindImmediately(meetingid,mode,accepter);
            }
        }.start();
    }

    /**
     * 获取无侵入流程提醒数据
     * @param type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）5:会议取消提醒（来源于会议变更）
     *               6：会议创建提醒（来源于会议变更） 7：会议变更提醒（原参会人）  8：会议服务变更提醒
     * @param meetingId
     * @param wfTitle 流程标题
     * @param wfCreater 流程创建人
     * @param wfRemark 流程签字意见
     * @param wfAccepter 流程接收人
     * @param wfSecretLevel 流程密级
     * @param sysRemindWorkflow
     * @return
     */
    private void beforeWfRemind(int type,String meetingId,String wfTitle,String wfCreater,String wfRemark,String wfAccepter,String wfSecretLevel,SysRemindWorkflow sysRemindWorkflow) {
        Map<String,Object> retMap=MeetingRemindUtil.beforeWfRemind(type,meetingId,wfTitle,wfCreater,wfRemark,wfAccepter,wfSecretLevel);
        boolean executeStandardBusiness=true;
        if(null!=retMap){
            wfTitle=Util.null2String(retMap.get("wfTitle"));
            wfCreater=Util.null2String(retMap.get("wfCreater"));
            wfAccepter=Util.null2String(retMap.get("wfAccepter"));
            wfRemark=Util.null2String(retMap.get("wfRemark"));
            wfSecretLevel=Util.null2String(retMap.get("wfSecretLevel"));
            executeStandardBusiness=!"false".equals(Util.null2String(retMap.get("executeStandardBusiness")));
        }

        if(executeStandardBusiness){
            try {
                sysRemindWorkflow.setMeetingSysRemind(wfTitle, Util.getIntValue(meetingId), Util.getIntValue(wfCreater), wfAccepter, wfRemark);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
