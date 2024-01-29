package com.engine.kq.cmd.myattendance;

import com.alibaba.fastjson.JSON;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQAttFlowSetBiz;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import com.engine.kq.biz.KQTimeSelectionComInfo;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.util.KQTransMethod;
import com.engine.kq.util.PageUidFactory;
import com.engine.kq.wfset.util.SplitSelectSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.util.*;

/**
 * 前台--人事--我的考勤--明细
 */
public class GetHrmKQReportDetialInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetHrmKQReportDetialInfoCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    private static boolean isIndirectSuperId(String id,String checkSuperId,ResourceComInfo info){

        int loopBreakTimes = 20 ;
        for(String loopId = id;
            isRightResourceId(loopId) && (loopBreakTimes--)>0 ;
            loopId = info.getManagerID(loopId)){
            if(isDirectSuperId(loopId,checkSuperId,info)) return true ;
        }
        return false ;
    }

    private static boolean isRightResourceId(String id){
        return StringUtils.isNotBlank(id) && !"0".equals(id) ;
    }

    private static boolean isDirectSuperId(String id,String checkSuperId,ResourceComInfo info){
        String superId = Util.null2String(info.getManagerID(id)) ;
        return isRightResourceId(superId) && superId.equals(checkSuperId) ;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        //==zj 如果来自移动端就显示为当前唯一生效考勤组的信息
        if ("1".equals(Util.null2String(params.get("ismobile")))){
            //==zj  判断主账号有没有考勤组
            KqUtil kqUtil = new KqUtil();
            String userId = Util.null2String(user.getUID());  //获取主账号userid
            String today = DateUtil.getCurrentDate();         //获取当前日期

            Boolean flag = kqUtil.mainAccountCheck(userId, today);
            new BaseBean().writeLog("==zj==(获取打卡界面)" + flag);
            if (!flag){
                //如果主账号没有考勤组，再看子账号是否有考勤组
                List<Map> list = kqUtil.childAccountCheck(userId, today);
                new BaseBean().writeLog("==zj==(获取打卡界面-maps)" + JSON.toJSONString(list));
                if (list.size()  == 1){
                    //如果只有一个生效考勤组
                    Map<String,String> map = list.get(0);
                    int userid = Integer.parseInt(map.get("userid"));
                    User user = new User(userid);
                    this.user = user;
                }
            }
        }
        try {
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String resourceId = Util.null2String(params.get("resourceId"));
            int departmentId = 0;
            if("".equals(resourceId)) {
                resourceId = ""+user.getUID();
                departmentId = user.getUserDepartment();
            } else {
                departmentId = Util.getIntValue(resourceComInfo.getDepartmentID(resourceId), 0);
            }



            //==zj 这里多个判断，如果查询账号为当前登录账号的子账号，解除权限。
            KqUtil kqUtil = new KqUtil();
            new BaseBean().writeLog("==zj==(权限判断)" + params.get("ismobile"));
            Boolean result = false;
            if ("1".equals(Util.null2String(params.get("ismobile")))){

            }else {
                result =   !kqUtil.isChildAccount(user.getUID(),resourceId);
            }

            if (!resourceId.equals("" + user.getUID()) && !isIndirectSuperId(resourceId, user.getUID() + "", resourceComInfo) && !HrmUserVarify.checkUserRight("HrmResource:Absense", user, departmentId) && result) {
                resultMap.put("status", "-1");
                resultMap.put("hasRight", false);
                return resultMap;
            }
            String kqtype = Util.null2String(params.get("kqtype"));
            if (kqtype.equalsIgnoreCase("realWorkdays")) {
                resultMap = getRealWorkdaysList();
            } else if (kqtype.equalsIgnoreCase("absent")) {
                resultMap = getAbsentList();
            } else if (kqtype.equalsIgnoreCase("beLate") || kqtype.equalsIgnoreCase("graveBeLate")) {
                if (kqtype.equalsIgnoreCase("graveBeLate")) {
                    params.put("tabKey", "2");
                }
                resultMap = getBeLateList();
            } else if (kqtype.equalsIgnoreCase("leaveEarly") || kqtype.equalsIgnoreCase("graveLeaveEarly")) {
                if (kqtype.equalsIgnoreCase("graveLeaveEarly")) {
                    params.put("tabKey", "2");
                }
                resultMap = getLeaveEearlyList();
            } else if (kqtype.equalsIgnoreCase("noSign")) {
                resultMap = getForgotCheckList();
            } else if (kqtype.startsWith("leaveType_")
                    || kqtype.equalsIgnoreCase("evection")
                    || kqtype.equalsIgnoreCase("outDays")
                    || kqtype.equalsIgnoreCase("overTime")) {
                resultMap = getLeaveList();
            } else if (kqtype.equals("leaveDays")) {
                resultMap = getLeaveDaysList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    return resultMap;
    }

    /**
     * 获取缺勤明细
     *
     * @return
     */
    private Map<String, Object> getLeaveDaysList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?

            /**若未指定查看人员，则默认为当前登录人员*/
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**获取指定日期的起点和终点*/
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**未出勤不需要查询今天之后的日期*/
            if (fromDate.compareTo(currentdate) > 0) {
                return resultMap;
            }
            if (fromDate.compareTo(currentdate) <= 0 && toDate.compareTo(currentdate) > 0) {
                toDate = currentdate;
            }

            /**获取请假类型对应的随机颜色值*/
            KQTransMethod kqTransMethod = new KQTransMethod();
            KQTimeSelectionComInfo kqTimeSelectionComInfo = new KQTimeSelectionComInfo();
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            Map<String, Object> leaveTypeColorMap = new HashMap<String, Object>();
            rulesComInfo.setTofirstRow();
            while (rulesComInfo.next()) {
                leaveTypeColorMap.put(rulesComInfo.getId(), KQLeaveRulesBiz.getColor());
            }

            KQGroupComInfo groupComInfo = new KQGroupComInfo();

            boolean hasLate = false;//是否存在迟到
            boolean hasEarly = false;//是否存在早退
            boolean hasAbsent = false;//是否存在旷工

            /*获取缺勤时长*/
            Map<String, Object> dayMap = new HashMap<String, Object>();//缺勤时长
            String str = "select * from kq_format_total where resourceId=? and kqDate>=? and kqDate<=? and workdays!=attendancedays order by kqDate desc";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(str, resourceId, fromDate, toDate);
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");//考勤日期
                String workDays = recordSet.getString("workdays");//应出勤天数；
                String attendanceDays = recordSet.getString("attendancedays");//实际出勤天数
                if (workDays.equals(attendanceDays)) {
                    continue;
                }

                String day = String.format("%.2f", Util.getDoubleValue(workDays, 0.00) - Util.getDoubleValue(attendanceDays, 0.00));
                dayMap.put(kqDate, day);
            }

            /*获取缺勤明细：迟到、严重迟到、早退、严重早退、旷工、请假*/
            Map<String, Object> sclTimeMap = new HashMap<String, Object>();//上班时段
            List<String> sclTimeDetailList = new ArrayList<String>();

            Map<String, Object> absTimeMap = new HashMap<String, Object>();//具体的缺勤时段
            List<Map<String, Object>> absTimeDetailList = new ArrayList<Map<String, Object>>();
            Map<String, Object> absTimeDetailMap = new HashMap<String, Object>();
            String sql = "select * from kq_format_detail where resourceId=? and kqDate>=? and kqDate<=? " +
                    " and (belatemins>0 or graveBeLateMins>0 or leaveearlymins>0 or graveLeaveEarlyMins>0 or absenteeismmins>0 or leaveMins>0) order by kqDate";
            recordSet.executeQuery(sql, resourceId, fromDate, toDate);
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");//考勤日期
                String workBeginDate = recordSet.getString("workbegindate");//工作开始日期
                String workBeginTime = recordSet.getString("workbegintime");//工作开始时间
                String workEndDate = recordSet.getString("workenddate");//工作结束日期
                String workEndTime = recordSet.getString("workendtime");//工作结束时间
                String belateMins = recordSet.getString("belatemins");//迟到时长
                String graveBeLateMins = recordSet.getString("graveBeLateMins");//严重迟到时长
                String leaveearlyMins = recordSet.getString("leaveearlymins");//早退时长
                String graveLeaveEarlyMins = recordSet.getString("graveLeaveEarlyMins");//严重早退时长
                String absenteeismmins = recordSet.getString("absenteeismmins");//旷工时长
                String signInDate = recordSet.getString("signInDate");//签到日期
                String signInTime = recordSet.getString("signintime");//签到时间
                String signOutDate = recordSet.getString("signOutDate");//签退日期
                String signOutTime = recordSet.getString("signouttime");//签退时间
                String groupId = recordSet.getString("groupId");//考勤组ID

                if (kqDate.compareTo(workBeginDate) == 0 && kqDate.compareTo(workEndDate) == 0) {
                    /*上班时段*/
                    sclTimeDetailList = (List<String>) sclTimeMap.get(kqDate);
                    if (sclTimeDetailList == null) {
                        sclTimeDetailList = new ArrayList<String>();
                    }
                    String temp = workBeginTime + "-" + workEndTime;
                    if (!sclTimeDetailList.contains(temp)) {
                        sclTimeDetailList.add(temp);
                    }
                    sclTimeMap.put(kqDate, sclTimeDetailList);

                    /*迟到和严重迟到算作缺勤*/
                    if (Util.getDoubleValue(belateMins, 0.00) > 0.00 || Util.getDoubleValue(graveBeLateMins, 0.00) > 0.00) {
                        hasLate = true;

                        absTimeDetailList = (List) absTimeMap.get(kqDate);
                        if (absTimeDetailList == null) {
                            absTimeDetailList = new ArrayList<Map<String, Object>>();
                        }

                        absTimeDetailMap = new HashMap<String, Object>();
                        absTimeDetailMap.put("bgColor", "#FFCCFF");
                        absTimeDetailMap.put("time", workBeginTime + "-" + signInTime);
                        absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(512504, user.getLanguage()) + "：" + signInDate + " " + signInTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20081, user.getLanguage())});
                        absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20081, user.getLanguage()));//迟到
                        absTimeDetailList.add(absTimeDetailMap);

                        absTimeMap.put(kqDate, absTimeDetailList);
                    }
                    /*早退和严重早退算作缺勤*/
                    if (Util.getDoubleValue(leaveearlyMins, 0.00) > 0.00 || Util.getDoubleValue(graveLeaveEarlyMins, 0.00) > 0.00) {
                        hasEarly = true;

                        absTimeDetailList = (List) absTimeMap.get(kqDate);
                        if (absTimeDetailList == null) {
                            absTimeDetailList = new ArrayList<Map<String, Object>>();
                        }

                        absTimeDetailMap = new HashMap<String, Object>();
                        absTimeDetailMap.put("bgColor", "#6EBF70");
                        absTimeDetailMap.put("time", signOutTime + "-" + workEndTime);
                        absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(512505, user.getLanguage()) + "：" + signOutDate + " " + signOutTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20082, user.getLanguage())});
                        absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20082, user.getLanguage()));//早退
                        absTimeDetailList.add(absTimeDetailMap);

                        absTimeMap.put(kqDate, absTimeDetailList);
                    }
                    /*旷工算作缺勤*/
                    if (Util.getDoubleValue(absenteeismmins, 0.00) > 0.00) {
                        hasAbsent = true;

                        absTimeDetailList = (List) absTimeMap.get(kqDate);
                        if (absTimeDetailList == null) {
                            absTimeDetailList = new ArrayList<Map<String, Object>>();
                        }

                        absTimeDetailMap = new HashMap<String, Object>();
                        absTimeDetailMap.put("bgColor", "#CCCCFF");
                        absTimeDetailMap.put("time", workBeginTime + "-" + workEndTime);
                        absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20085, user.getLanguage())});
                        absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20085, user.getLanguage()));//旷工
                        absTimeDetailList.add(absTimeDetailMap);

                        absTimeMap.put(kqDate, absTimeDetailList);
                    }
                } else if (kqDate.compareTo(workBeginDate) == 0 && kqDate.compareTo(workEndDate) < 0) {
                    /*上班时段*/
                    sclTimeDetailList = (List<String>) sclTimeMap.get(kqDate);
                    if (sclTimeDetailList == null) {
                        sclTimeDetailList = new ArrayList<String>();
                    }
                    String temp = workBeginTime + "-24:00";
                    if (!sclTimeDetailList.contains(temp)) {
                        sclTimeDetailList.add(temp);
                    }
                    sclTimeMap.put(kqDate, sclTimeDetailList);

                    sclTimeDetailList = (List<String>) sclTimeMap.get(workEndDate);
                    if (sclTimeDetailList == null) {
                        sclTimeDetailList = new ArrayList<String>();
                    }
                    temp = "00:00-" + workEndTime;
                    if (!sclTimeDetailList.contains(temp)) {
                        sclTimeDetailList.add(temp);
                    }
                    sclTimeMap.put(workEndDate, sclTimeDetailList);

                    /*迟到和严重迟到算作缺勤*/
                    if (Util.getDoubleValue(belateMins, 0.00) > 0.00 || Util.getDoubleValue(graveBeLateMins, 0.00) > 0.00) {
                        hasLate = true;

                        if (signInDate.compareTo(workBeginDate) == 0) {
                            absTimeDetailList = (List) absTimeMap.get(kqDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#FFCCFF");
                            absTimeDetailMap.put("time", workBeginTime + "-" + signInTime);
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(512504, user.getLanguage()) + "：" + signInDate + " " + signInTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20081, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20081, user.getLanguage()));//迟到
                            absTimeDetailList.add(absTimeDetailMap);

                            absTimeMap.put(kqDate, absTimeDetailList);
                        } else {
                            absTimeDetailList = (List) absTimeMap.get(kqDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#FFCCFF");
                            absTimeDetailMap.put("time", workBeginTime + "-24:00");
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(512504, user.getLanguage()) + "：" + signInDate + " " + signInTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20081, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20081, user.getLanguage()));//迟到
                            absTimeDetailList.add(absTimeDetailMap);
                            absTimeMap.put(kqDate, absTimeDetailList);

                            absTimeDetailList = (List) absTimeMap.get(workEndDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#FFCCFF");
                            absTimeDetailMap.put("time", "00:00-" + workEndTime);
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(512504, user.getLanguage()) + "：" + signInDate + " " + signInTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20081, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20081, user.getLanguage()));//迟到
                            absTimeDetailList.add(absTimeDetailMap);
                            absTimeMap.put(workEndDate, absTimeDetailList);
                        }
                    }
                    /*早退和严重早退算作缺勤*/
                    if (Util.getDoubleValue(leaveearlyMins, 0.00) > 0.00 || Util.getDoubleValue(graveLeaveEarlyMins, 0.00) > 0.00) {
                        hasEarly = true;

                        if (signOutDate.compareTo(workEndDate) == 0) {
                            absTimeDetailList = (List) absTimeMap.get(workEndDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#6EBF70");
                            absTimeDetailMap.put("time", signOutTime + "-" + workEndTime);
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(512505, user.getLanguage()) + "：" + signOutDate + " " + signOutTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20082, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20082, user.getLanguage()));//早退
                            absTimeDetailList.add(absTimeDetailMap);
                            absTimeMap.put(workEndDate, absTimeDetailList);
                        } else {
                            absTimeDetailList = (List) absTimeMap.get(kqDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#6EBF70");
                            absTimeDetailMap.put("time", signOutTime + "-24:00");
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(512505, user.getLanguage()) + "：" + signOutDate + " " + signOutTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20082, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20082, user.getLanguage()));//早退
                            absTimeDetailList.add(absTimeDetailMap);
                            absTimeMap.put(workEndDate, absTimeDetailList);

                            absTimeDetailList = (List) absTimeMap.get(workEndDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#6EBF70");
                            absTimeDetailMap.put("time", "24:00-" + workEndTime);
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(512505, user.getLanguage()) + "：" + signOutDate + " " + signOutTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20082, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20082, user.getLanguage()));//早退
                            absTimeDetailList.add(absTimeDetailMap);
                            absTimeMap.put(workEndDate, absTimeDetailList);
                        }
                    }
                    /*旷工算作缺勤*/
                    if (Util.getDoubleValue(absenteeismmins, 0.00) > 0.00) {
                        hasAbsent = true;

                        absTimeDetailList = (List) absTimeMap.get(kqDate);
                        if (absTimeDetailList == null) {
                            absTimeDetailList = new ArrayList<Map<String, Object>>();
                        }

                        absTimeDetailMap = new HashMap<String, Object>();
                        absTimeDetailMap.put("bgColor", "#CCCCFF");
                        absTimeDetailMap.put("time", workBeginTime + "-24:00");
                        absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20085, user.getLanguage())});
                        absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20085, user.getLanguage()));//旷工
                        absTimeDetailList.add(absTimeDetailMap);
                        absTimeMap.put(kqDate, absTimeDetailList);

                        absTimeDetailList = (List) absTimeMap.get(workEndDate);
                        if (absTimeDetailList == null) {
                            absTimeDetailList = new ArrayList<Map<String, Object>>();
                        }

                        absTimeDetailMap = new HashMap<String, Object>();
                        absTimeDetailMap.put("bgColor", "#CCCCFF");
                        absTimeDetailMap.put("time", "00:00-" + workEndTime);
                        absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + workBeginDate + " " + workBeginTime, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + workEndDate + " " + workEndTime, SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20085, user.getLanguage())});
                        absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20085, user.getLanguage()));//旷工
                        absTimeDetailList.add(absTimeDetailMap);
                        absTimeMap.put(kqDate, absTimeDetailList);
                    }
                } else {
                    //弹性工作制
                    String kqType = groupComInfo.getKqtype(groupId);
                    if (kqType.equals("3")) {
                        String signStart = groupComInfo.getSignstart(groupId);

                        /*上班时段*/
                        sclTimeDetailList = (List<String>) sclTimeMap.get(kqDate);
                        if (sclTimeDetailList == null) {
                            sclTimeDetailList = new ArrayList<String>();
                        }
                        String temp = signStart + "-24:00";
                        if (!sclTimeDetailList.contains(temp)) {
                            sclTimeDetailList.add(temp);
                        }
                        sclTimeMap.put(kqDate, sclTimeDetailList);

                        /*旷工算作缺勤*/
                        if (Util.getDoubleValue(absenteeismmins, 0.00) > 0.00) {
                            hasAbsent = true;

                            absTimeDetailList = (List) absTimeMap.get(kqDate);
                            if (absTimeDetailList == null) {
                                absTimeDetailList = new ArrayList<Map<String, Object>>();
                            }

                            absTimeDetailMap = new HashMap<String, Object>();
                            absTimeDetailMap.put("bgColor", "#CCCCFF");
                            absTimeDetailMap.put("time", signStart + "-24:00");
                            absTimeDetailMap.put("tips", new String[]{SystemEnv.getHtmlLabelName(505421, user.getLanguage()) + "：" + kqDate + " " + signStart, SystemEnv.getHtmlLabelName(505423, user.getLanguage()) + "：" + kqDate + " 24:00", SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + SystemEnv.getHtmlLabelName(20085, user.getLanguage())});
                            absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(20085, user.getLanguage()));//旷工
                            absTimeDetailList.add(absTimeDetailMap);

                            absTimeMap.put(kqDate, absTimeDetailList);
                        }
                    }
                }
            }

            /*请假流程*/
            List<String> leaveTypeList = new ArrayList<String>();

            Map<String, Object> paramsMap = new HashMap<String, Object>();
            paramsMap.put("typeselect", "6");
            paramsMap.put("tabKey", "1");
            paramsMap.put("kqtype", "" + KqSplitFlowTypeEnum.LEAVE.getFlowtype());
            paramsMap.put("resourceId", resourceId);
            paramsMap.put("fromDate", fromDate);
            paramsMap.put("toDate", toDate);

            KQAttFlowSetBiz kqAttFlowSetBiz = new KQAttFlowSetBiz();
            Map<String, String> sqlMap = kqAttFlowSetBiz.getFLowSql(paramsMap, user);
            String sqlFrom = sqlMap.get("from");
            String sqlWhere = sqlMap.get("where");
            sql = "select * " + sqlFrom + sqlWhere;
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String requestId = recordSet.getString("requestId");
                String leaveType = recordSet.getString("newLeaveType");
                double duration = Util.getDoubleValue(recordSet.getString("duration"), 0.00);
                double backDuration = Util.getDoubleValue(recordSet.getString("backDuraion"),0.00);
                String durationRule = recordSet.getString("durationRule");
                String fromDateTemp = recordSet.getString("fromDate");
                String fromTimeTemp = recordSet.getString("fromTime");
                String toDateTemp = recordSet.getString("toDate");
                String toTimeTemp = recordSet.getString("toTime");
                String requestName = recordSet.getString("requestName");

                if (backDuration >= duration) {
                    continue;
                }
                if (!leaveTypeList.contains(leaveType)) {
                  leaveTypeList.add(leaveType);
                }
                Map<String,List<String>> leaveBackMap = Maps.newHashMap();
                if(backDuration > 0.0){
                  leaveBackMap = getLeaveBackList(requestId);
                }

                String unitName = "";//单位名称，天/小时
                if (durationRule.equals("1") || durationRule.equals("2") || durationRule.equals("4")) {
                    unitName = SystemEnv.getHtmlLabelName(389325, user.getLanguage());//(天)
                } else {
                    unitName = SystemEnv.getHtmlLabelName(389326, user.getLanguage());//(小时)
                }
                String timeselection = "";
                if ("2".equalsIgnoreCase(durationRule)) {
                  if (leaveType.length() > 0) {
                    timeselection = rulesComInfo.getTimeSelection(leaveType);
                  }
                }

                boolean isEnd = false;
                for (String date = fromDateTemp; !isEnd; date = DateUtil.getDate(date, 1)) {
                    if (date.equals(toDateTemp)) {
                        isEnd = true;
                    }
                    absTimeDetailList = (List<Map<String, Object>>) absTimeMap.get(date);
                    if (absTimeDetailList == null) {
                        absTimeDetailList = new ArrayList<Map<String, Object>>();
                    }
                    absTimeDetailMap = new HashMap<String, Object>();
                    absTimeDetailMap.put("bgColor", leaveTypeColorMap.get(leaveType));
                   String fromtime_ = fromTimeTemp;
                    String toTime_ = toTimeTemp;
                  if("2".equalsIgnoreCase(durationRule) && "1".equalsIgnoreCase(timeselection)){
                    Map<String,String> half_map = kqTimeSelectionComInfo.getTimeselections("0",leaveType,durationRule);
                    Map<String, Object> apm_Map = getTimeDetail4Half(half_map,fromTimeTemp,toTimeTemp);
                    fromtime_ = Util.null2String(apm_Map.get("am"));
                    toTime_ = Util.null2String(apm_Map.get("pm"));
                  }
                  absTimeDetailMap.put("tips", new String[]{"<a href=javaScript:openFullWindowHaveBarForWFList('/workflow/request/ViewRequestForwardSPA.jsp?requestid=" + requestId + "&isovertime=0'," + requestId + ");>" + requestName + "</a>",
                      SystemEnv.getHtmlLabelName(500371, user.getLanguage()) + "：" + fromDateTemp + " " + fromtime_, SystemEnv.getHtmlLabelName(500372, user.getLanguage()) + "：" + toDateTemp + " " + toTime_,
                      SystemEnv.getHtmlLabelName(63, user.getLanguage()) + "：" + Util.formatMultiLang(rulesComInfo.getLeaveName(leaveType), "" + user.getLanguage()), SystemEnv.getHtmlLabelName(21551, user.getLanguage()) + unitName + "：" + String.format("%.2f",duration)+
                      (backDuration > 0.0 ? ("("+SystemEnv.getHtmlLabelName(24473,user.getLanguage())+":"+String.format("%.2f",backDuration)+")") : "")});

                  absTimeDetailMap.put("type", SystemEnv.getHtmlLabelName(670, user.getLanguage()));
                    if (date.equals(fromDateTemp) && !date.equals(toDateTemp)) {
                      if(!leaveBackMap.isEmpty() && leaveBackMap.containsKey(date)){
                        List<String> leaveBackList = leaveBackMap.get(date);
                        for(int i = 0 ; i < leaveBackList.size() ; i++){
                          if(i == 0){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", fromTimeTemp + "-" + leaveBackList.get(0));
                            absTimeDetailList.add(tmpMap);
                          }else if(i == leaveBackList.size()-1){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(leaveBackList.size()-1) + "-" + "24:00");
                            absTimeDetailList.add(tmpMap);
                          }else{
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(i) + "-" + leaveBackList.get(i+1));
                            absTimeDetailList.add(tmpMap);
                          }
                        }
                      }else{
                        absTimeDetailMap.put("time", fromTimeTemp + "-" + "24:00");
                        absTimeDetailList.add(absTimeDetailMap);
                      }
                    }
                    if (date.equals(fromDateTemp) && date.equals(toDateTemp)) {
                      if(!leaveBackMap.isEmpty() && leaveBackMap.containsKey(date)){
                        List<String> leaveBackList = leaveBackMap.get(date);
                        for(int i = 0 ; i < leaveBackList.size() ; i++){
                          if(i == 0){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", fromTimeTemp + "-" + leaveBackList.get(0));
                            absTimeDetailList.add(tmpMap);
                          }else if(i == leaveBackList.size()-1){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(leaveBackList.size()-1) + "-" + toTimeTemp);
                            absTimeDetailList.add(tmpMap);
                          }else{
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(i) + "-" + leaveBackList.get(i+1));
                            absTimeDetailList.add(tmpMap);
                          }
                        }
                      }else{
                        absTimeDetailMap.put("time", fromTimeTemp + "-" + toTimeTemp);
                        absTimeDetailList.add(absTimeDetailMap);
                      }
                    }
                    if (!date.equals(fromDateTemp) && date.equals(toDateTemp)) {
                      if(!leaveBackMap.isEmpty() && leaveBackMap.containsKey(date)){
                        List<String> leaveBackList = leaveBackMap.get(date);
                        for(int i = 0 ; i < leaveBackList.size() ; i++){
                          if(i == 0){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", "00:00" + "-" + leaveBackList.get(0));
                            absTimeDetailList.add(tmpMap);
                          }else if(i == leaveBackList.size()-1){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(leaveBackList.size()-1) + "-" + toTimeTemp);
                            absTimeDetailList.add(tmpMap);
                          }else{
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(i) + "-" + leaveBackList.get(i+1));
                            absTimeDetailList.add(tmpMap);
                          }
                        }
                      }else{
                        absTimeDetailMap.put("time", "00:00-" + toTimeTemp);
                        absTimeDetailList.add(absTimeDetailMap);
                      }
                    }
                    if (date.compareTo(fromDateTemp) > 0 && date.compareTo(toDateTemp) < 0) {
                      if(!leaveBackMap.isEmpty() && leaveBackMap.containsKey(date)){
                        List<String> leaveBackList = leaveBackMap.get(date);
                        for(int i = 0 ; i < leaveBackList.size() ; i++){
                          if(i == 0){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", "00:00" + "-" + leaveBackList.get(0));
                            absTimeDetailList.add(tmpMap);
                          }else if(i == leaveBackList.size()-1){
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(leaveBackList.size()-1) + "-" + "24:00");
                            absTimeDetailList.add(tmpMap);
                          }else{
                            Map<String,Object> tmpMap = Maps.newHashMap();
                            tmpMap.putAll(absTimeDetailMap);
                            tmpMap.put("time", leaveBackList.get(i) + "-" + leaveBackList.get(i+1));
                            absTimeDetailList.add(tmpMap);
                          }
                        }
                      }else{
                        absTimeDetailMap.put("time", "00:00-24:00");
                        absTimeDetailList.add(absTimeDetailMap);
                      }
                    }
                    absTimeMap.put(date, absTimeDetailList);
                }
            }

            /*拼凑数据给前端展现*/
            List<Map<String, Object>> typesList = new ArrayList<Map<String, Object>>();
            Map<String, Object> typeMap = new HashMap<String, Object>();
            for (String leaveType : leaveTypeList) {
                typeMap = new HashMap<String, Object>();
                typeMap.put("bgcolor", leaveTypeColorMap.get(leaveType));
                typeMap.put("value", rulesComInfo.getLeaveName(leaveType));

                typesList.add(typeMap);
            }
            if (hasLate) {
                typeMap = new HashMap<String, Object>();
                typeMap.put("bgcolor", "#FFCCFF");
                typeMap.put("value", SystemEnv.getHtmlLabelName(20081, user.getLanguage()));
                typesList.add(typeMap);
            }

            if (hasEarly) {
                typeMap = new HashMap<String, Object>();
                typeMap.put("bgcolor", "#6EBF70");
                typeMap.put("value", SystemEnv.getHtmlLabelName(20082, user.getLanguage()));
                typesList.add(typeMap);
            }

            if (hasAbsent) {
                typeMap = new HashMap<String, Object>();
                typeMap.put("bgcolor", "#CCCCFF");
                typeMap.put("value", SystemEnv.getHtmlLabelName(20085, user.getLanguage()));
                typesList.add(typeMap);
            }

            resultMap.put("types", typesList);

            List<Map<String, Object>> detailsList = new ArrayList<Map<String, Object>>();
            Map<String, Object> detailsMap = new HashMap<String, Object>();
            double daySum = 0.00;
            boolean isEnd = false;
            for (String date = fromDate; !isEnd; date = DateUtil.getDate(date, 1)) {
                if (date.equals(toDate)) {
                    isEnd = true;
                }
                String day = (String) dayMap.get(date);
                if (day == null || "".equals(day)) {
                    continue;
                }

                daySum = daySum + Util.getDoubleValue(day, 0.00);
                //如果取不到上班时间，默认未09:00-18:00，否则点击未出勤明细会闪退
                if (sclTimeMap.get(date) == null || "".equals(sclTimeMap.get(date))) {
                    List<String> temp = new ArrayList<String>();
                    temp.add("09:00-18:00");
                    sclTimeMap.put(date, temp);
                }

                detailsMap = new HashMap<String, Object>();
                detailsMap.put("day", day);
                detailsMap.put("date", kqTransMethod.getBelongDateShow(date, "" + user.getLanguage()));
                detailsMap.put("sclTime", sclTimeMap.get(date));
                if(!absTimeMap.containsKey(date)){
                  continue;
                }
                detailsMap.put("absTime", absTimeMap.get(date));

                detailsList.add(detailsMap);
            }
            resultMap.put("absenceDay", String.format("%.2f", daySum));
            resultMap.put("details", detailsList);
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(10000831, Util.getIntValue(user.getLanguage())));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513188, user.getLanguage()).replace("{lastName}", lastName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

  public Map<String,List<String>> getLeaveBackList(String requestId) {
      Map<String,List<String>> leaveBackMap = Maps.newHashMap();
    RecordSet recordSet1 = new RecordSet();
    String back_sql = "select distinct requestid,fromdatedb,fromtimedb,todatedb,totimedb,durationrule,newleavetype from kq_flow_split_leaveback where leavebackrequestid=?  order by fromdatedb, fromtimedb ";
    recordSet1.executeQuery(back_sql, requestId);
    while (recordSet1.next()){
      String back_fromdatedb = recordSet1.getString("fromdatedb");
      String back_fromtimedb = recordSet1.getString("fromtimedb");
      String back_todatedb = recordSet1.getString("todatedb");
      String back_totimedb = recordSet1.getString("totimedb");
      String durationrule = recordSet1.getString("durationrule");
      String newleavetype = recordSet1.getString("newleavetype");
      boolean isEnd = false;
      for (String date = back_fromdatedb; !isEnd; date = DateUtil.getDate(date, 1)) {
        if (date.equals(back_todatedb)) {
          isEnd = true;
        }
        List<String> leaveBackListTmp = Lists.newArrayList();
        if(leaveBackMap.containsKey(date)){
          leaveBackListTmp = leaveBackMap.get(date);
        }
        if (date.equals(back_fromdatedb) && !date.equals(back_todatedb)) {
          leaveBackListTmp.add(back_fromtimedb);
          leaveBackListTmp.add("24:00");
        }
        if (date.equals(back_fromdatedb) && date.equals(back_todatedb)) {
          leaveBackListTmp.add(back_fromtimedb);
          leaveBackListTmp.add(back_totimedb);
        }
        if (!date.equals(back_fromdatedb) && date.equals(back_todatedb)) {
          leaveBackListTmp.add("00:00");
          leaveBackListTmp.add(back_totimedb);
        }
        if (date.compareTo(back_fromdatedb) > 0 && date.compareTo(back_todatedb) < 0) {
          leaveBackListTmp.add("00:00");
          leaveBackListTmp.add("24:00");
        }
        leaveBackMap.put(date, leaveBackListTmp);
      }
    }
    return leaveBackMap;
  }

  /**
   * 半天时间的显示
   * @param half_map
   * @param fromTimeTemp
   * @param toTimeTemp
   */
  public Map<String, Object> getTimeDetail4Half(Map<String, String> half_map, String fromTimeTemp, String toTimeTemp) {
    Map<String, Object> apm_Map = new HashMap<>();
    String cus_am = SystemEnv.getHtmlLabelName(16689, user.getLanguage());
    String cus_pm = SystemEnv.getHtmlLabelName(16690, user.getLanguage());
    if(half_map != null && !half_map.isEmpty()){
      cus_am = Util.null2String(half_map.get("half_on"));
      cus_pm = Util.null2String(half_map.get("half_off"));
    }
    String start = cus_am;
    String end = cus_pm;
    if (fromTimeTemp.equalsIgnoreCase(SplitSelectSet.forenoon_start)) {
      start = cus_am;
    } else if (fromTimeTemp.equalsIgnoreCase(SplitSelectSet.forenoon_end)) {
      start = cus_pm;
    }
    if (toTimeTemp.equalsIgnoreCase(SplitSelectSet.afternoon_start)) {
      end = cus_am;
    } else if (toTimeTemp.equalsIgnoreCase(SplitSelectSet.afternoon_end)) {
      end = cus_pm;
    }
    apm_Map.put("am", start);
    apm_Map.put("pm", end);
    return apm_Map;
  }

  /**
     * 获取实际出勤天数的明细列表
     *
     * @return
     */
    public Map<String, Object> getRealWorkdaysList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**
             * 获取明细列表
             */
            String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid,serialid as serialid1, serialid as serialid2," +
                    " workbegintime,workendtime,signintime,signouttime, attendanceMins, signMins ";
            String sqlFrom  = " from hrmresource a, kq_format_detail b  ";
            String sqlWhere = " where a.id = b.resourceid and (attendanceMins>0 or signMins>0)";
            String orderBy = " kqdate asc, workbegintime asc " ;
            if (fromDate.length() > 0) {
                sqlWhere += " and kqdate >= '" + fromDate + "'";
            }
            if (toDate.length() > 0) {
                sqlWhere += " and kqdate <= '" + toDate + "'";
            }
            if (resourceId.length() > 0) {
                sqlWhere += " and resourceid = " + resourceId;
            }

            String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");
            String tableString = "" +
                    "<table pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"checkbox\">" +
                    "<sql backfields=\"" + backFields + "\" sqlform=\"" + Util.toHtmlForSplitPage(sqlFrom) + "\" sqlprimarykey=\"a.id\" sqlorderby=\"" + orderBy + "\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"/>" +
                    "<head>" +
                    "		<col width=\"8%\" text=\""+SystemEnv.getHtmlLabelName(413,user.getLanguage())+"\" column=\"lastname\" orderkey=\"lastname\"/>"+
                    "		<col width=\"8%\" text=\""+SystemEnv.getHtmlLabelName(714,user.getLanguage())+"\" column=\"workcode\" orderkey=\"workcode\"/>"+
                    "		<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(124,user.getLanguage())+"\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>"+
                    "		<col width=\"9%\" text=\""+SystemEnv.getHtmlLabelName(602,user.getLanguage())+"\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\""+user.getLanguage()+"\"/>"+
                    "		<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(97,user.getLanguage())+"\" column=\"kqdate\" orderkey=\"kqdate\"/>"+
                    "		<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(390054,user.getLanguage())+"\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>"+
                    "		<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(390495,user.getLanguage())+"\" column=\"serialid1\" orderkey=\"serialid1\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignInTime\" otherpara=\"column:signintime+column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>"+
                    "		<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(390496,user.getLanguage())+"\" column=\"serialid2\" orderkey=\"serialid2\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignOutTime\" otherpara=\"column:signouttime+column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>"+
                    "		<col width=\"9%\" text=\""+SystemEnv.getHtmlLabelName(391040,user.getLanguage())+"\" column=\"attendanceMins\" orderkey=\"attendanceMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />"+
                    "		<col width=\"9%\" text=\""+SystemEnv.getHtmlLabelName(504433,user.getLanguage())+"\" column=\"signMins\" orderkey=\"signMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />"+
                    "</head>" +
                    "</table>";
            //主要用于 显示定制列以及 表格 每页展示记录数选择
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionkey, tableString);

            resultMap.put("sessionkey", sessionkey);
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取旷工的明细列表
     *
     * @return
     */
    public Map<String, Object> getAbsentList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**
             * 获取明细列表
             */
            String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid, serialid as serialid1," +
                    " workbegintime,workendtime, signintime,signouttime,absenteeismmins ";

            String sqlFrom = "from hrmresource a, kq_format_detail b  ";
            String sqlWhere = " where a.id = b.resourceid and absenteeismMins>0 ";
            String orderBy = " kqdate asc, workbegintime asc ";
            if (fromDate.length() > 0) {
                sqlWhere += " and kqdate >= '" + fromDate + "'";
            }

            if (toDate.length() > 0) {
                sqlWhere += " and kqdate <= '" + toDate + "'";
            }

            if (resourceId.length() > 0) {
                sqlWhere += " and resourceid = " + resourceId;
            }

            String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");
            String tableString = "" +
                    "<table pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"none\">" +
                    "<sql backfields=\"" + backFields + "\" sqlform=\"" + Util.toHtmlForSplitPage(sqlFrom) + "\" sqlprimarykey=\"a.id\" sqlorderby=\"" + orderBy + "\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"/>" +
                    "<head>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(413, user.getLanguage()) + "\" column=\"lastname\" orderkey=\"lastname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(714, user.getLanguage()) + "\" column=\"workcode\" orderkey=\"workcode\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(124, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(602, user.getLanguage()) + "\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\"" + user.getLanguage() + "\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(97, user.getLanguage()) + "\" column=\"kqdate\" orderkey=\"kqdate\"/>" +
                    "				<col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(390054, user.getLanguage()) + "\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(18949, user.getLanguage()) + "\" column=\"serialid1\" orderkey=\"serialid1\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignTime\" otherpara=\"column:signintime+column:signouttime+column:kqdate+column:resourceid+" + user.getLanguage() + "\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(391416, user.getLanguage()) + "\" column=\"absenteeismMins\" orderkey=\"absenteeismMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />" +
                    "</head>" +
                    "</table>";

            //主要用于 显示定制列以及 表格 每页展示记录数选择
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionkey, tableString);

            resultMap.put("sessionkey", sessionkey);
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取迟到明细列表
     *
     * @return
     */
    public Map<String, Object> getBeLateList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?
            String tabKey = Util.null2String(params.get("tabKey"));//是迟到还是严重迟到：2-严重迟到、其他-迟到

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**
             * 获取明细列表
             */
            String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid, serialid as serialid1," +
                    " workbegintime,workendtime, signintime,signouttime, beLateMins, graveBeLateMins ";
            String sqlFrom = "from hrmresource a, kq_format_detail b  ";
            String sqlWhere = " where a.id = b.resourceid";
            String orderBy = " kqdate asc, workbegintime asc ";
            if (fromDate.length() > 0) {
                sqlWhere += " and kqdate >= '" + fromDate + "'";
            }

            if (toDate.length() > 0) {
                sqlWhere += " and kqdate <= '" + toDate + "'";
            }

            if (resourceId.length() > 0) {
                sqlWhere += " and resourceid = " + resourceId;
            }

            if (tabKey.equals("2")) {
                sqlWhere += " and graveBeLateMins>0 ";
            } else {
                sqlWhere += " and beLateMins>0 ";
            }

            String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");
            String tableString = "" +
                    "<table pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"none\">" +
                    "<sql backfields=\"" + backFields + "\" sqlform=\"" + Util.toHtmlForSplitPage(sqlFrom) + "\" sqlprimarykey=\"a.id\" sqlorderby=\"" + orderBy + "\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"/>" +
                    "<head>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(413, user.getLanguage()) + "\" column=\"lastname\" orderkey=\"lastname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(714, user.getLanguage()) + "\" column=\"workcode\" orderkey=\"workcode\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(124, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(602, user.getLanguage()) + "\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\"" + user.getLanguage() + "\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(97, user.getLanguage()) + "\" column=\"kqdate\" orderkey=\"kqdate\"/>" +
                    "				<col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(390054, user.getLanguage()) + "\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(18949, user.getLanguage()) + "\" column=\"serialid1\" orderkey=\"serialid1\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignTime\" otherpara=\"column:signintime++column:kqdate+column:resourceid+" + user.getLanguage() + "\"/>";
            if (tabKey.equals("2")) {
                tableString += "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(391413, user.getLanguage()) + "\" column=\"graveBeLateMins\" orderkey=\"graveBeLateMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />";
            } else {
                tableString += "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(391413, user.getLanguage()) + "\" column=\"beLateMins\" orderkey=\"beLateMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />";
            }
            tableString += "</head>" +
                    "</table>";

            //主要用于 显示定制列以及 表格 每页展示记录数选择
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionkey, tableString);

            resultMap.put("sessionkey", sessionkey);
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取早退的明细列表
     *
     * @return
     */
    public Map<String, Object> getLeaveEearlyList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?
            String tabKey = Util.null2String(params.get("tabKey"));//是早退还是严重早退：2-严重早退、其他-早退

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**
             * 获取明细列表
             */
            String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid," +
                    " workbegintime,workendtime, signintime,signouttime, leaveEarlyMins, graveLeaveEarlyMins ";
            String sqlFrom = "from hrmresource a, kq_format_detail b  ";
            String sqlWhere = " where a.id = b.resourceid";
            String orderBy = " kqdate asc, workbegintime asc ";
            if (fromDate.length() > 0) {
                sqlWhere += " and kqdate >= '" + fromDate + "'";
            }

            if (toDate.length() > 0) {
                sqlWhere += " and kqdate <= '" + toDate + "'";
            }

            if (resourceId.length() > 0) {
                sqlWhere += " and resourceid = " + resourceId;
            }

            if (tabKey.equals("2")) {
                sqlWhere += " and graveLeaveEarlyMins>0 ";
            } else {
                sqlWhere += " and leaveEarlyMins>0 ";
            }

            String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");

            String tableString = "" +
                    "<table pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"none\">" +
                    "<sql backfields=\"" + backFields + "\" sqlform=\"" + Util.toHtmlForSplitPage(sqlFrom) + "\" sqlprimarykey=\"a.id\" sqlorderby=\"" + orderBy + "\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"/>" +
                    "<head>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(413, user.getLanguage()) + "\" column=\"lastname\" orderkey=\"lastname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(714, user.getLanguage()) + "\" column=\"workcode\" orderkey=\"workcode\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(124, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(602, user.getLanguage()) + "\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\"" + user.getLanguage() + "\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(97, user.getLanguage()) + "\" column=\"kqdate\" orderkey=\"kqdate\"/>" +
                    "				<col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(390054, user.getLanguage()) + "\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(18949, user.getLanguage()) + "\" column=\"serialid1\" orderkey=\"serialid1\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignTime\" otherpara=\"+column:signouttime+column:kqdate+column:resourceid+" + user.getLanguage() + "\"/>";
            if (tabKey.equals("2")) {
                tableString += "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(391414, user.getLanguage()) + "\" column=\"graveLeaveEarlyMins\" orderkey=\"graveLeaveEarlyMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />";
            } else {
                tableString += "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(391414, user.getLanguage()) + "\" column=\"leaveEarlyMins\" orderkey=\"leaveEarlyMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />";
            }
            tableString += "</head>" +
                    "</table>";

            //主要用于 显示定制列以及 表格 每页展示记录数选择
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionkey, tableString);

            resultMap.put("sessionkey", sessionkey);
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取漏签的明细列表
     *
     * @return
     */
    public Map<String, Object> getForgotCheckList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**
             * 获取明细列表
             */
            String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid, serialid as serialid1, serialid as serialid2," +
                    " workbegintime,workendtime, signintime,signouttime,case when forgotcheckmins> 0 then 1 when forgotBeginWorkCheckMins> 0 then 1 else 0 end as forgotcheck " ;

            String sqlFrom  = "from hrmresource a, kq_format_detail b  ";
            String sqlWhere = " where a.id = b.resourceid and (forgotCheckMins>0 or forgotBeginWorkCheckMins>0)";
            String orderby = " kqdate asc, workbegintime asc " ;
            String tableString = "";

            if (fromDate.length() > 0) {
                sqlWhere += " and kqdate >= '" + fromDate + "'";
            }

            if (toDate.length() > 0) {
                sqlWhere += " and kqdate <= '" + toDate + "'";
            }

            if (resourceId.length() > 0) {
                sqlWhere += " and resourceid = " + resourceId;
            }

            String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");

            tableString = "" +
                    "<table pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"none\">" +
                    "<sql backfields=\"" + backFields + "\" sqlform=\"" + Util.toHtmlForSplitPage(sqlFrom) + "\" sqlprimarykey=\"a.id\" sqlorderby=\"" + orderby + "\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"/>" +
                    "<head>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(413, user.getLanguage()) + "\" column=\"lastname\" orderkey=\"lastname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(714, user.getLanguage()) + "\" column=\"workcode\" orderkey=\"workcode\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(124, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>" +
                    "				<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(602, user.getLanguage()) + "\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\"" + user.getLanguage() + "\"/>" +
                    "				<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(97, user.getLanguage()) + "\" column=\"kqdate\" orderkey=\"kqdate\"/>" +
                    "               <col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(390054,user.getLanguage())+"\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>"+
                    "				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(390495,user.getLanguage())+"\" column=\"serialid1\" orderkey=\"serialid1\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignInTime\" otherpara=\"column:signintime+column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>"+
                    "				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(390496,user.getLanguage())+"\" column=\"serialid2\" orderkey=\"serialid2\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignOutTime\" otherpara=\"column:signouttime+column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>"+
                    "				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(34099,user.getLanguage())+"\" column=\"forgotCheck\" orderkey=\"forgotCheck\" />"+
                    "</head>" +
                    "</table>";

            //主要用于 显示定制列以及 表格 每页展示记录数选择
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionkey, tableString);

            resultMap.put("sessionkey", sessionkey);
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取请假、加班、出差、公出、异常数据抵冲的明细列表
     *
     * @return
     */
    public Map<String, Object> getLeaveList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String kqtype = Util.null2String(params.get("kqtype"));
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            params.put("tabKey", "1");
            params.put("resourceId", resourceId);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
            params.put("typeselect", "6");
            params.put("isNoAccount","1");

            if (kqtype.equalsIgnoreCase("overtime")) {//加班
                params.put("kqtype", KqSplitFlowTypeEnum.OVERTIME.getFlowtype());
            } else if (kqtype.equalsIgnoreCase("evection")) {//出差
                params.put("kqtype", KqSplitFlowTypeEnum.EVECTION.getFlowtype());
            } else if (kqtype.equalsIgnoreCase("outDays")) {//公出
                params.put("kqtype", KqSplitFlowTypeEnum.OUT.getFlowtype());
            } else if (kqtype.startsWith("leaveType_")) {
                String[] typeInfo = Util.splitString(kqtype, "_");
                if (typeInfo[0].equals("leaveType")) {
                    params.put("kqtype", KqSplitFlowTypeEnum.LEAVE.getFlowtype());
                    params.put("newleavetype", typeInfo[1]);
                }
            }

            resultMap.put("sessionkey", new KQAttFlowSetBiz().buildFlowSetTableString(params, user));
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            writeLog(e);
        }
        return resultMap;
    }
}
