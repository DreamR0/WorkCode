package com.engine.kq.cmd.remindOnPC;

import com.alibaba.fastjson.JSON;
import com.api.cusomization.qc2721227.util.KqCustomUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.bean.KQHrmScheduleSign;
import com.engine.kq.biz.*;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.*;

public class GetRemindInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

    private static KQLog logger = new KQLog();//用于记录日志信息

    public GetRemindInfoCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        KqCustomUtil kqCustomUtil = new KqCustomUtil();

        //获取当前日期
        String currentDate = DateUtil.getCurrentDate();
        //获取当前时间
        String currentTime = DateUtil.getCurrentHourMin();
        //获取当前的日期时间
        String currentDateTime = DateUtil.getFullDate();
        //获取当前登录人员ID
        String resourceId = "" + user.getUID();
        //获取当前登录人员的考勤组
        KQGroupEntity kqGroupEntity = null;
        KQGroupMemberComInfo groupMemberComInfo = new KQGroupMemberComInfo();
        kqGroupEntity = groupMemberComInfo.getUserKQGroupInfo(resourceId, currentDate);
        if (kqGroupEntity == null) {
            //logger.info("获取不到当前人员的考勤组，登录PC端不提醒。resourceId=" + resourceId);
            resultMap.put("isOpen", false);
            return resultMap;
        }
        //判断是否是无需考勤人员
        String excludeId = kqGroupEntity.getExcludeid();
        List<String> excludeIdList = Arrays.asList(excludeId.split(","));
        if (excludeIdList != null && excludeIdList.contains(resourceId)) {
            //logger.info("当前人员无需考勤，登录PC端不提醒。resourceId=" + resourceId);
            resultMap.put("isOpen", false);
            return resultMap;
        }
        //==z 判断当天考勤人员是否请假
        new BaseBean().writeLog("==zj==(打卡提醒)" + kqCustomUtil.isLeave(resourceId,currentDate));
        if (kqCustomUtil.isLeave(resourceId,currentDate)){
            resultMap.put("isOpen", false);
            return resultMap;
        }

        //获取考勤组的打卡方式
        String signType = kqGroupEntity.getSignintype();
        if ("4".equals(signType)) {
            //logger.info("当前考勤组无需打卡，登录PC端不提醒。resourceId=" + resourceId);
            resultMap.put("isOpen", false);
            return resultMap;
        }
        /**
         * 假如班次时间如下：
         * 2019-10-15 22:00:00~~2019-10-16 02:00:00
         * 2019-10-16 12:00:00~~2019-10-16 20:00:00
         * 那么人员A于2019-10-16 01:00:00登录系统时要弹窗提示其打卡(如果他没有打卡的话)
         * 所以这里需要判断前一天的班次情况
         */
        for (int i = -1; i <= 0; i++) {
            KQWorkTime kqWorkTime = new KQWorkTime();
            String kqDate = DateUtil.addDate(currentDate, i);
            //获取当前对应人员的班次
            String serialId = kqWorkTime.getSerialIds(resourceId, kqDate);
            if (serialId == null || "".equals(serialId) || serialId.contains("{")) {
                if (i == 0) {
                    //logger.info("获取不到当前人员的班次，登录PC端不提醒。resourceId=" + resourceId);
                    resultMap.put("isOpen", false);
                    return resultMap;
                } else {
                    continue;
                }
            }
            //判断班次是否开启了打卡提醒
            KQShiftManagementComInfo shiftManagementComInfo = new KQShiftManagementComInfo();
            String cardRemind = shiftManagementComInfo.getCardRemind(serialId);
            if (!"1".equals(cardRemind)) {
                if (i == 0) {
                    //logger.info("未开启打卡提醒，登录PC端不提醒。resourceId=" + resourceId);
                    resultMap.put("isOpen", false);
                    return resultMap;
                } else {
                    continue;
                }
            }
            //判断班次是否开启了登录PC端打卡提醒
            String remindOnPC = shiftManagementComInfo.getRemindOnPC(serialId);
            if (!"1".equals(remindOnPC)) {
                if (i == 0) {
                    //logger.info("未开启打卡提醒，登录PC端不提醒。resourceId=" + resourceId);
                    resultMap.put("isOpen", false);
                    return resultMap;
                } else {
                    continue;
                }
            }
            //判断是否能获取到工作时间
            WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(resourceId, kqDate);
            if (workTimeEntity == null) {
                if (i == 0) {
                    //logger.info("获取不到工作时间，登录PC端不提醒。resourceId=" + resourceId);
                    resultMap.put("isOpen", false);
                    return resultMap;
                } else {
                    continue;
                }
            }
            //判断是否当前时间是否在考勤时间范围内
            boolean isInScope = false;
            String preDate = DateUtil.addDate(kqDate, -1);
            String nextDate = DateUtil.addDate(kqDate, 1);
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
            List<TimeScopeEntity> signTime = workTimeEntity.getSignTime();
            for (int j = 0; j < signTime.size(); j++) {
                TimeScopeEntity signTimeScope = signTime.get(j);

                String signBeginDateTime = "";//上班打卡开始时间
//                signBeginDateTime = signTimeScope.getBeginTimeAcross() ? preDate : kqDate;
                signBeginDateTime = signTimeScope.isBeginTimePreAcross() ? preDate : kqDate;
                signBeginDateTime += " " + kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime()) + ":00";

                String signEndDateTime = "";//下班打卡结束时间
                signEndDateTime = signTimeScope.getEndTimeAcross() ? nextDate : kqDate;
                signEndDateTime += " " + kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime()) + ":59";

                String signBeginDateTimeStart = signBeginDateTime;//上班打卡开始时间
                String signBeginDateTimeEnd = signEndDateTime;//上班打卡结束时间

                String signEndDateTimeStart = signBeginDateTime;//下班打卡开始时间
                String signEndDateTimeEnd = signEndDateTime;//下班打卡结束时间
                TimeSignScopeEntity timeSignScopeEntity = signTimeScope.getTimeSignScopeEntity();
                if (timeSignScopeEntity == null) {
                    //没有设置 上班打卡结束时间和下班打卡开始时间
                } else {
                    String beginTimeEnd = timeSignScopeEntity.getBeginTimeEnd();//上班打卡结束时间
                    boolean beginTimeEndAcross = timeSignScopeEntity.isBeginTimeEndAcross();

                    String endTimeStart = timeSignScopeEntity.getEndTimeStart();//下班打卡开始时间
                    boolean endTimeStartAcross = timeSignScopeEntity.isEndTimeStartAcross();

                    if (beginTimeEnd.length() > 0) {
                        //如果设置了 上班结束时间
                        if (endTimeStart.length() > 0) {
                            signBeginDateTimeStart = signBeginDateTime;
                            signBeginDateTimeEnd = beginTimeEndAcross ? nextDate : kqDate;
                            signBeginDateTimeEnd += " " + kqTimesArrayComInfo.turn48to24Time(beginTimeEnd) + ":59";

                            signEndDateTimeStart = endTimeStartAcross ? preDate : kqDate;
                            signEndDateTimeStart += " " + kqTimesArrayComInfo.turn48to24Time(endTimeStart) + ":00";
                            signEndDateTimeEnd = signEndDateTime;
                        } else {
                            //没有设置下班开始时间
                            signBeginDateTimeStart = signBeginDateTime;
                            signBeginDateTimeEnd = beginTimeEndAcross ? nextDate : kqDate;
                            signBeginDateTimeEnd += " " + kqTimesArrayComInfo.turn48to24Time(beginTimeEnd) + ":59";

                            //如果设置了上班结束时间，相当于下班开始时间也被限定了
                            String kqTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(beginTimeEnd) + 1);
                            signEndDateTimeStart = beginTimeEndAcross ? nextDate : kqDate;
                            signEndDateTimeStart += " " + kqTimesArrayComInfo.turn48to24Time(kqTime) + ":00";
                            signEndDateTimeEnd = signEndDateTime;
                        }
                    } else {
                        //如果没有设置上班结束时间，设置了下班开始时间
                        //如果设置了下班开始时间，相当于上班结束时间也被限定了
                        if (endTimeStart.length() > 0) {
                            signBeginDateTimeStart = signBeginDateTime;
                            String kqTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(endTimeStart) - 1);
                            signBeginDateTimeEnd = endTimeStartAcross ? preDate : kqDate;
                            signBeginDateTimeEnd += " " + kqTimesArrayComInfo.turn48to24Time(kqTime) + ":59";

                            signEndDateTimeStart = endTimeStartAcross ? preDate : kqDate;
                            signEndDateTimeStart += " " + kqTimesArrayComInfo.turn48to24Time(endTimeStart) + ":00";
                            signEndDateTimeEnd = signEndDateTime;
                        } else {
                            //没有设置 上班打卡结束时间和下班打卡开始时间
                        }
                    }
                }
                if (signBeginDateTimeStart.compareTo(currentDateTime) <= 0 && signBeginDateTimeEnd.compareTo(currentDateTime) >= 0) {
                    //判断是否已经打过卡
                    boolean hasSign = hasSign(resourceId, signBeginDateTimeStart, signBeginDateTimeEnd);
                    if (hasSign) {
                        //logger.info("已经打过卡了，登录PC端不提醒。resourceId=" + resourceId);
                        resultMap.put("isOpen", false);
                        return resultMap;
                    } else {
                        break;
                    }
                } else {
                    if (i == 0 && j == signTime.size() - 1) {
                        //logger.info("不在打卡时间范围内，登录PC端不提醒。resourceId=" + resourceId);
                        resultMap.put("isOpen", false);
                        return resultMap;
                    } else {
                        continue;
                    }
                }
            }
        }

        if ("1".equals(signType) || "2".equals(signType)) {
            resultMap.put("msg", SystemEnv.getHtmlLabelName(21415, user.getLanguage()));
            resultMap.put("canSignOnPC", true);
            resultMap.put("isOpen", true);
        } else {
            resultMap.put("msg", SystemEnv.getHtmlLabelName(510586, user.getLanguage()));
            resultMap.put("canSignOnPC", false);
            resultMap.put("isOpen", true);
        }
        return resultMap;
    }

    /**
     * 拼接打卡日期和时间的SQL
     *
     * @param beginDateTime
     * @param endDateTime
     * @return
     */
    private boolean hasSign(String resourceId, String beginDateTime, String endDateTime) {
        RecordSet rs = new RecordSet();
        StringBuffer sql = new StringBuffer();
        sql.append("select * from hrmschedulesign where isincom=1 and userid = ").append(resourceId);
        if (rs.getDBType().equals("oracle")||rs.getDBType().equals("postgresql")) {
            sql.append(" AND signDate||' '||signTime>='").append(beginDateTime).append("' ");
            sql.append(" AND signDate||' '||signTime<='").append(endDateTime).append("' ");
        } else if (rs.getDBType().equals("mysql")) {
            sql.append(" AND concat(signDate,' ',signTime)>='").append(beginDateTime).append("' ");
            sql.append(" AND concat(signDate,' ',signTime)<='").append(endDateTime).append("' ");
        } else {
            sql.append(" AND signDate+' '+signTime>='").append(beginDateTime).append("' ");
            sql.append(" AND signDate+' '+signTime<='").append(endDateTime).append("' ");
        }
        rs.executeQuery(sql.toString());
        boolean hasSign = rs.getCounts() > 0;
        return hasSign;
    }
}
