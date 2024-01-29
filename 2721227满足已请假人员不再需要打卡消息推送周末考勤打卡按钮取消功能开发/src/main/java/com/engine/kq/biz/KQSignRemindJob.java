package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.cusomization.qc2721227.util.KqCustomUtil;
import com.cloudstore.dev.api.bean.MessageBean;
import com.cloudstore.dev.api.bean.MessageType;
import com.cloudstore.dev.api.util.Util_Message;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.msgcenter.biz.ConfigManager;
import com.engine.msgcenter.biz.WeaMessageTypeConfig;
import com.google.common.collect.Sets;
import java.io.PrintWriter;
import java.io.StringWriter;
import weaver.common.DateUtil;
import weaver.common.MessageUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.email.EmailWorkRunnable;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.common.Tools;
import weaver.hrm.resource.ResourceComInfo;
import weaver.interfaces.schedule.BaseCronJob;

import java.util.*;

public class KQSignRemindJob extends BaseCronJob {

    private BaseBean log = new BaseBean();

    private KQLog kqLog = new KQLog();

    private static List myTimerTaskList = new ArrayList();
    private static Timer timer = new Timer();

    public void execute() {
        String currentDate = Tools.getCurrentDate();
        kqLog.info(">>>>>>>>>>>>>>>>KQSignRemind>>>>>>>>>>>>>>begin>>>>>>>>>>>>>>>>");
        handleKqRemind(currentDate);
        kqLog.info(">>>>>>>>>>>>>>>>KQSignRemind>>>>>>>>>>>>>>end>>>>>>>>>>>>>>>>");
    }

