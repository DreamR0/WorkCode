package com.engine.kq.util;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionOption;
import com.api.customization.qc2304359.KqReportUtil;
import com.engine.kq.biz.*;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.enums.KQSettingsEnum;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.wfset.util.SplitSelectSet;
import java.text.DecimalFormat;
import java.util.*;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.workflow.WorkflowComInfo;
import weaver.workflow.workflow.WorkflowRequestComInfo;

public class TransMethod extends BaseBean {

    public static Boolean getIsHave() {
        return isHave;
    }

    public static void setIsHave(Boolean isHave) {
        TransMethod.isHave = isHave;
    }

    //qc2304359 判断是否来自迟到或者早退
    private  static Boolean isHave=false;

	private static DecimalFormat df = new DecimalFormat("0.00");

    public ArrayList<String> getOperateByGroup(String id, String otherPara) {
        ArrayList<String> resultList = new ArrayList<String>();
        String[] splitStr = Util.splitString(otherPara, "+");
        String kqType = Util.null2String(splitStr[0]);
        String subcompanyid = Util.null2String(splitStr[1]);
        String userid = Util.null2String(splitStr[2]);
        int resourceid = Util.getIntValue(userid);
        CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
        int operatelevel = checkSubCompanyRight.ChkComRightByUserRightCompanyId(resourceid, "HrmKQGroup:Add", Util.getIntValue(subcompanyid, -1));
        if (operatelevel > 0) {
            resultList.add("true");
            resultList.add("true");
            resultList.add("true");
            resultList.add(String.valueOf(kqType.equals("2")));
        }else{
            resultList.add("false");
            resultList.add("false");
            resultList.add("false");
            resultList.add("false");
        }
        if (operatelevel > 1) {
            resultList.add(this.getKQGroupCheckbox(id));
        }else{
            resultList.add("false");
        }
        resultList.add("true");
        return resultList;
    }
	
	public ArrayList<String> getOperate(String id, String kqType){
		ArrayList<String> resultList = new ArrayList<String>();

		resultList.add("true");
		resultList.add("true");
		resultList.add("true");
		resultList.add(String.valueOf(kqType.equals("2")));
		resultList.add(this.getKQGroupCheckbox(id));
		resultList.add("true");
		return resultList;
	}

  public ArrayList<String> getGroupMembersOperate(String id, String groupId){
    ArrayList<String> resultList = new ArrayList<String>();
    String result = getKQGroupMembersCheckbox(id+"+"+groupId);

    resultList.add(result);
    return resultList;
  }

  public String getKQGroupName(String groupname, String otherPara){
    String kqGroupName = groupname;
    String[] splitStr = Util.splitString(otherPara, "+");
    String groupid = Util.null2String(splitStr[0]);
    String resourceid = Util.null2String(splitStr[1]);
    String kqdate = Util.null2String(splitStr[2]);
    String strLanguage = Util.null2String(splitStr[3]);
    if(resourceid.length()>0){
      int language = Util.getIntValue(strLanguage,7);
      if(kqdate.length()==0) {
        kqdate = DateUtil.getCurrentDate();
      }
      String currentGroupId = Util.null2String(new KQGroupMemberComInfo().getKQGroupId(resourceid,kqdate));
      if(groupid.equals(currentGroupId)) {
        kqGroupName += "(<span style=\"color:#F00\">"+ SystemEnv.getHtmlLabelName(509551, language)+"</span>)";
      }
    }
    return kqGroupName;
  }

	public int getGroupUserCount(String groupid){
		int count = 0;
		RecordSet rs = new RecordSet();
		String sql = "";
		sql = " SELECT count(distinct id) FROM ( "+
						" SELECT a.id,a.status FROM HrmResource a, kq_groupmember b "+
						" WHERE (a.id=b.typevalue and b.type =1 and (isdelete is null or isdelete <> '1') and groupid = "+groupid+" ) "+
						" UNION ALL "+
						" SELECT a.id,a.status FROM HrmResource a, kq_groupmember b "+
						" WHERE (a.subcompanyid1 = b.typevalue AND a.seclevel>=b.seclevel AND a.seclevel<=b.seclevelto AND b.type=2 and (isdelete is null or isdelete <> '1') and groupid = "+groupid+" ) "+
						" UNION ALL "+
						" SELECT a.id,a.status FROM HrmResource a, kq_groupmember b "+
						" WHERE (a.departmentid = b.typevalue AND a.seclevel>=b.seclevel AND a.seclevel<=b.seclevelto AND b.type=3 and (isdelete is null or isdelete <> '1') and groupid = "+groupid+" ) "+
						" UNION ALL "+
						" SELECT a.id,a.status FROM HrmResource a, kq_groupmember b "+
						" WHERE  (a.jobtitle = b.typevalue AND b.type=5  and (isdelete is null or isdelete <> '1')  and groupid = "+groupid+" AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND a.subcompanyid1 IN(b.jobtitlelevelvalue)) OR (b.jobtitlelevel=3 AND a.departmentid IN(b.jobtitlelevelvalue))))" +
						" UNION ALL "+
						" select a.id,a.status from hrmresource a where seclevel>=(select min(seclevel) from kq_groupmember where type=6 and (isdelete is null or isdelete <> '1') and groupid = "+groupid+" ) and seclevel<=  (select max(seclevelto) from kq_groupmember where type=6 and (isdelete is null or isdelete <> '1') and groupid = "+groupid+" )) t" +
						" where t.status in(0,1,2,3) ";
		rs.executeQuery(sql) ;
		if(rs.next()){
			count=rs.getInt(1);
		}
		return count;
	}

