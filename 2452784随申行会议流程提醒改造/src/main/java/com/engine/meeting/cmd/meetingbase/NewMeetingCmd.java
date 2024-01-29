package com.engine.meeting.cmd.meetingbase;

import com.api.meeting.service.MeetingBaseService;
import com.api.workplan.util.TimeZoneCastUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Meeting;
import com.engine.common.constant.BizLogType;
import com.engine.common.constant.ParamConstant;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.meeting.service.impl.MeetingBaseServiceImpl;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.meeting.service.MeetingFieldService;
import com.engine.meeting.service.MeetingSignService;
import com.engine.meeting.service.impl.MeetingFieldServiceImpl;
import com.engine.meeting.service.impl.MeetingSignServiceImpl;
import com.engine.meeting.service.impl.MeetingTypeServiceImpl;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.encrypt.EncryptUtil;
import weaver.file.FileUpload;
import weaver.general.ThreadPoolUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.Maint.MeetingComInfo;
import weaver.meeting.Maint.MeetingInterval;
import weaver.meeting.Maint.MeetingRoomComInfo;
import weaver.meeting.Maint.MeetingSetInfo;
import weaver.meeting.MeetingLog;
import weaver.meeting.MeetingUtil;
import weaver.meeting.MeetingViewer;
import weaver.meeting.defined.MeetingCreateWFUtil;
import weaver.meeting.defined.MeetingFieldManager;
import weaver.meeting.defined.MeetingWFComInfo;
import weaver.meeting.defined.MeetingWFUtil;
import weaver.meeting.util.exchange.MeetingExchangeUtil;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.request.RequestManager;
import weaver.workflow.workflow.WFManager;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class NewMeetingCmd extends AbstractCommonCommand<Map<String, Object>> {

    private SimpleBizLogger logger;
    private BizLogContext bizLogContext;

    public NewMeetingCmd(User user, Map<String, Object> params) {
        this.user = user;
        this.params = params;
        this.logger = new SimpleBizLogger();
        this.bizLogContext=new BizLogContext();
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

	private com.engine.meeting.service.MeetingBaseService getService(User user) {
        return (MeetingBaseServiceImpl) ServiceUtil.getService(MeetingBaseServiceImpl.class, user);
    }
	
    public void beforeLog(){
        //入库前logger取得
        bizLogContext.setDateObject(new Date());
        bizLogContext.setUserid(user.getUID());
        bizLogContext.setLogType(BizLogType.MEETING);
        bizLogContext.setUsertype(Util.getIntValue(user.getLogintype()));
        bizLogContext.setTargetName(Util.null2String(params.get("name")));
        bizLogContext.setLogType(BizLogType.MEETING);
        bizLogContext.setBelongTypeTargetId(bizLogContext.getTargetId());//所属类型id
        bizLogContext.setBelongType(BizLogSmallType4Meeting.MEETING_BASE);
        bizLogContext.setLogSmallType(BizLogSmallType4Meeting.MEETING_BASE);
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.putAll(params);
        paramMap.remove("request");
        bizLogContext.setParams(paramMap);
        bizLogContext.setClientIp(Util.null2String(params.get(ParamConstant.PARAM_IP)));
        bizLogContext.setBelongTypeTargetName("meeting");
//        SimpleBizLogger logger = new SimpleBizLogger();
        logger.setUser(user);//当前操作人
        logger.setMainSql("SELECT * FROM meeting where id = "+bizLogContext.getTargetId(), "id");//主表sql
        logger.setMainTargetNameColumn("name");//当前targetName对应的列（对应日志中的对象名）
        logger.before(bizLogContext);
    }

    public List<BizLogContext> getLogContexts()  {
        return logger.getBizLogContexts();
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        beforeLog();

        String ClientIP = Util.null2String(params.get(ParamConstant.PARAM_IP));

        String userid = user.getUID() + "";
        HttpServletRequest request = (HttpServletRequest) params.get("request");
        Date newdate = new Date();
        long datetime = newdate.getTime();
        boolean retstatus = true;
        Timestamp timestamp = new Timestamp(datetime);
        MeetingBaseService mbs = new MeetingBaseService();
        String CurrentDate = (timestamp.toString()).substring(0, 4) + "-" + (timestamp.toString()).substring(5, 7) + "-" + (timestamp.toString()).substring(8, 10);
        String CurrentTime = (timestamp.toString()).substring(11, 13) + ":" + (timestamp.toString()).substring(14, 16) + ":" + (timestamp.toString()).substring(17, 19);
        Map ret = new HashMap();
        MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
        MeetingViewer meetingViewer = new MeetingViewer();
        MeetingComInfo meetingComInfo = new MeetingComInfo();
        MeetingUtil meetingUtil = new MeetingUtil();
        RecordSet recordSet = new RecordSet();

        int isquick = Util.getIntValue(Util.null2String(params.get("isquick")), 0);//是否来自快速占用
        String method = Util.null2String(params.get("method"));
        String isFrom = Util.null2String(params.get("isFrom"));
        String meetingtype = Util.null2String(params.get("meetingtype"));
        String name = Util.null2String(params.get("name"));//会议名称
        String caller = Util.null2String(params.get("caller"));//召集人,必填
        String contacter = Util.null2String(params.get("contacter"));//联系人,空值使用当前操作人
        String ewsid = Util.null2String(params.get("ewsid"));//exchange的ewsid
        String ewsUpdateDate = Util.null2String(params.get("ewsUpdateDate"));//ewsUpdateDate
        if (caller.isEmpty()) {//召集人为空时 默认设置为当前人员
            caller = userid;
        }
        // 联系人在为空的清空下，不做默认当前人处理，，召集人和参会人员/分部/部门为空情况下，给予当前操作人的默认值处理
//        if ("".equals(contacter)) contacter = userid;
        int roomType = 1;
        String address = Util.null2String(params.get("address"));//会议地点
        String customizeAddress = Util.null2String(params.get("customizeAddress"));
        if (!"".equals(address)) {//优先选择会议室
//            customizeAddress = "";
        } else {//自定义会议室
            roomType = 2;
        }
        String desc = Util.htmlFilter4UTF8(Util.null2String(params.get("desc_n")));//描述,可为空
        String secretLevel = Util.null2String(Util.null2String(params.get("secretLevel")), MeetingUtil.DEFAULT_SECRET_LEVEL);//密级,必填
        String secretDeadline = Util.null2String(Util.null2String(params.get("secretDeadline")));//保密期限
        if (secretLevel.equals("")) {
            secretLevel = MeetingUtil.DEFAULT_SECRET_LEVEL;
        }
        //时间
        int repeatType = Util.getIntValue(Util.null2String(params.get("repeatType")), 0);//是否是重复会议,0 正常会议.
        String begindate = Util.null2String(params.get("begindate"));
        String enddate = Util.null2String(params.get("enddate"));
		if("".equals(enddate)){
			enddate = begindate;
		}
        if (repeatType > 0) {
            begindate = Util.null2String(params.get("repeatbegindate"));
            enddate = Util.null2String(params.get("repeatenddate"));
        }
        String begintime = Util.null2String(params.get("begintime"));
        String endtime = Util.null2String(params.get("endtime"));
        /* ----------新增日期转换 start ----------------*/
        String changeToB[] = TimeZoneCastUtil.FormatDateServer(begindate + " " + begintime, 0);
        String changeToE[] = TimeZoneCastUtil.FormatDateServer(enddate + " " + endtime, 1);
        begindate = changeToB[0];
        begintime = changeToB[1];
        enddate = changeToE[0];
        endtime = changeToE[1];
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
        int isEnableSecondAuth = Util.getIntValue(Util.null2String(params.get("isEnableSecondAuth")), 0);  //是否开启二次身份验证
        //参会人员
        String hrmmembers = Util.null2String(params.get("hrmmembers"));//参会人员
        String hrmDepartments = Util.null2String(params.get("hrmDepartments"));//参会部门
        String hrmSubCompanys = Util.null2String(params.get("hrmSubCompanys"));//参会分部
        int totalmember = Util.getIntValue(Util.null2String(params.get("totalmember")), 1);//参会人数
        if (hrmmembers.isEmpty() && hrmSubCompanys.isEmpty() && hrmDepartments.isEmpty()) {//参会人为空时 默认设置为当前人员
            hrmmembers = userid;
            //优化新建时如果没有人员填写，这里会默认给当前人员，所以需要设置为1
            if(totalmember < 1){
                totalmember = 1;
            }

        }
        String othermembers = Util.fromScreen(Util.null2String(params.get("othermembers")), user.getLanguage());//其他参会人员
        String crmmembers = Util.null2String(params.get("crmmembers"));//参会客户
        int crmtotalmember = Util.getIntValue(Util.null2String(params.get("crmtotalmember")), 0);//参会人数
        //其他信息
        String projectid = Util.null2String(params.get("projectid"));    //加入了项目id
        String accessorys = meetingUtil.getPermissionDocIds(Util.null2String(params.get("accessorys")), user);    //系统附件
        //自定义字段
        int remindType = 1;  //老的提醒方式,默认1不提醒
        //重复策略字段
        int repeatdays = Util.getIntValue(Util.null2String(params.get("repeatdays")), 0);
        int repeatweeks = Util.getIntValue(Util.null2String(params.get("repeatweeks")), 0);
        String rptWeekDays = Util.null2String(params.get("rptWeekDays"));
        int repeatmonths = Util.getIntValue(Util.null2String(params.get("repeatmonths")), 0);
        int repeatmonthdays = Util.getIntValue(Util.null2String(params.get("repeatmonthdays")), 0);
        int repeatStrategy = Util.getIntValue(Util.null2String(params.get("repeatStrategy")), 0);
        if (isquick == 1) {
            caller = userid;
            hrmmembers = userid;
        }
        char flag = 2;
        String ProcPara = "";
        String description= SystemEnv.getHtmlLabelName(10000849, Util.getIntValue(user.getLanguage()))+":"+name+" "+SystemEnv.getHtmlLabelName(81901,user.getLanguage())+":"+begindate+" "+begintime+" "+SystemEnv.getHtmlLabelName(2105,user.getLanguage())+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
        if(StringUtils.isBlank(meetingtype)){
            meetingtype = null;
        }
        if(StringUtils.isBlank(projectid)){
            projectid = null;
        }
        ProcPara = meetingtype;
        ProcPara += flag + name;
        ProcPara += flag + caller;
        ProcPara += flag + contacter;
        ProcPara += flag + projectid; //加入项目id
        ProcPara += flag + address;
        ProcPara += flag + begindate;
        ProcPara += flag + begintime;
        ProcPara += flag + enddate;
        ProcPara += flag + endtime;
        ProcPara += flag + desc;
        ProcPara += flag + userid;
        ProcPara += flag + CurrentDate;
        ProcPara += flag + CurrentTime;
        ProcPara += flag + "" + totalmember;
        ProcPara += flag + othermembers;
        ProcPara += flag + "";
        ProcPara += flag + description;
        ProcPara += flag + "" + remindType;
        ProcPara += flag + "" + remindBeforeStart;
        ProcPara += flag + "" + remindBeforeEnd;
        ProcPara += flag + "" + remindTimesBeforeStart;
        ProcPara += flag + "" + remindTimesBeforeEnd;
        ProcPara += flag + customizeAddress;
        String uuid = UUID.randomUUID().toString();
        ProcPara += flag + uuid;
        if (recordSet.getDBType().equals("oracle") || recordSet.getDBType().equalsIgnoreCase("mysql")|| recordSet.getDBType().equalsIgnoreCase("postgresql")) {
            recordSet.executeProc("Meeting_Insert", ProcPara);
        } else {
            recordSet.executeProc("Meeting_Insert", ProcPara);
        }
        recordSet.executeQuery("SELECT id FROM Meeting where uuid = ?", uuid);
        recordSet.next();
        String MaxID = recordSet.getString(1);
        RecordSet lock = new RecordSet();
		if (!"save".equals(method)) {
			lock.executeUpdate("Update Meeting Set meetingstatus = 7 WHERE id=?", MaxID);//更新会议状态为提交锁定状态
		}

        List updateValueList = new ArrayList();
        updateValueList.add(repeatType);
        updateValueList.add(repeatdays);
        updateValueList.add(repeatweeks);
        updateValueList.add(rptWeekDays);
        updateValueList.add(begindate);
        updateValueList.add(enddate);
        updateValueList.add(repeatmonths);
        updateValueList.add(repeatmonthdays);
        updateValueList.add(repeatStrategy);
        updateValueList.add(roomType);
        updateValueList.add(secretLevel);
        updateValueList.add(secretDeadline);
        updateValueList.add(remindTypeNew);
        updateValueList.add(remindImmediately);
        updateValueList.add(remindHoursBeforeStart);
        updateValueList.add(remindHoursBeforeEnd);

        String updateSql = "update Meeting set repeatType = ? "
                + " , repeatdays = ? "
                + " , repeatweeks = ?"
                + " , rptWeekDays = ?"
                + " , repeatbegindate = ? "
                + " , repeatenddate = ? "
                + " , repeatmonths = ?"
                + " , repeatmonthdays = ?"
                + " , repeatStrategy = ?"
                + " , roomType = ?"
                + " , secretLevel = ? "
                + " , secretDeadline = ? "
                + " , remindTypeNew = ? "
                + " , remindImmediately = ?"
                + " , remindHoursBeforeStart = ?"
                + " , remindHoursBeforeEnd = ?";
        if (recordSet.getDBType().equalsIgnoreCase("oracle")&&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")) {
            updateSql += " , hrmmembers = empty_clob() ";
        } else {
            updateSql += " , hrmmembers = ? ";
            updateValueList.add(hrmmembers);
        }
        updateValueList.add(crmmembers);
        updateValueList.add(crmtotalmember);
        updateValueList.add(accessorys);
        updateValueList.add(hrmSubCompanys);
        updateValueList.add(hrmDepartments);
        updateValueList.add(MaxID);
        updateSql += " , crmmembers = ? "
                + " , crmtotalmember = ?"
                + " , accessorys = ? "
                + " , hrmSubCompanys = ? "
                + " , hrmDepartments = ? "
                + " where id = ?";
        recordSet.executeUpdate(updateSql, updateValueList);
        //对密级进行加密
        HrmClassifiedProtectionBiz hrmClassifiedProtectionBiz = new HrmClassifiedProtectionBiz();
        //是否启用分级保护
        boolean isOpenSecret = hrmClassifiedProtectionBiz.isOpenClassification();
        String encKey = "";
        String crc = "";
        if(isOpenSecret && (!"".equals(secretLevel))){
            EncryptUtil encryptUtil = new EncryptUtil();
            Map<String,String> map = encryptUtil.getLevelCRC(MaxID,secretLevel);
            encKey = map.get("encKey");
            crc = map.get("crc");
        }
        RecordSet rs = new RecordSet();
        rs.executeUpdate("update meeting set encKey = ?,crc = ? where id = ? ", encKey,crc,MaxID);

        if (recordSet.getDBType().equalsIgnoreCase("oracle") && Util.null2String(recordSet.getOrgindbtype()).equals("oracle")) {
            meetingUtil.updateHrmmembers(MaxID, hrmmembers);
        }
        bizLogContext.setTargetId(MaxID);
        bizLogContext.setOperateType(BizLogOperateType.ADD);
        logger.setMainSql("SELECT * FROM meeting where id = "+bizLogContext.getTargetId(), "id");//主表sql
        try {
            //保存自定义字段
            MeetingFieldManager mfm = new MeetingFieldManager(1);
            mfm.filterDocAndRequestIds(params, user);
            mfm.editCustomData(params, Util.getIntValue(MaxID));
            //更新会议共享以及查看状态
            MeetingUtil.updateMM2andMV(userid,MaxID);

            //会议议程
            int topicrows = Util.getIntValue(Util.null2String(params.get("topicrows")), 0);
            if (topicrows > 0) {
                MeetingFieldManager mfm2 = new MeetingFieldManager(2);
                mfm.filterDocAndRequestIds(params, user);
                for (int i = 1; i <= topicrows; i++) {
                    mfm2.editCustomDataDetail(params, user, 0, i, Util.getIntValue(MaxID));
                }
            }
            //会议服务
            int servicerows = Util.getIntValue(Util.null2String(params.get("servicerows")), 0);
            if (servicerows > 0) {
                MeetingFieldManager mfm3 = new MeetingFieldManager(3);
                for (int i = 1; i <= servicerows; i++) {
                    mfm3.editCustomDataDetail(params, user, 0, i, Util.getIntValue(MaxID));
                }
            }

            meetingViewer.setMeetingShareById("" + MaxID);
            meetingComInfo.removeMeetingInfoCache();
            //文档和附件的共享明细
            meetingUtil.meetingDocShare(MaxID);
            //设置附件等级

            MeetingUtil.setAccessorySecretLevel(MaxID, secretLevel, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //更新是否开启二次身份校验
        recordSet.executeUpdate("Update Meeting Set isEnableSecondAuth = ? WHERE id=?", isEnableSecondAuth,MaxID);

        if ("save".equals(method)) {

            ret.put("meetingid", MaxID);
            ret.put("status", true);
            ret.put("retstatus", retstatus);
            return ret;
        }
        Map<String, Object> paramswf = new HashMap<String, Object>();
        paramswf.put("meetingtype", meetingtype);
        paramswf.put("repeattype", repeatType);

        MeetingTypeServiceImpl meetingTypeService = (MeetingTypeServiceImpl) ServiceUtil.getService(MeetingTypeServiceImpl.class, new User());
        Map<String, Object> m = meetingTypeService.getApproveWFId(paramswf);

        //Map<String, String> m = new MeetingTypeService().getApproveWFId(meetingtype, repeatType);
        String approvewfid = Util.null2String(m.get("approvewfid"));
        String formid = Util.null2String(m.get("formid"));

        /**
         * 存在审批流程，触发相应流程，否则直接更新会议为正常状态
         */
        if (!approvewfid.equals("0") && !approvewfid.equals("") && !"exchange".equals(isFrom)) {
            //公共方法已经获取了一次最新版本流程，这里不再重复获取
            //approvewfid=WorkflowVersion.getActiveVersionWFID(approvewfid);
            lock.executeUpdate("Update Meeting Set meetingstatus = 0 WHERE id=?", MaxID);//存在审批流程，修改会议状态从锁定到草稿状态
            MeetingLog meetingLog = new MeetingLog();
            meetingLog.resetParameter();
            meetingLog.insSysLogInfo(user, Util.getIntValue(MaxID), name, "新建审批会议", "303", "1", 1, ClientIP);
            //判断85系统表单的情况下是否在workflow_bill中设置了calzz
            boolean canSystemBill = false;
            recordSet.executeSql("select clazz from workflow_bill where id = 85");
            if (recordSet.next()) {
                if (!recordSet.getString("clazz").equals("")) {
                    canSystemBill = true;
                }
            }
            //表单id为85并且没有设置clazz的情况下还是走原来的系统表单逻辑
            if ("85".equals(formid) && !canSystemBill) {//原系统表单

                Map<String, Object> paramsbase = new HashMap<>();
                paramsbase.put("meetingid",MaxID);
                paramsbase.put("approvewfId",approvewfid);
                try {
                    ret=submitMeetingWF(user,MaxID,approvewfid,request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ret.put("meetingid", MaxID);
                ret.put("status", true);
            } else {//新表单,通过Action统一处理

                String errMessagecontent = MeetingCreateWFUtil.createWF(MaxID, user, approvewfid, ClientIP);
                errMessagecontent = errMessagecontent.replaceAll("</?[^>]+>", "");
                if (!errMessagecontent.isEmpty()) {
                    ret.put("error", errMessagecontent);
                    ret.put("showMsg", errMessagecontent);
                    ret.put("status", false);
                    ret.put("meetingid", MaxID);
                    ret.put("retstatus", retstatus);
                    return ret;
                }
                ret.put("status", true);
                ret.put("meetingid", MaxID);
            }
        } else {

            MeetingLog meetingLog = new MeetingLog();
			com.engine.meeting.service.MeetingBaseService base = getService(user);
			params.put("meetingid",MaxID);
            Map chkMap = base.chkRoom(params);
            MeetingSetInfo meetingSetInfo = new MeetingSetInfo();
            int roomconflictchk=meetingSetInfo.getRoomConflictChk();
            int roomconflict =meetingSetInfo.getRoomConflict();
            List roomList = (List)chkMap.get("list");
            List idList = (List)chkMap.get("idlist");
            idList.add(MaxID);//将当前会议id添加到idlist中
            List statusList = (List)chkMap.get("statuslist");
            boolean status = true;
            if(roomList.size()>0 && roomconflictchk==1 && roomconflict==2){//说明存在冲突，且开启会议室冲突不能提交
                if((statusList.contains("1") || statusList.contains("2"))){//冲突的会议中存在待审批或者正常状态的会议，直接更新为草稿状态
                    retstatus = false;
                    recordSet.execute("Update Meeting Set meetingstatus = 0 WHERE id=" + MaxID);//更新会议状态为草稿
                }else{//说明冲突的会议状态只存在7，即为锁定状态，同一时间提交的会议，则去判断当前会议id是否为所有会议里最小的id，是则更新为正常状态，反之为更新为草稿状态
                    if(MaxID.equals(getMinId(idList))){
                        recordSet.execute("Update Meeting Set meetingstatus = 2 WHERE id=" + MaxID);//更新会议状态为正常
                    }else{
                        retstatus = false;
                        recordSet.execute("Update Meeting Set meetingstatus = 0 WHERE id=" + MaxID);//更新会议状态为草稿
                    }
                }
            }else{//不存在冲突，直接更新为正常状态会议
                recordSet.execute("Update Meeting Set meetingstatus = 2 WHERE id=" + MaxID);//更新会议状态为正常
            }
            
            if (repeatType == 0) {
                meetingLog.resetParameter();
                meetingLog.insSysLogInfo(user, Util.getIntValue(MaxID), name, "新建正常会议", "303", "1", 1, ClientIP);
                //生成会议日程和会议提醒
                try {
                    //exchange相关
                    if(StringUtils.isNotBlank(ewsid)){
                        recordSet.executeUpdate("update meeting set ewsid = ?,ewsupdatedate = ? where id = ?", ewsid, ewsUpdateDate, MaxID);
                    }
                    MeetingInterval.createWPAndRemind(MaxID, null, ClientIP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                meetingLog.resetParameter();
                meetingLog.insSysLogInfo(user, Util.getIntValue(MaxID), name, "新建会议模板", "303", "1", 1, ClientIP);
                int intervaltime = 0;
                String otherinfo = "";
                if (repeatType == 1) {
                    intervaltime = repeatdays;
                } else if (repeatType == 2) {
                    intervaltime = repeatweeks;
                    otherinfo = rptWeekDays;
                } else if (repeatType == 3) {
                    intervaltime = repeatmonths;
                    otherinfo = "" + repeatmonthdays;
                }
                int days = new MeetingSetInfo().getDays();
                MeetingInterval.updateMeetingRepeat(days, MaxID, begindate, enddate, "" + repeatType, intervaltime, otherinfo, repeatStrategy);
            }
            ret.put("status", true);
            ret.put("meetingid", MaxID);
        }
        //新加日志结束
        //入库后logger取得
//        List<BizLogContext> bizLogListContext = logger.getBizLogContexts();
//        if (bizLogListContext.size() > 0) {
//            LogUtil.writeBizLog(bizLogListContext.get(0));
//        }
        //获得api结束时间
        //获得api结束时间
        long afterTime = new Date().getTime();
        ret.put("apiCost", afterTime - datetime);
        //会议签到
        if(params.containsKey("defaultAllowSignTime")){
            MeetingSignService meetingSignService = (MeetingSignServiceImpl) ServiceUtil.getService(MeetingSignServiceImpl.class, user);
            params.put("meetingid",MaxID);
            meetingSignService.saveSignCreateSet(params);
        }else if(isquick == 1){ // 快速新建功能，将会议签到默认设置添加进去
            rs.execute("select * from MeetingSet order by id");
            if (rs.next()) {
                MeetingSignService meetingSignService = (MeetingSignServiceImpl) ServiceUtil.getService(MeetingSignServiceImpl.class, user);
                int defaultAllowSignTime = Util.getIntValue(rs.getString("defaultAllowSignTime"), 0);
                int allowSignBack = Util.getIntValue(rs.getString("allowSignBack"));
                int afterSignCanBack = Util.getIntValue(rs.getString("afterSignCanBack"));
                int defaultAllowSignBackTime = Util.getIntValue(rs.getString("defaultAllowSignBackTime"));
                Map saveSignSetMap = new HashMap();
                saveSignSetMap.put("defaultAllowSignTime",defaultAllowSignTime);
                saveSignSetMap.put("allowSignBack",allowSignBack);
                saveSignSetMap.put("afterSignCanBack",afterSignCanBack);
                saveSignSetMap.put("defaultAllowSignBackTime",defaultAllowSignBackTime);
                saveSignSetMap.put("meetingid",MaxID);
                meetingSignService.saveSignCreateSet(saveSignSetMap);
            }
        }
        ret.put("retstatus", retstatus);
        return ret;
    }

    /**
     * 提交会议流程
     *
     * @param user
     * @param meetingId
     * @param approvewfId
     * @param request
     * @return
     * @throws Exception
     */
    public Map submitMeetingWF(User user, String meetingId, String approvewfId, HttpServletRequest request) throws Exception {
        Map ret = new HashMap();
        RecordSet recordSet = new RecordSet();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentdate = sdf.format(new Date()).split(" ")[0];
        String currenttime = sdf.format(new Date()).split(" ")[1];
        int workflowid = -1;
        int nodeid = -1;
        String nodetype = "";
        String meetingname = "";
        recordSet.executeProc("Meeting_SelectByID", meetingId);
        recordSet.next();
        meetingname = recordSet.getString("name");
        String meetingtype = recordSet.getString("meetingtype");
        int repeatType = Util.getIntValue(recordSet.getString("repeatType"), 0);
        int requestid = Util.getIntValue(recordSet.getString("requestid"), -1);

        if (!approvewfId.equals(""))
            workflowid = Integer.valueOf(approvewfId).intValue();

        int formid = -1;

        if (workflowid != -1) {
            recordSet.executeProc("workflow_Workflowbase_SByID", workflowid + "");
            if (recordSet.next()) {
                formid = recordSet.getInt("formid");
            }
            if (nodeid == -1) {
                recordSet.executeProc("workflow_CreateNode_Select", workflowid + "");
                if (recordSet.next())
                    nodeid = recordSet.getInt(1);
                nodetype = "0";
            }
        }
        //判断85系统表单的情况下是否在workflow_bill中设置了calzz
        boolean canSystemBill = false;
        recordSet.executeSql("select clazz from workflow_bill where id = 85");
        if (recordSet.next()) {
            if (!recordSet.getString("clazz").equals("")) {
                canSystemBill = true;
            }
        }
        if ((formid != -1 && formid != 85) || canSystemBill) {
            //会议自定义表单,肯定是从会议卡片来
            String errmsg = "";
            if (requestid == -1) {//创建
                if (workflowid != -1) {
                    errmsg = MeetingCreateWFUtil.createWF(meetingId, user, "" + workflowid, Util.getIpAddr(request));
                }
            } else {//流转
                errmsg = MeetingCreateWFUtil.nextNodeBySubmit(requestid, meetingId, user, "" + formid, Util.getIpAddr(request));
            }
            errmsg = errmsg.replaceAll("</?[^>]+>", "");
            if (!errmsg.isEmpty()) {
                ret.put("status", false);
                ret.put("error", errmsg);
                ret.put("showError", errmsg);
            } else {
                ret.put("status", true);
                ret.put("meetingid", meetingId);
            }
            return ret;
        } else {
            recordSet.executeQuery("Select * From Meeting WHERE ID=" + meetingId);
            if (recordSet.next()) {
                meetingname = Util.null2String(recordSet.getString("name"));
            }
            MeetingLog meetingLog = new MeetingLog();
            meetingLog.resetParameter();
            meetingLog.insSysLogInfo(user, Util.getIntValue(meetingId), meetingname, "" + SystemEnv.getHtmlLabelName(84488, user.getLanguage()), "303", "2", 1, request.getRemoteAddr());
            String requestname = SystemEnv.getHtmlLabelName(16419,
                    user.getLanguage())
                    + "-" + meetingname;
            String remark = new ResourceComInfo().getResourcename(""
                    + user.getUID())
                    + sdf.format(new Date());
            RequestManager RequestManager = new RequestManager();
            RequestManager.setSrc("submit");
            RequestManager.setIscreate("1");
            RequestManager.setRequestid(-1);
            RequestManager.setWorkflowid(workflowid);
            RequestManager.setIsremark(-1);
            RequestManager.setFormid(formid);
            RequestManager.setIsbill(1);
            RequestManager.setBillid(-1);
            RequestManager.setNodeid(nodeid);
            RequestManager.setNodetype(nodetype);
            RequestManager.setRequestname(requestname);
            RequestManager.setRemark(remark);
            RequestManager.setUser(user);
            RequestManager.setIsagentCreater(-1);
            RequestManager.setBeAgenter(0);
            RequestManager.setRequest(new FileUpload(request));
            //系统表单判断会议名称是否和流程名称对应关系
            MeetingWFComInfo meetingWFComInfo = new MeetingWFComInfo();
            String billKey = meetingWFComInfo.getFieldnames("85", "name");
            if (billKey.equals("requestName")) {
                RequestManager.setRequestname(meetingname);
            }
            WFManager wfManager = new WFManager();
            wfManager.setWfid(workflowid);
            wfManager.getWfInfo();
            String messageType = wfManager.getMessageType();
            if (messageType.equals("1")) {
                messageType = wfManager.getSmsAlertsType();
            }
            RequestManager.setMessageType(messageType);
            boolean savestatus = RequestManager.saveRequestInfo();
            requestid = RequestManager.getRequestid();
            if (!savestatus) {//创建流程失败
                if (requestid != 0) {
                    String message = RequestManager.getMessage();
                    ret.put("status", false);
                    ret.put("error", message);
                    ret.put("showMsg", message);
                    return ret;
                }
            }
            MeetingWFUtil.updateMeeting2WF(meetingId, "" + formid, "" + requestid, user.getUID());
            recordSet.execute("delete meeting_sharedetail where meetingid=" + meetingId + " and sharelevel=4");
            boolean flowstatus = RequestManager.flowNextNode();
            if (!flowstatus) {
                ret.put("status", false);
                return ret;
            }

            recordSet.executeQuery("select a.nodeid from workflow_flownode a,workflow_nodebase b where a.nodeid=b.id and a.workflowid=" + workflowid + " and a.nodetype = 0");
            if (recordSet.next()) {
                int node_id = Util.getIntValue(recordSet.getString("nodeid"), 0);
                if (nodeid == node_id) {
                    recordSet.execute("update workflow_currentoperator set isremark='2',operatedate='" + currentdate + "',operatetime='" + currenttime + "' where (isremark = '5' or isremark='0') and requestid=" + requestid + " and nodeid=" + node_id + "");
                }
            }

            //归档
            String currentnodetypetmp = RequestManager.getNextNodetype();
            recordSet.executeQuery("select currentnodetype from workflow_requestbase where requestid=" + requestid);
            if (recordSet.next()) currentnodetypetmp = recordSet.getString("currentnodetype");
            if ("3".equals(currentnodetypetmp)) {
                recordSet.execute("update workflow_currentoperator set isremark = '2',iscomplete=1 where  requestid = " + requestid + " and nodeid = " + nodeid + " and (isremark = '9' or preisremark = '9') and userid = " + user.getUID());
            } else {
                recordSet.execute("update workflow_currentoperator set isremark = '2' where  requestid = " + requestid + " and nodeid = " + nodeid + " and isremark = '9' and userid = " + user.getUID() + " and preisremark = '9'");
            }
            /*下一个节点未审批节点时才给节点操作者赋予会议权限*/
            if (RequestManager.getNextNodetype().equals("1")) {
                String sqlstr = "select distinct userid,usertype from workflow_currentoperator where isremark = '0' and requestid =" + requestid;
                int theusertype = 0;
                recordSet.executeQuery(sqlstr);
                RecordSet recordSet2 = new RecordSet();
                while (recordSet.next()) {
                    int userid1 = recordSet.getInt("userid");
                    int usertype1 = recordSet.getInt("usertype");
                    if (usertype1 == 0) theusertype = 1;
                    else theusertype = 2;

                    int sharelevel = 4;
                    int shareRight = 1;
                    if (!meetingId.equals("")) {
                        recordSet2.executeQuery(" select sharelevel from meeting_sharedetail where meetingid = " + meetingId + " and userid = " + userid1 + " and usertype = " + theusertype + " AND shareLevel <> 2");
                        if (recordSet2.getCounts() == 0) {
                            recordSet2.execute("INSERT INTO Meeting_ShareDetail(meetingid, userid, usertype, sharelevel,shareRight) VALUES (" + meetingId + "," + userid1 + "," + theusertype + "," + sharelevel + "," + shareRight + ")");  //插入审批人权限
                        }
                    }
                }
            }

            recordSet.execute("Update Meeting Set requestid =" + requestid + ",meetingstatus=1 WHERE ID=" + meetingId);//更新当前会议的requestid
            //如果仅仅两个节点直接触发流程
            if (RequestManager.getNextNodetype().equals("3")) {//下一节点直接是归档节点,会直接变成正常状态
                //设置会议正常,创建日程或者形成周期会议
                MeetingWFUtil.setMeetingNormal(meetingId);

            }
            ret.put("status", true);
            return ret;
        }
    }

    public String getMinId(List list){
        int temp = 0;
        for(int i = 0;i < list.size();i++){
            int id = Util.getIntValue(Util.null2String(list.get(i)));
            if(temp >= id){
                temp = id;
            }
        }
        return temp+"";
    }

}