    private void handleKqRemind(String date) {
        ResourceComInfo rci = null;
        try {
            rci = new ResourceComInfo();
        } catch (Exception e) {
            log.writeLog(e);
        }
        Map<Long, List<String>> timeMap_begin = new HashMap<Long, List<String>>();
        Map<Long, List<String>> timeMap_end = new HashMap<Long, List<String>>();
        List<String> beginList = new ArrayList<String>();
        List<String> endList = new ArrayList<String>();
        KQGroupEntity kqGroupEntity = null;
        KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
        KQShiftManagementComInfo shiftComInfo = new KQShiftManagementComInfo();
        KqCustomUtil kqCustomUtil = new KqCustomUtil();

        //add
        Map<String,String> shiftmap = new HashMap<String,String>();
        String shiftsql = "select id,cardRemind from kq_shiftmanagement";
        RecordSet rs1 = new RecordSet();
        rs1.executeQuery(shiftsql);
        while (rs1.next()){
            String id = Util.null2String(rs1.getString("id"));
            String cardRemind = Util.null2String(rs1.getString("cardRemind"));
            shiftmap.put(id,cardRemind);
        }
//        kqLog.info("KQSignRemind>>>>>shiftmap:" + shiftmap.toString());
        //end


        String sql = "SELECT DISTINCT resourceId FROM (" + new KQGroupBiz().getGroupMemberSql() + ") t ";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        while (recordSet.next()) {
            String resourceId = recordSet.getString("resourceId");
            String status = StringUtil.vString(rci.getStatus(resourceId));
            String accountType = StringUtil.vString(rci.getAccountType(resourceId), "0");
            if (!("0".equals(status) || "1".equals(status) || "2".equals(status) || "3".equals(status))) {
                continue;
            }
            if (!"0".equals(accountType)) {
                continue;
            }
            kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(resourceId, date);
            if (kqGroupEntity == null) {
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>kqGroupEntity:null");
                continue;
            }
            //判断是否是无需考勤人员
            String excludeId = kqGroupEntity.getExcludeid();
            List<String> excludeIdList = Arrays.asList(excludeId.split(","));
            if (excludeIdList != null && excludeIdList.contains(resourceId)) {
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>无需考勤人员");
                continue;
            }
            new BaseBean().writeLog("==zj==(打卡提醒计划任务)" + kqCustomUtil.isLeave(resourceId,date));
            if (kqCustomUtil.isLeave(resourceId,date)){
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>当天有请假流程");
                continue;
            }

            String signtype = kqGroupEntity.getSignintype();
            //打卡方式是 无需打卡
            if("4".equalsIgnoreCase(signtype)){
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>打卡方式是无需打卡");
                continue;
            }
            ShiftInfoBean shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceId, date, false);
            if (shiftInfoBean == null) {
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>shiftInfoBean:null");
                continue;
            }
            String serialId = shiftInfoBean.getSerialid();
            if (serialId == null || "".equals(serialId)) {
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>serialId:");
                continue;
            }
            String cardRemind = shiftComInfo.getCardRemind(serialId);
            if (!"1".equals(cardRemind) || !"1".equals(shiftmap.get(serialId))) {
                continue;
            }
            String cardRemOfSignIn = shiftComInfo.getCardRemOfSignIn(serialId);
            String minsBeforeSignIn = shiftComInfo.getMinsBeforeSignIn(serialId);
            String cardRemOfSignOut = shiftComInfo.getCardRemOfSignOut(serialId);
            String minsAfterSignOut = shiftComInfo.getMinsAfterSignOut(serialId);
            String remindMode = shiftComInfo.getRemindMode(serialId);
            if (!"1".equals(remindMode) && !"2".equals(remindMode) && !"3".equals(remindMode)) {
                kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>remindMode:");
                continue;
            }
            String remindTimeStr = getAllRemindTimeStr(minsBeforeSignIn, minsAfterSignOut, shiftInfoBean, shiftComInfo);
            kqLog.info("KQSignRemind>>>>>resourceId:" + resourceId + ">>>>remindTimeStr:"+remindTimeStr);
            String[] remindTimeArr = remindTimeStr.split(";");
            for (String remindTime : remindTimeArr) {
                if (remindTime == null || remindTime.equals("")) {
                    continue;
                }
                long reminTime_start = Long.parseLong(remindTime.split("-")[0]);
                long reminTime_end = Long.parseLong(remindTime.split("-")[1]);
                String serialNumber = remindTime.split("-")[2];
                if (reminTime_start > 0 && "1".equals(cardRemOfSignIn)) {
                    beginList = timeMap_begin.get(reminTime_start);
                    if (beginList == null) {
                        beginList = new ArrayList<String>();
                    }
                    beginList.add(resourceId + "-" + serialId + "-" + remindMode + "-" + serialNumber);
                    timeMap_begin.put(reminTime_start, beginList);
                }
                if (reminTime_end > 0 && "1".equals(cardRemOfSignOut)) {
                    endList = timeMap_end.get(reminTime_end);
                    if (endList == null) {
                        endList = new ArrayList<String>();
                    }
                    endList.add(resourceId + "-" + serialId + "-" + remindMode + "-" + serialNumber);
                    timeMap_end.put(reminTime_end, endList);
                }
            }
        }
        kqLog.info("timeMap_begin:"+ JSONObject.toJSONString(timeMap_begin));
        kqLog.info("timeMap_end:"+ JSONObject.toJSONString(timeMap_end));
        doTimer(timeMap_begin, 1);
        doTimer(timeMap_end, 2);
    }

    private String getAllRemindTimeStr(String minsBeforeSignIn, String minsAfterSignOut, ShiftInfoBean shiftInfoBean, KQShiftManagementComInfo kqComInfo) {
        StringBuilder builder = new StringBuilder();
        String currentDate = DateUtil.getCurrentDate();
        String nextDate = currentDate;
        String nextDate2 = currentDate;
        String nextDate3 = currentDate;
        List<String> allWorkTime = shiftInfoBean.getAllWorkTime();
        // 1天1次或1天2次或1天3次上下班
        int shiftonoffworkcounts = Util.getIntValue(kqComInfo.getShiftonoffworkcounts(shiftInfoBean.getSerialid()), 0);
        String startWorkTime = "";
        String endWorkTime = "";
        String startWorkTime2 = "";
        String endWorkTime2 = "";
        String startWorkTime3 = "";
        String endWorkTime3 = "";
        switch (shiftonoffworkcounts) {
            case 1:
                //1天1次上下班
                startWorkTime = allWorkTime.get(0);
                endWorkTime = allWorkTime.get(1);
                if (startWorkTime.compareTo(endWorkTime) > 0) {
                    nextDate = DateUtil.addDate(currentDate, 1);
                }
                break;
            case 2:
                //1天2次上下班
                startWorkTime = allWorkTime.get(0);
                endWorkTime = allWorkTime.get(1);
                startWorkTime2 = allWorkTime.get(2);
                endWorkTime2 = allWorkTime.get(3);
                if (startWorkTime.compareTo(endWorkTime) > 0) {
                    nextDate = DateUtil.addDate(currentDate, 1);
                }
                if (startWorkTime2.compareTo(endWorkTime2) > 0) {
                    nextDate2 = DateUtil.addDate(currentDate, 1);
                }
                break;
            case 3:
                //1天3次上下班
                startWorkTime = allWorkTime.get(0);
                endWorkTime = allWorkTime.get(1);
                startWorkTime2 = allWorkTime.get(2);
                endWorkTime2 = allWorkTime.get(3);
                startWorkTime3 = allWorkTime.get(4);
                endWorkTime3 = allWorkTime.get(5);
                if (startWorkTime.compareTo(endWorkTime) > 0) {
                    nextDate = DateUtil.addDate(currentDate, 1);
                }
                if (startWorkTime2.compareTo(endWorkTime2) > 0) {
                    nextDate2 = DateUtil.addDate(currentDate, 1);
                }
                if (startWorkTime3.compareTo(endWorkTime3) > 0) {
                    nextDate3 = DateUtil.addDate(currentDate, 1);
                }
                break;
        }

        long afterBeforeTime_begin1 = 0;
        long afterBeforeTime_end1 = 0;
        long afterBeforeTime_begin2 = 0;
        long afterBeforeTime_end2 = 0;
        long afterBeforeTime_begin3 = 0;
        long afterBeforeTime_end3 = 0;
        if (!"".equals(startWorkTime)) {
            long time_begin = Tools.parseToDate(currentDate + " " + startWorkTime + ":00", "yyyy-MM-dd HH:mm:ss").getTime();
            afterBeforeTime_begin1 = time_begin - 60 * 1000 * Util.getIntValue(minsBeforeSignIn);
        }
        if (!"".equals(endWorkTime)) {
            long time_end = Tools.parseToDate(nextDate + " " + endWorkTime + ":00", "yyyy-MM-dd HH:mm:ss").getTime();
            afterBeforeTime_end1 = time_end + 60 * 1000 * Util.getIntValue(minsAfterSignOut);
        }

        if (!"".equals(startWorkTime2)) {
            long time_begin = Tools.parseToDate(currentDate + " " + startWorkTime2 + ":00", "yyyy-MM-dd HH:mm:ss").getTime();
            afterBeforeTime_begin2 = time_begin - 60 * 1000 * Util.getIntValue(minsBeforeSignIn);
        }
        if (!"".equals(endWorkTime2)) {
            long time_end = Tools.parseToDate(nextDate2 + " " + endWorkTime2 + ":00", "yyyy-MM-dd HH:mm:ss").getTime();
            afterBeforeTime_end2 = time_end + 60 * 1000 * Util.getIntValue(minsAfterSignOut);
        }

        if (!"".equals(startWorkTime3)) {
            long time_begin = Tools.parseToDate(currentDate + " " + startWorkTime3 + ":00", "yyyy-MM-dd HH:mm:ss").getTime();
            afterBeforeTime_begin3 = time_begin - 60 * 1000 * Util.getIntValue(minsBeforeSignIn);
        }
        if (!"".equals(endWorkTime3)) {
            long time_end = Tools.parseToDate(nextDate3 + " " + endWorkTime3 + ":00", "yyyy-MM-dd HH:mm:ss").getTime();
            afterBeforeTime_end3 = time_end + 60 * 1000 * Util.getIntValue(minsAfterSignOut);
        }
        builder.append(afterBeforeTime_begin1).append("-").append(afterBeforeTime_end1).append("-").append("1").append(";")
                .append(afterBeforeTime_begin2).append("-").append(afterBeforeTime_end2).append("-").append("2").append(";")
                .append(afterBeforeTime_begin3).append("-").append(afterBeforeTime_end3).append("-").append("3");
        return builder.toString();
    }

    private void doTimer(Map<Long, List<String>> timerMap, int type) {
        kqLog.info(">>>>SignRemind>>>>type="+type+">>>>timerMap=" + JSONObject.toJSONString(timerMap));
        for (Map.Entry<Long, List<String>> entry : timerMap.entrySet()) {
            Date d = new Date();
            d.setTime(entry.getKey());
            Date currentDate = new Date();
            if (currentDate.getTime() > d.getTime()) {
                continue;
            }
            final List<String> list = entry.getValue();
            kqLog.info(">>>>SignRemind>>>>time=" + entry.getKey() + ">>>>list.size=" + list.size()+ ">>>>list=" + list.toString());
            if (list.size() == 0) continue;
            MyTimerTask myTimerTask = new MyTimerTask("" + entry.getKey(), list, type);
            if (!myTimerTaskList.contains(myTimerTask.getKey())) {
                kqLog.info(">>>>SignRemind>>>>Add TimerTask>>>>time=" + entry.getKey()+":d:"+d);
                myTimerTaskList.add(myTimerTask.getKey());
                try{
                    timer.schedule(myTimerTask, d);
                }catch (Exception e){
                    e.printStackTrace();
                    kqLog.info("MyTimerTask:Exception");
                    StringWriter errorsWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(errorsWriter));
                    kqLog.info(errorsWriter.toString());
                }
            }
        }
    }

    public void appNotification(Set resourceIDSet,int type) {
        try {

            String detailTitle = "510146";
            String title = "";
            if (1 == type) {
                title = "" + weaver.systeminfo.SystemEnv.getHtmlLabelName(10005295, weaver.general.ThreadVarLanguage.getLang()) + "";
            } else {
                title = "" + weaver.systeminfo.SystemEnv.getHtmlLabelName(10005309, weaver.general.ThreadVarLanguage.getLang()) + "";
            }
            String content = "";
            int createrId = 1;
            String pcUrl = "";
            String mobileUrl = "/spa/hrm/static4mobile/index.html#/sign";
            kqLog.info(">>>>>>>>appNotification>>>>>>>>>>>>>>time>>>>>>>"+DateUtil.getDateTime()+">>>>>>resourceIDSet>>>>>>"+ JSON.toJSON(resourceIDSet));
            ConfigManager configManager = new ConfigManager();
            // 2、默认规则检查用户配置,返回后台自定义消息类型和对应通过配置的用户
            //    @param singPath 与需求变更之前的path参数含义一致,代表该消息类型详细配置中唯一标志值,各模块参考自己的值(有疑问请联系云商店-田泽法)
            //    @param userid 按默认规则检查的用户配置信息(检查多人参考重载方法)
            Map<WeaMessageTypeConfig, List<String>> accessConfig = configManager.defaultRuleCheckConfig(MessageType.HRM_KQ_REMIND, resourceIDSet, null);

            // 3、遍历自定义消息类型集合
            for (Map.Entry<WeaMessageTypeConfig, List<String>> entry : accessConfig.entrySet()) {
                // 4、构造消息实体
                MessageBean bean = Util_Message.createMessage(MessageType.HRM_KQ_REMIND, 0, title, detailTitle, content, pcUrl, mobileUrl, createrId);

                // 5、获取新的自定义消息类型
                WeaMessageTypeConfig config = entry.getKey();

                // 6、新的自定义消息类型相关信息通知到消息实体bean上
                bean.setMessageConfig(config);

                // 7、设置检查配置通过需要发送消息的用户
                bean.setUserList(Sets.newHashSet(entry.getValue()));

                // 8、发送消息
                kqLog.info("in>>>>SignRemind>>>>MyTimerTask>>>appNotification=bean::"+bean);
                Util_Message.sendAndpublishMessage(bean);
            }
        } catch (Exception e) {
            log.writeLog(e);

        }
    }

    private void emailNotification(String resourceId, String serialId, String emailAddress, String emailTitle, String emailContent, String serialNumber, int type) {
        String sendTo = Util.null2String(emailAddress);
        if ("".equals(sendTo)) {
            kqLog.info("邮箱地址为空，resourceId:" + resourceId + ">>>>>>emailAddress>>>>>>" + emailAddress);
            return;
        }
        String currentDate = DateUtil.getCurrentDate();
        //是否已经签到过了，如果已经签到过了，就不提醒了
        boolean hasSign = hasSign(resourceId, serialNumber, type);
        if (hasSign) {
            kqLog.info("已经签到或签退过了，resourceId:" + resourceId + ">>>>>>emailAddress>>>>>>" + emailAddress);
            return;
        }
        EmailWorkRunnable.threadModeReminder(sendTo, emailTitle, emailContent);
    }

    private void mobileNotification(String resourceId, String serialId, String mobile, String content, String serialNumber, int type) {
        if (mobile == null || "".equals(mobile)) {
            kqLog.info("手机号码为空，resourceId:" + resourceId + ">>>>>>mobile>>>>>>" + mobile);
            return;
        }
        //是否已经签到过了，如果已经签到过了，就不提醒了
        boolean hasSign = hasSign(resourceId, serialNumber, type);
        if (hasSign) {
            kqLog.info("已经签到或签退过了，resourceId:" + resourceId + ">>>>>>mobile>>>>>>" + mobile);
            return;
        }
        try {
            MessageUtil.sendSMS(mobile, content);
        } catch (Exception e) {
            kqLog.info("SendSMS error.resourceId:" + resourceId + ">>>>>>mobile>>>>>>" + mobile);
            log.writeLog(e);
        }
    }

    public boolean hasSign(String resourceId, String serialNumber, int type) {
        boolean hasSign = false;
        try {
            boolean hasSignIn = false;
            boolean hasSignOut = false;
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
            String currentDate = DateUtil.getCurrentDate();
            String preDate = DateUtil.addDate(currentDate, -1);//上一天日期
            String nextDate = DateUtil.addDate(currentDate, 1);//下一天日期

            KQWorkTime kqWorkTime = new KQWorkTime();
            WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(resourceId, currentDate);
            List<TimeScopeEntity> lsSignTime = new ArrayList<>();
            List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
            List<TimeScopeEntity> lsRestTime = new ArrayList<>();
            List<Object> workFlow = null;

            if (workTimeEntity != null) {
                lsSignTime = workTimeEntity.getSignTime();//允许打卡时间
                lsWorkTime = workTimeEntity.getWorkTime();//工作时间
                lsRestTime = workTimeEntity.getRestTime();//休息时段时间
            }
            if (!lsWorkTime.isEmpty()) {
                //这个serialNumber传过来1表示是第一个打卡区间的，传过来2是第二个，以此类推
                int inx_serialNumber = Util.getIntValue(serialNumber) - 1;
                if (inx_serialNumber > -1) {
                    TimeScopeEntity signTimeScope = lsSignTime.get(inx_serialNumber);
                    TimeScopeEntity workTimeScope = lsWorkTime.get(inx_serialNumber);
                    TimeScopeEntity restTimeScope = lsRestTime.isEmpty() ? null : lsRestTime.get(inx_serialNumber);
                    List<Object> lsCheckInfo = new KQFormatSignData().getSignInfo(resourceId, signTimeScope, workTimeScope, currentDate, preDate, nextDate, kqTimesArrayComInfo);
                    if (!lsCheckInfo.isEmpty()) {
                        for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
                            Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(j);
                            String signType = Util.null2String(checkInfo.get("signType"));
                            //签到
                            if ("1".equalsIgnoreCase(signType)) {
                                hasSignIn = true;
                            }
                            //签退
                            if ("2".equalsIgnoreCase(signType)) {
                                hasSignOut = true;
                            }
                        }
                    }
                    kqLog.info("hasSign:resourceId:"+resourceId+":type:" + type+":lsCheckInfo:" + lsCheckInfo + ":hasSignIn:" + hasSignIn + ":hasSignOut:" + hasSignOut);
                }
            }
            if (1 == type) {
                if (hasSignIn) {
                    hasSign = true;
                }
            } else {
                if (hasSignOut) {
                    hasSign = true;
                }
            }
        } catch (Exception e) {
            log.writeLog(e);
        }
        return hasSign;
    }

    class MyTimerTask extends TimerTask {

        private String key;
        private List<String> list;
        private int type;

        public MyTimerTask(String key, List<String> list, int type) {
            this.key = key;
            this.list = list;
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        @Override
        public void run() {
            kqLog.info("in>>>>SignRemind>>>>MyTimerTask>>>time=");
            ResourceComInfo rci = null;
            try {
                rci = new ResourceComInfo();
                KQEmailRemindComInfo emailComInfo = new KQEmailRemindComInfo();
                KQMessageRemindComInfo messageComInfo = new KQMessageRemindComInfo();
                Set resourceIDSet = new HashSet();
                Set resourceIDlog = new HashSet();
                for (String info : list) {
                    try {
                        String resourceId = info.split("-")[0];
                        String serailId = info.split("-")[1];
                        String remindMode = info.split("-")[2];
                        String serialNumber = info.split("-")[3];
                        resourceIDlog.add(resourceId);
                        // 1. 消息中心
                        kqLog.info("in>>>>SignRemind>>>>MyTimerTask>>>remindMode="+remindMode);
                        if ("1".equals(remindMode) && checkAppNotification(resourceId, serailId, serialNumber, type)) {
                            resourceIDSet.add(resourceId);
                        }
                        //2. 邮件提醒
                        if ("2".equals(remindMode)) {
                            String emailAddress = rci.getEmail(resourceId);
                            String emailTitle = emailComInfo.getEmailTitle(serailId, "" + type);
                            String emailConTent = emailComInfo.getEmailContent(serailId, "" + type);
                            emailNotification(resourceId, serailId, emailAddress, emailTitle, emailConTent, serialNumber, type);
                        }
                        // 3. 短信提醒
                        if ("3".equals(remindMode)) {
                            String mobile = rci.getMobile(resourceId);
                            String content = messageComInfo.getMessageContent(serailId, "" + type);
                            mobileNotification(resourceId, serailId, mobile, content, serialNumber, type);
                        }
                    } catch (Exception e) {
                        log.writeLog(e);
                    }
                }
                kqLog.info("in>>>>SignRemind>>>>resourceIDlog="+resourceIDlog);
                kqLog.info("in>>>>SignRemind>>>>resourceIDSet="+resourceIDSet);
                appNotification(resourceIDSet,type);
                kqLog.info("out>>>>SignRemind>>>>MyTimerTask>>>time=");
            } catch (Exception e) {
                log.writeLog("MyTimerTask:"+e);
            }
        }
    }

    public boolean checkAppNotification(String resourceId, String serialId, String serialNumber, int type) {
        //是否已经签到过了，如果已经签到过了，就不提醒了
        boolean hasSign = hasSign(resourceId, serialNumber, type);
        kqLog.info("in>>>>SignRemind>>>>MyTimerTask>>>resourceId="+resourceId+"::hasSign::"+hasSign);
        if (hasSign) {
            kqLog.info("已经签到或签退过了，resourceId:" + resourceId);
            return false;
        }
        return true;
    }

    public static List getMyTimerTaskList() {
        return myTimerTaskList;
    }

}