	public String getKQTypeName(String kqtype, String strLanguage){
		int language = Util.getIntValue(strLanguage,7);
		String kQTypeName = "";
		if(kqtype.equals("1")){
			kQTypeName = SystemEnv.getHtmlLabelName(389127,language);
		}else if(kqtype.equals("2")){
			kQTypeName = SystemEnv.getHtmlLabelName(389128,language);
		}else if(kqtype.equals("3")){
			kQTypeName = SystemEnv.getHtmlLabelName(520551,language);
		}
		return kQTypeName;
	}

	public String getKQGroupDetial(String id,String otherPara){
		String kQGroupDetial = "";
		String[] splitStr = Util.splitString(otherPara, "+");
		String kqtype = Util.null2String(splitStr[0]);
		int language = Util.getIntValue(splitStr[1],7);
		KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
		ShiftManagementToolKit shiftManagementToolKit = new ShiftManagementToolKit();

		if(kqtype.equals("1")){
			String sql = "";
			RecordSet rs = new RecordSet();
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			sql = "select * from kq_fixedschedulce where groupid = ? order by weekday ";
			rs.executeQuery(sql,id);
			while(rs.next()){
				int weekday = rs.getInt("weekday");
				String serialid = Util.null2String(rs.getString("serialid"));
				if(serialid.length()==0)serialid="0";
				if(map.get(serialid)==null){
					map.put(serialid,UtilKQ.getWeekDay(weekday,language) );
				}else{
					String value = map.get(serialid);
					value=value+"、"+UtilKQ.getWeekDay(weekday,language);
					map.put(serialid,value);
				}
			}
			Iterator<Map.Entry<String,String>> iter = map.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> entry = iter.next();
				String serialid = entry.getKey();
				String weeks = entry.getValue();
				if(kQGroupDetial.length()>0)kQGroupDetial+="<br>";
				if(serialid.equals("0")){
					kQGroupDetial += weeks +  SystemEnv.getHtmlLabelName(26593,language);
				}else{
					kQGroupDetial += weeks + shiftManagementToolKit.getShiftOnOffWorkSections(serialid,language);
				}
			}
		}else if(kqtype.equals("2")){
			List<String> serialids = Util.splitString2List(kqGroupComInfo.getSerialids(id),",") ;
			List<String> lsSerialids = new ArrayList<>();
			for (String serialid : serialids) {
				if(Util.null2String(serialid).length()==0 || lsSerialids.contains(serialid)
								||Util.null2String(shiftManagementToolKit.getShiftOnOffWorkSections(serialid,language)).length()==0)continue;
				lsSerialids.add(serialid);
				if(kQGroupDetial.length()>0)kQGroupDetial+="<br>";
				kQGroupDetial += shiftManagementToolKit.getShiftOnOffWorkSections(serialid,language);
			}
		}else if(kqtype.equals("3")){
			kQGroupDetial = SystemEnv.getHtmlLabelName(389120,language);
		}
		return kQGroupDetial;
	}

	public String getSignTime(String signDate, String signTime){
		return signDate+" "+signTime;
	}

	public String getFlowDurationByUnit(String duration, String otherPara){
    String[] splitStr = Util.splitString(otherPara, "+");
    String kqType = "";
    String durationrule = "";
    String lan = "";
    String newLeaveType = "";
    String requestid = "";
    String typeselect = "";
    String fromDate = "";
    String toDate = "";
    String backduraion = "";

    if(splitStr.length == 6){
      kqType = splitStr[0];
      durationrule = splitStr[1];
      lan = splitStr[2];
      newLeaveType = splitStr[3];
      requestid = splitStr[4];
      backduraion = splitStr[5];
    }else if(splitStr.length == 5){
      kqType = splitStr[0];
      durationrule = splitStr[1];
      lan = splitStr[2];
      requestid = splitStr[3];
      backduraion = splitStr[4];
    }else if(splitStr.length == 8){
      kqType = splitStr[0];
      durationrule = splitStr[1];
      lan = splitStr[2];
      newLeaveType = splitStr[3];
      requestid = splitStr[4];
      typeselect = splitStr[5];
      fromDate = splitStr[6];
      toDate = splitStr[7];
    }else if(splitStr.length == 3){
      kqType = splitStr[0];
      durationrule = splitStr[1];
      lan = splitStr[2];
    }
    if(Util.getIntValue(kqType) == 0 && newLeaveType.length() == 0){
      return duration;
    }

    return getUnitByKQType(kqType,newLeaveType,durationrule,Util.getDoubleValue(duration),requestid,lan,backduraion);

	}

  /**
   * 根据考勤流程类型+请假类型id得到单位
   * @param kqType
   * @param newLeaveType
   * @param durationrule
   * @param duration
   * @param requestid
   * @param lan
   * @param backduraion
   * @return
   */
  private String getUnitByKQType(String kqType, String newLeaveType, String durationrule,
      double duration, String requestid, String lan, String backduraion) {
    KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
//    String unit = "";
    double kq_duration = duration;
    double proportion = 0.0;
    Map<String,String> backDuraion = new HashMap<>();
    double backDuration = Util.getDoubleValue(Util.null2s(backduraion,"0.0"),0.0);
    String minimumUnit = "";
    switch (kqType){
      case "0":
        proportion = Util.getDoubleValue(kqLeaveRulesComInfo.getProportion(newLeaveType));
        minimumUnit = ""+KQLeaveRulesBiz.getMinimumUnit(newLeaveType);
//        if(requestid.length() > 0){
//          backDuraion = getLeaveBackDuraion(requestid,typeselect,fromDate,toDate);
//        }
//        unit = getMinimumUnitName(""+minimumUnit, lan);
        break;
      case "1":
        minimumUnit = KQTravelRulesBiz.getMinimumUnit();//单位类型
        proportion = Util.getDoubleValue(KQTravelRulesBiz.getHoursToDay());//换算关系
//        unit = getMinimumUnitName(minimumUnit,lan);
        break;
      case "2":
        minimumUnit = KQExitRulesBiz.getMinimumUnit();//单位类型
        proportion = Util.getDoubleValue(KQExitRulesBiz.getHoursToDay());//换算关系
//        unit = getMinimumUnitName(minimumUnit,lan);
        break;
      case "3":
        minimumUnit = ""+KQOvertimeRulesBiz.getMinimumUnit();//当前加班单位
        proportion = KQOvertimeRulesBiz.getHoursToDay();//当前天跟小时计算关系
//        unit = getMinimumUnitName(minimumUnit,lan);
        break;
      case "4":
        break;
      case "5":
        break;
      case "6":
        break;
      case "7":
        break;
      default:
        break;
    }


    if(KQUnitBiz.isLeaveHour(minimumUnit)){//按小时
      if(!KQUnitBiz.isLeaveHour(durationrule)){
        if(proportion>0) {
          kq_duration = duration*proportion;
          backDuration = backDuration*proportion;
        }
      }
    }else{//按天
      if(KQUnitBiz.isLeaveHour(durationrule)){
        if(proportion>0) {
          kq_duration = duration/proportion;
          backDuration = backDuration/proportion;
        }
      }
    }
//    if(unit.length() > 0){
//      return kq_duration+"("+unit+")";
//    }else{
//    }
    if(backDuration > 0){
      return KQDurationCalculatorUtil.getDurationRound(""+kq_duration)+"("+SystemEnv.getHtmlLabelName(24473,
          Util.getIntValue(lan))+":"+KQDurationCalculatorUtil.getDurationRound(""+backDuration)+")";
    }else{
      return KQDurationCalculatorUtil.getDurationRound(""+kq_duration);
    }
  }

  /**
   * 根据请假流程id获取被销假数据的时长
   * @param requestid
   * @param typeselect
   * @param fromDate
   * @param toDate
   * @return
   */
  public Map<String,String> getLeaveBackDuraion(String requestid, String typeselect,
      String fromDate, String toDate) {
    if(typeselect.length()==0){
      typeselect = "3";
    }
    if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
      if(typeselect.equals("1")){
        fromDate = TimeUtil.getCurrentDateString();
        toDate = TimeUtil.getCurrentDateString();
      }else{
        fromDate = TimeUtil.getDateByOption(typeselect,"0");
        toDate = TimeUtil.getDateByOption(typeselect,"1");
      }
    }
    Map<String,String> backDuraion = new HashMap<>();
    RecordSet rs = new RecordSet();
    String getLeaveBackDuraion = "select sum(cast(duration as decimal(18,4))) as duration1,durationrule from kq_flow_split_leaveback where leavebackrequestid = ? ";
    if (fromDate.length() > 0 && toDate.length() > 0){
      getLeaveBackDuraion += " and ( fromDate between '"+fromDate+"' and '"+toDate+"' or toDate between '"+fromDate+"' and '"+toDate+"' "
          + " or '"+fromDate+"' between fromDate and toDate or '"+toDate+"' between fromDate and toDate) ";
    }
    getLeaveBackDuraion += " group by durationrule ";
    rs.executeQuery(getLeaveBackDuraion, requestid);
    if (rs.next()){
      String duration1 = rs.getString("duration1");
      String durationrule = rs.getString("durationrule");
      backDuraion.put("durationrule", durationrule);
      backDuraion.put("duration", duration1);
    }
    return backDuraion;
  }

  private String getMinimumUnitName(String minimumUnit,int lan){
    String minimumUnitName = "";
    switch (minimumUnit) {
      case "1":
        minimumUnitName = SystemEnv.getHtmlLabelName(1925, lan);
        break;
      case "2":
        minimumUnitName = SystemEnv.getHtmlLabelName(1925, lan);
        break;
      case "3":
        minimumUnitName = SystemEnv.getHtmlLabelName(391, lan);
        break;
      case "4":
        minimumUnitName = SystemEnv.getHtmlLabelName(1925, lan);
        break;
      default:
        break;
    }
    return minimumUnitName;
  }

	public String getSerailName(String serialid,String otherPara) {
		String serailName = "";
		if(Util.null2String(serialid).trim().length()==0) return serailName;
		String workSections = "";
		KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

		KQShiftManagementComInfo kqShiftManagementComInfo = new KQShiftManagementComInfo();
		String[] params = Util.splitString(otherPara, "+");
		String workbegintime = Util.null2String(params[0]).trim();
		String workendtime = Util.null2String(params[1]).trim();

    serailName = kqShiftManagementComInfo.getSerial(serialid);
    if(workbegintime.length()>0&&workendtime.length()>0){
      //只有下班时间可能跨天
      workendtime = kqTimesArrayComInfo.turn48to24Time(workendtime);
      workSections += workbegintime+"-"+workendtime;
		}
		if(workSections.length()>0) {
			serailName += "(" + workSections + ")";
		}
		return serailName;
	}

  /**
   * 上班打卡时间
   * @param serialid
   * @param otherPara
   * @return
   */
  public String getReportDetialSignInTime(String serialid,String otherPara) {
    String signTime = "";
    String[] params = Util.splitString(otherPara, "+");
    String begintime = Util.null2String(params[0]).trim();
    String kqdate = Util.null2String(params[1]).trim();
    String resourceid = Util.null2String(params[2]).trim();
    int language = Util.getIntValue(params[3],7);

    if(begintime.length()>0){
      signTime += begintime;
    }

    if(Util.null2String(serialid).length()>0){
      if(signTime.length()==0){
        signTime = SystemEnv.getHtmlLabelName(25994,language);
      }
    }else{
      //弹性工时打卡时间取自签到签退数据
    }

    return signTime;
  }

  /**
   * 下班打卡时间
   * @param serialid
   * @param otherPara
   * @return
   */
  public String getReportDetialSignOutTime(String serialid,String otherPara) {
    String signTime = "";
    String[] params = Util.splitString(otherPara, "+");
    String endtime = Util.null2String(params[0]).trim();
    String kqdate = Util.null2String(params[1]).trim();
    String resourceid = Util.null2String(params[2]).trim();
    int language = Util.getIntValue(params[3],7);

    if(endtime.length()>0){
      signTime += endtime;
    }

    if(Util.null2String(serialid).length()>0){
      if(signTime.length()==0){
        signTime = SystemEnv.getHtmlLabelName(25994,language);
      }
    }else{
      //弹性工时打卡时间取自签到签退数据
    }

    return signTime;
  }


	public String getReportDetialSignTime(String serialid,String otherPara) {
		String signTime = "";
		String[] params = Util.splitString(otherPara, "+");
		String begintime = Util.null2String(params[0]).trim();
		String endtime = Util.null2String(params[1]).trim();
		String kqdate = Util.null2String(params[2]).trim();
		String resourceid = Util.null2String(params[3]).trim();
		int language = Util.getIntValue(params[4],7);

		if(begintime.length()>0&&endtime.length()>0){
			signTime += begintime+"-"+endtime;
		}else if(begintime.length()>0){
      signTime += begintime;
    }else if(endtime.length()>0){
      signTime += endtime;
    }

		if(Util.null2String(serialid).length()>0){
			if(signTime.length()==0){
				signTime = SystemEnv.getHtmlLabelName(25994,language);
			}
		}else{
			//弹性工时打卡时间取自签到签退数据
		}

		return signTime;
	}

	//迟到时长明细
	public String getReportDetialMinToHour(String value) {
        value = value.trim();
		if(value.length()>0){
            //qc2304359 如果来自迟到和早退，最小单位0.5h,向上取整
            if (isHave){
                new BaseBean().writeLog("==zj==(迟到时长明细value前)" + JSON.toJSONString(value));
                KqReportUtil kqReportUtil = new KqReportUtil();
                value = kqReportUtil.halfHourCal(value,1);
                new BaseBean().writeLog("==zj==(迟到时长明细value后)" + JSON.toJSONString(value));
            }else {
                value =  df.format(Util.getDoubleValue(value)/60);
            }
		}
		return value;
	}

	public String getLeavetype(String newleavetype) {
    KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
    if(newleavetype.length() == 0){
      return "";
    }
        boolean show_leave_type_unit = KQSettingsBiz.showLeaveTypeSet(KQSettingsEnum.LEAVETYPE_UNIT.getMain_key());
    String name = kqLeaveRulesComInfo.getLeaveName(newleavetype);
        if(show_leave_type_unit){
            name += kqLeaveRulesComInfo.getUnitName(newleavetype, 7);
        }
		return name;
	}

    public String getLeavetype(String newleavetype, String otherPara) {
        String[] splitStr = Util.splitString(otherPara, "+");
        String languageId = (splitStr==null || splitStr.length<1) ? "7":splitStr[0];
        KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
        if(newleavetype.length() == 0){
            return "";
        }
        boolean show_leave_type_unit = KQSettingsBiz.showLeaveTypeSet(KQSettingsEnum.LEAVETYPE_UNIT.getMain_key());
        String name = kqLeaveRulesComInfo.getLeaveName(newleavetype);
        if(show_leave_type_unit){
            name += kqLeaveRulesComInfo.getUnitName(newleavetype, Util.getIntValue(languageId));
        }
        return name;
    }

	public String getKQGroupCheckboxByGroup(String otherPara){
		String returnVal = "true";

		RecordSet rs = new RecordSet();
        String[] splitStr = Util.splitString(otherPara, "+");
        String id = Util.null2String(splitStr[0]);
        String subcompanyid = Util.null2String(splitStr[1]);
        String userid = Util.null2String(splitStr[2]);
        int resourceid = Util.getIntValue(userid);
        CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
        int operatelevel = checkSubCompanyRight.ChkComRightByUserRightCompanyId(resourceid, "HrmKQGroup:Add", Util.getIntValue(subcompanyid, -1));
        if (operatelevel <= 1) {
            returnVal = "false";
        }
        String sql = "";
        //有考勤组成员
        if(returnVal.equals("true")) {
             sql = " SELECT count(1) FROM kq_groupmember WHERE (isdelete is null or isdelete <> '1') AND groupid=" + id;
            rs.executeQuery(sql);
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    returnVal = "false";
                }
            }
        }

		//有考勤排班
		if(returnVal.equals("true")){
			sql = " SELECT count(1) FROM kq_shiftschedule where (isdelete is null or isdelete <> '1') AND groupid="+id;
			rs.executeQuery(sql);
			if(rs.next()){
				if(rs.getInt(1)>0){
					returnVal = "false";
				}
			}
		}
		return returnVal;
	}

	public String getKQGroupCheckbox(String id){
		String returnVal = "true";
		String sql = "";
		RecordSet rs = new RecordSet();

		//有考勤组成员
		sql = " SELECT count(1) FROM kq_groupmember WHERE (isdelete is null or isdelete <> '1') AND groupid="+id;
		rs.executeQuery(sql);
		if(rs.next()){
			if(rs.getInt(1)>0){
				returnVal = "false";
			}
		}

		//有考勤排班
		if(returnVal.equals("true")){
			sql = " SELECT count(1) FROM kq_shiftschedule where (isdelete is null or isdelete <> '1') AND groupid="+id;
			rs.executeQuery(sql);
			if(rs.next()){
				if(rs.getInt(1)>0){
					returnVal = "false";
				}
			}
		}
		return returnVal;
	}

	public String getKQGroupMembersCheckbox(String params){
  	String returnVal = "true";
  	String[] arrParams = Util.splitString(params,"+");
  	String id = arrParams[0];
  	String groupId = arrParams[1];
		String sql = "";
		RecordSet rs = new RecordSet();
		sql = " SELECT count(1) FROM (\n" +
					" SELECT DISTINCT t.id, t.resourceid, t.groupid, t.status, t.dsporder,t.lastname,t.subcompanyid1, t.departmentid, t.loginid FROM (  \n" +
					" SELECT b.id,a.id AS resourceid, b.groupid, a.status,a.dsporder,a.lastname,a.subcompanyid1, a.departmentid, a.loginid FROM HrmResource a, kq_groupmember b  \n" +
					" WHERE a.id=b.typevalue and b.type =1 and (b.isdelete is null or  b.isdelete <> '1')  \n" +
					" UNION ALL  \n" +
					" SELECT b.id,a.id AS resourceid, b.groupid, a.status,a.dsporder,a.lastname,a.subcompanyid1, a.departmentid, a.loginid FROM HrmResource a, kq_groupmember b  \n" +
					" WHERE a.subcompanyid1 = b.typevalue AND a.seclevel>=b.seclevel AND a.seclevel<=b.seclevelto AND b.type=2 and (b.isdelete is null or  b.isdelete <> '1')  \n" +
					" UNION ALL  \n" +
					" SELECT b.id,a.id AS resourceid, b.groupid, a.status,a.dsporder,a.lastname,a.subcompanyid1, a.departmentid, a.loginid FROM HrmResource a, kq_groupmember b  \n" +
					" WHERE a.departmentid = b.typevalue AND a.seclevel>=b.seclevel AND a.seclevel<=b.seclevelto AND b.type=3 and (b.isdelete is null or  b.isdelete <> '1')  \n" +
					" UNION ALL  \n" +
					" SELECT b.id,a.id AS resourceid, b.groupid, a.status,a.dsporder,a.lastname,a.subcompanyid1, a.departmentid, a.loginid FROM HrmResource a, kq_groupmember b  \n" +
					" WHERE  (a.jobtitle = b.typevalue AND b.type=5 and (b.isdelete is null or  b.isdelete <> '1') AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND a.subcompanyid1 IN(b.jobtitlelevelvalue)) OR (b.jobtitlelevel=3 AND a.departmentid IN(b.jobtitlelevelvalue))))) t \n" +
					" UNION ALL  \n" +
					" SELECT b.id,a.id AS resourceid, b.groupid, a.status,a.dsporder,a.lastname,a.subcompanyid1, a.departmentid, a.loginid FROM HrmResource a, kq_groupmember b \n" +
					" WHERE b.type=6 AND a.seclevel>=b.seclevel AND a.seclevel<=b.seclevelto and (b.isdelete is null or b.isdelete <> '1')) t, kq_shiftschedule a\n" +
					" where t.resourceid=a.resourceid AND t.groupid=a.groupid AND (a.isdelete is null or  a.isdelete <> '1') AND  t.id="+id + " and t.groupId="+groupId;
		rs.executeQuery(sql);
		if(rs.next()){
			if(rs.getInt(1)>0){
				returnVal = "false";
			}
		}
		return returnVal;
	}

  public String getFlowTimeByUnit(String time, String otherPara){
    String compareTime = time;
    String timename = time;
    String[] splitStr = Util.splitString(otherPara, "+");
    String kqtype = "";
    String timetype = "";//0表示开始时间，1表示结束时间
    String lan = "";
    String newLeaveType = "";
    String durationrule = "";
    String timeselection = "1";
    String selectiontype = "";
    String changeType = "";
    KQTimeSelectionComInfo kqTimeSelectionComInfo = new KQTimeSelectionComInfo();
    KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
    Map<String,String> half_map = new HashMap<>();
    if(splitStr.length == 5){
      //归档
      kqtype = splitStr[0];
      timetype = splitStr[1];
      lan = splitStr[2];
      newLeaveType = splitStr[3];
      durationrule = splitStr[4];
    }else if(splitStr.length == 4){
      kqtype = splitStr[0];
      timetype = splitStr[1];
      lan = splitStr[2];
      newLeaveType = splitStr[3];
    }
    switch (kqtype){
      case "0":
        durationrule = (durationrule.length() > 0 ? durationrule : ""+KQLeaveRulesBiz.getMinimumUnit(newLeaveType));
        timeselection = kqLeaveRulesComInfo.getTimeSelection(newLeaveType);
        selectiontype = ""+KqSplitFlowTypeEnum.LEAVE.getFlowtype();
        changeType = kqLeaveRulesComInfo.getMinimumUnit(newLeaveType);
        break;
      case "1":
        durationrule = (durationrule.length() > 0 ? durationrule : ""+KQTravelRulesBiz.getMinimumUnit());
        timeselection = KQTravelRulesBiz.getTimeselection();
        selectiontype = ""+KqSplitFlowTypeEnum.EVECTION.getFlowtype();
        changeType = durationrule;
        newLeaveType = "0";
        break;
      case "2":
        durationrule = (durationrule.length() > 0 ? durationrule : ""+KQExitRulesBiz.getMinimumUnit());
        timeselection = KQExitRulesBiz.getTimeselection();
        selectiontype = ""+KqSplitFlowTypeEnum.OUT.getFlowtype();
        changeType = durationrule;
        newLeaveType = "0";
        break;
      case "3":
        durationrule = (durationrule.length() > 0 ? durationrule : ""+KQOvertimeRulesBiz.getMinimumUnit());
        timeselection = KQOvertimeRulesBiz.getTimeselection();
        selectiontype = ""+KqSplitFlowTypeEnum.OVERTIME.getFlowtype();
        changeType = durationrule;
        newLeaveType = "0";
        if(compareTime.length() > 5){
          compareTime = compareTime.substring(0,5);
        }
        break;
      case "4":
        break;
      case "5":
        break;
      case "6":
        break;
      case "7":
        break;
      default:
        break;
    }
    if("2".equalsIgnoreCase(durationrule) || "4".equalsIgnoreCase(durationrule)){
      if("2".equalsIgnoreCase(durationrule)){
        half_map = kqTimeSelectionComInfo.getTimeselections(selectiontype,newLeaveType,changeType);
        if("1".equalsIgnoreCase(timeselection)){
          //下拉框显示
          String cus_am = "";
          String cus_pm = "";
          if(half_map != null && !half_map.isEmpty()){
            cus_am = Util.null2String(half_map.get("half_on"));
            cus_pm = Util.null2String(half_map.get("half_off"));
          }
          if("0".equalsIgnoreCase(timetype)){
            if(compareTime.equalsIgnoreCase(SplitSelectSet.forenoon_start)){
              timename = SystemEnv.getHtmlLabelName(16689,Util.getIntValue(lan));
              if(cus_am.length() > 0){
                timename = cus_am;
              }
            }else if(compareTime.equalsIgnoreCase(SplitSelectSet.forenoon_end)){
              timename = SystemEnv.getHtmlLabelName(16690,Util.getIntValue(lan));
              if(cus_pm.length() > 0){
                timename = cus_pm;
              }
            }
          }else if("1".equalsIgnoreCase(timetype)){
            if(compareTime.equalsIgnoreCase(SplitSelectSet.afternoon_start)){
              timename = SystemEnv.getHtmlLabelName(16689,Util.getIntValue(lan));
              if(cus_am.length() > 0){
                timename = cus_am;
              }
            }else if(compareTime.equalsIgnoreCase(SplitSelectSet.afternoon_end)){
              timename = SystemEnv.getHtmlLabelName(16690,Util.getIntValue(lan));
              if(cus_pm.length() > 0){
                timename = cus_pm;
              }
            }
          }
        }else{

        }
      }
      if("4".equalsIgnoreCase(durationrule)){
        if("0".equalsIgnoreCase(timetype)){
          if(compareTime.equalsIgnoreCase(SplitSelectSet.forenoon_start)){
            timename = SystemEnv.getHtmlLabelName(390728,Util.getIntValue(lan));
          }
        }else if("1".equalsIgnoreCase(timetype)){
          if(compareTime.equalsIgnoreCase(SplitSelectSet.afternoon_end)){
            timename = SystemEnv.getHtmlLabelName(390728,Util.getIntValue(lan));
          }
        }

      }
    }
    return timename;
  }

  /**
   * 考勤报表，加班明细里根据当前加班单位显示加班时长
   * @param duration
   * @return
   */
  public String getDuration_minByUnit(String duration){

    int uintType = KQOvertimeRulesBiz.getMinimumUnit();//当前加班单位
    double hoursToDay = KQOvertimeRulesBiz.getHoursToDay();//当前天跟小时计算关系

    String valueField = "";
    if(uintType==3 || uintType== 5 || uintType== 6){//按小时计算
      valueField = KQDurationCalculatorUtil.getDurationRound(""+(Util.getDoubleValue(duration)/(60)));
    }else{//按天计算
      valueField = KQDurationCalculatorUtil.getDurationRound(""+(Util.getDoubleValue(duration)/(60*hoursToDay)));
    }

    return valueField;
  }

  /**
   * 考勤报表，加班明细里根据当前加班数据是否关联调休
   * @param paidLeaveEnable
   * @param otherPram
   * @return
   */
  public String getPaidLeaveEnable(String paidLeaveEnable,String otherPram){
    int lan = Util.getIntValue(otherPram,7);
    if("1".equalsIgnoreCase(paidLeaveEnable)){
      return SystemEnv.getHtmlLabelName(163, lan);
    }else{
      return SystemEnv.getHtmlLabelName(161, lan);
    }
  }

  /**
   * 考勤报表，加班明细里根据当前加班数据来源
   * @param computingMode
   * @param otherPram
   * @return
   */
  public String getComputingMode(String computingMode,String otherPram){
//    String mode = "";
//    if("1".equalsIgnoreCase(computingMode)){
//      mode = "以加班流程为准";
//    }else if("2".equalsIgnoreCase(computingMode)){
//      mode = "以打卡为准，但不能超过加班流程时长";
//    }else if("3".equalsIgnoreCase(computingMode)){
//      mode = "根据打卡时间计算加班时长";
//    }
    int lan = Util.getIntValue(otherPram,7);
    if("1".equalsIgnoreCase(computingMode)){
      return SystemEnv.getHtmlLabelName(30045, lan);
    }else{
      return SystemEnv.getHtmlLabelName(500502, lan);
    }
  }

  public String getSchedulecode(String params){
    String schedulecode = "";
    String sql = "";
    RecordSet rs = new RecordSet();
    try{
      boolean isOneDevice = true;
      sql = " select count(1) from kq_schedule_device ";
      rs.executeQuery(sql);
      if(rs.next()){
        if(rs.getInt(1)>1){
          isOneDevice = false;
        }
      }

      if(isOneDevice){
        sql = " SELECT schedulecode FROM kq_schedule_code where resourceid="+params;
        rs.executeQuery(sql);
        while (rs.next()){
          if(schedulecode.length()>0)schedulecode+=",";
          schedulecode += Util.null2String(rs.getString("schedulecode"));
        }
      }else{
        sql = " SELECT count(1) as cnt FROM kq_schedule_code where resourceid="+params;
        rs.executeQuery(sql);
        if(rs.next()){
          schedulecode = ""+rs.getInt("cnt");
        }
      }
    }catch (Exception e){
      writeLog(e);
    }
    return schedulecode;
  }
  public String getScheduleDeviceCheckbox(String id){
    String returnVal = "true";

    String sql = "";
    RecordSet rs = new RecordSet();
    try{
      sql = " select count(1) from kq_schedule_code where deviceid = ? ";
      rs.executeQuery(sql,id);
      if(rs.next()){
        if(rs.getInt(1)>0){
          returnVal = "false";
        }
      }
    }catch (Exception e){
      writeLog(e);
    }
    return returnVal;
  }

  public ArrayList<String> getScheduleDeviceOperate(String id){
    ArrayList<String> resultList = new ArrayList<String>();

    resultList.add("true");
    resultList.add(this.getScheduleDeviceCheckbox(id));
    resultList.add("true");
    return resultList;
  }

  public String getWorkflowname(String field001){
    WorkflowComInfo workflowComInfo = new WorkflowComInfo();
    return workflowComInfo.getWorkflowname(field001);
  }

  public String getFlowTypeName(String field006, String strLanguage){
    if("0".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(83393, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("1".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(83394, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("2".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(83395, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("3".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(83396, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("5".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(390737, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("6".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(389117, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("7".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(390274, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else if("8".equalsIgnoreCase(field006)){
      return SystemEnv.getHtmlLabelName(513400, Util.getIntValue(Util.getIntValue(strLanguage)));
    }else {
      return "";
    }
  }

  public String getRequestLink(String fromDate,String req_requestid){
    WorkflowRequestComInfo workflowRequestComInfo = new WorkflowRequestComInfo();
    if(req_requestid.length() > 0 && Util.getIntValue(req_requestid) > 0){
      return "<a href='"+weaver.general.GCONST.getContextPath()+"/spa/workflow/index_form.jsp#/main/workflow/req?ismonitor=1&requestid=" + req_requestid + "'target='_blank'>" + fromDate + "</a>";
    }else{
      return fromDate;
    }
  }

  public String getOvertimeCard(String fromdate,String otherPara){
    String[] splitStr = Util.splitString(otherPara, "+");
    if(splitStr.length == 3){
      String fromtime = splitStr[0];
      String todate = splitStr[1];
      String totime = splitStr[2];
      //多时区会把这个文字给转换改成/
      String tmpfromdate = fromdate.replaceAll("-", "/");
      String tmptodate = todate.replaceAll("-", "/");
      String datetime = tmpfromdate+" "+fromtime+"-"+tmptodate+" "+totime;
      return datetime;
    }
    return "";
  }
}
