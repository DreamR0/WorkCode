/*
 *
 * Copyright (c) 2001-2016 泛微软件.
 * 泛微协同商务系统,版权所有.
 * 
 */
package weaver.meeting.Maint;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.api.workplan.util.TimeZoneCastUtil;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Meeting;
import com.engine.common.constant.BizLogType;
import com.engine.common.constant.ParamConstant;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.meeting.service.MeetingBaseService;
import com.engine.meeting.service.MeetingSignService;
import com.engine.meeting.service.impl.MeetingBaseServiceImpl;
import com.engine.meeting.service.impl.MeetingSignServiceImpl;
import com.engine.meeting.util.MeetingEncryptUtil;
import com.engine.meeting.util.MeetingSeatUtil;
import com.engine.workplan.service.WorkPlanBaseService;
import com.engine.workplan.service.impl.WorkPlanBaseServiceImpl;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.StringUtils;

import weaver.Constants;
import weaver.WorkPlan.exchange.EWSToWorkPlan;
import weaver.WorkPlan.exchange.WorkPlanExchangeFactory;
import weaver.WorkPlan.exchange.WorkplanEwsService;
import weaver.conn.RecordSet;
import weaver.dateformat.UnifiedConversionInterface;
import weaver.encrypt.EncryptUtil;
import weaver.general.BaseBean;
import weaver.general.ThreadPoolUtil;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.report.schedulediff.HrmScheduleDiffUtil;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.MeetingShareUtil;
import weaver.meeting.MeetingViewer;
import weaver.meeting.defined.MeetingCreateWFUtil;
import weaver.meeting.defined.MeetingFieldManager;
import weaver.meeting.remind.MeetingRemindUtil;
import weaver.meeting.MeetingUtil;
import weaver.meeting.util.exchange.MeetingEwsService;
import weaver.meeting.util.exchange.MeetingExchangeFactory;
import weaver.meeting.util.exchange.MeetingExchangeUtil;
import weaver.mobile.plugin.ecology.service.PushNotificationService;
import weaver.systeminfo.SystemEnv;

/**
 * 周期会议生成普通会议
 * 复制会议等
 * @author HuangGuanGuan
 * Jan 15, 2015
 *
 */
public class MeetingInterval{

	/**
	 * 获取重复预订会议时间数组
	 * @param begindate
	 * @param enddate
	 * @param type
	 * @param otherinfo
	 * @return
	 */
	private static ArrayList getBeginDate(String begindate,String enddate,String type,int intervaltime,String otherinfo){
		ArrayList begindatelist = new ArrayList();
		//开始时间与当前时间比较,忽略历史数据
		String now=TimeUtil.getCurrentDateString();
		if(now.compareTo(begindate)>0){
			begindate=now;
		}
		if(now.compareTo(enddate)>0){
			return begindatelist;
		}

		if("1".equals(type) && intervaltime > 0) { //天重复
			begindatelist.add(begindate);
			while(begindate.compareTo(enddate) <= 0) {				
				begindate = TimeUtil.dateAdd(begindate,intervaltime);
				if(begindate.compareTo(enddate) <= 0) {
					begindatelist.add(begindate);
				}
			}
		}
		else if("2".equals(type) && intervaltime > 0) { //周重复
			otherinfo=otherinfo.replaceAll("7", "0");//转换数据库保存的星期天为7 计算为0
			String weekdate = "";
			if(!"".equals(otherinfo)){
				weekdate = getFirstDayOfWeek(begindate);
				for(int i=0; i<7; i++) {
					String weekcount = String.valueOf(TimeUtil.dateWeekday(weekdate));
	                if(otherinfo.indexOf(weekcount) >= 0) {
	                	if(weekdate.compareTo(begindate)>=0&&weekdate.compareTo(enddate) <= 0){
		        			begindatelist.add(weekdate);
	                	}
	                }
	                weekdate = TimeUtil.dateAdd(weekdate,1);
				}
				
				while(begindate.compareTo(enddate) <= 0) {
			          begindate = TimeUtil.dateAdd(begindate,intervaltime*7);
			          weekdate = getFirstDayOfWeek(begindate);
					  
			          for(int i=0; i<7; i++) {
			        	  String weekcount = String.valueOf(TimeUtil.dateWeekday(weekdate));
			        	  if(!"".equals(otherinfo)){
				              if(otherinfo.indexOf(weekcount) >= 0) {
								  if(weekdate.compareTo(enddate) <= 0){
				        			begindatelist.add(weekdate);
								  }else{
									  break;
								  }
				              }
			        	  }
			              weekdate = TimeUtil.dateAdd(weekdate,1);
			          }
			   }
      	   }
		 }
		else if("3".equals(type) && intervaltime > 0) { //月重复
			 int year = Integer.parseInt(begindate.substring(0, 4));
			 int month = Integer.parseInt(begindate.substring(5, 7));
			 String datestr = "";
			 if(!"".equals(otherinfo)) {
				 if(Integer.parseInt(otherinfo) < 10) {
					 datestr = "0"+otherinfo;
				 } else {
					 datestr = otherinfo;
				 }			 
			 }
			 String firstDate = String.valueOf(year)+"-"+(month < 10?("0"+String.valueOf(month)):String.valueOf(month))+"-"+datestr;
			 if(begindate.compareTo(firstDate) <= 0){
				 begindatelist.add(firstDate);
			 }
			 while((begindate.substring(0, 7)).compareTo(enddate.substring(0, 7)) <= 0){
				 //if(month == 12) {
				//	year = year+1;
				//	month = 1;
				// } else {
					month = month + intervaltime;
				// }
				 if(month > 12) {
					year = year+month/12;
					month = month%12;
				 }
				 String monthstr = "";
				 if(month < 10){
					monthstr = "0"+String.valueOf(month);
				 } else {
					monthstr = String.valueOf(month);
				 }
				 String monthEndDate = "";
				 try {
					monthEndDate = TimeUtil.getYearMonthEndDay(year,month);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 
				 begindate = String.valueOf(year)+"-"+monthstr+"-"+datestr;
				 if("".equals(monthEndDate)) {
					 monthEndDate = begindate;
				 }
				 if((begindate.substring(0, 7)).compareTo(enddate.substring(0, 7)) <= 0) {
					 if(monthEndDate.compareTo(begindate)>-1) {
						 begindatelist.add(begindate);
					 }
					
				 }
		     }
		 }
		 //System.out.println(begindatelist);
		 return begindatelist;
	}
 
	 /**
     * 返回特定日期所处这一周的周一所处的日期
     * @param date 日期
     * @return String
     */
	private static String getFirstDayOfWeek(String date) {
        Calendar calendar = TimeUtil.getCalendar(date);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date dateBegin = new Date(0);
        dateBegin.setTime(calendar.getTimeInMillis() - (long) (TimeUtil.dateWeekday(date) - 1) * 24 * 60 * 60 * 1000);
        return formatter.format(dateBegin);
    }
    /**
     * 删除未生成的周期会议
     * @param meetingid
     */
    public static void deleteMeetingRepeat(String meetingid){
    	RecordSet rs = new RecordSet();
    	rs.executeSql("delete from Meeting_repeat where meetingid=" + meetingid);
    }

    
    /**
     * 获取某天的第n个工作日后的日期
     * @param CurrentDate
     * @param days
     * @param creater
     * @return
     */
    private static String getWorkDayByDays(String CurrentDate, int days, String creater){
    	String edate = CurrentDate;
    	String tmpdate = edate;
		HrmScheduleDiffUtil hrmScheduleDiffUtil=new HrmScheduleDiffUtil();
		hrmScheduleDiffUtil.setUser(new User(Util.getIntValue(creater)));
    	for(int i = 0,j=0; i < days; i++,j++){
    		tmpdate = TimeUtil.dateAdd(tmpdate, 1);
    		//System.out.println(tmpdate+"--"+hsdu.getIsWorkday(tmpdate,1,""));
    		if(hrmScheduleDiffUtil.getIsWorkday(tmpdate)){
    			edate = tmpdate;
    		} else {
    			i--;
    		}
    		if(j >= 100){
    			//超过100天的推迟，则认为没有设置工作日,日期不变。
    			edate = CurrentDate;
    			break;
    		}
    	}
    	return edate;
    }

    
    /**
     * 获取某天的第n个工作日后的日期
     * @param CurrentDate
     * @param days
     * @return
     */
    private static String getDayByDays(String CurrentDate, int days){
    	String edate = CurrentDate;
    	String tmpdate = edate;
    	for(int i = 0; i < days; i++){
    		tmpdate = TimeUtil.dateAdd(tmpdate, 1);
    		edate = tmpdate;
    	}
    	return edate;
    }
    
    /**
     * 添加重复会议-更新记录
     * @param days
     * @param meetingid
     * @param begindate
     * @param enddate
     * @param type
     * @param intervaltime
     * @param otherinfo
     */
    public static void updateMeetingRepeat(int days, String meetingid, String begindate,String enddate,String type,int intervaltime,String otherinfo){
    	updateMeetingRepeat(days,meetingid,begindate,enddate,type,intervaltime,otherinfo,0);
    }
    
    /**
     * 添加重复会议-更新记录
     * @param days
     * @param meetingid
     * @param begindate
     * @param enddate
     * @param type
     * @param intervaltime
     * @param otherinfo
     * @param repeatStrategy 重复会议策略
     */
    public static void updateMeetingRepeat(int days, final String meetingid, String begindate,String enddate,String type,int intervaltime,String otherinfo, int repeatStrategy){
    	final ArrayList begindatelist = getBeginDate(begindate, enddate, type, intervaltime, otherinfo);
    	final RecordSet rs = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
		Date newdate = new Date() ;
    	long datetime = newdate.getTime() ;
		Timestamp timestamp = new Timestamp(datetime) ;
		String CurrentDate = (timestamp.toString()).substring(0,4) + "-" + (timestamp.toString()).substring(5,7) + "-" +(timestamp.toString()).substring(8,10);
		//提前days生成会议
		final String edate = getDayByDays(CurrentDate, days);
		boolean firstMtOver = false;
		//使用后台线程生成周期会议
		new Thread(){
    		public void run() {
				for(int d=0; d<begindatelist.size(); d++) {
					final String date = (String)begindatelist.get(d);
					if(date.compareTo(edate) <= 0 ){
						
						try {
					    		cloneMeeting(meetingid, date);
						} catch (Exception e) {
							rs.writeLog("生成会议失败,meetingid:["+meetingid+"]date:+["+date+"]");
							rs.writeLog(e);
						}
				    		
					} else {
						rs.executeSql("insert into Meeting_repeat(meetingid,begindate) values("+meetingid+",'"+date+"') ");
					}
				}
				try {
					new MeetingComInfo().removeMeetingInfoCache();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}.start();
    	

    }
    
    /**
     * 结束重复会议
     * @param meetingid 重复模板会议
     */
    public void stopIntervalMeeting(String meetingid){
    	stopIntervalMeeting(meetingid, null);
    }
    
    /**
     * 提前结束重复会议
     * @param meetingid 重复模板会议
     * @param enddate 结束日期
     */
    public void stopIntervalMeeting(String meetingid, String enddate){
    	Date newdate = new Date() ;
    	long datetime = newdate.getTime() ;
		Timestamp timestamp = new Timestamp(datetime) ;
		String CurrentDate = (timestamp.toString()).substring(0,4) + "-" + (timestamp.toString()).substring(5,7) + "-" +(timestamp.toString()).substring(8,10);
		String CurrentTime = (timestamp.toString()).substring(11,13) + ":" + (timestamp.toString()).substring(14,16) + ":" +(timestamp.toString()).substring(17,19);
    	if(meetingid == null || "".equals(meetingid)){
    		return;
    	}
    	RecordSet rs = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
    	String stopdate = CurrentDate;
    	
    	if(enddate == null || "".equals(enddate)){
    		rs.executeSql("update Meeting set meetingstatus = 5, repeatenddate = '"+stopdate+"' where  id = "+ meetingid);
    	}else{
    		stopdate = enddate;
    		rs.executeSql("update Meeting set repeatenddate = '"+enddate+"' where  id = "+ meetingid);
    	}
    	rs.executeSql("delete from Meeting_repeat where begindate > '" + stopdate+"' and meetingid = "+ meetingid);
    	
    	rs.executeSql("update Meeting set meetingstatus = 4,cancel='1',canceldate='"+CurrentDate+"',canceltime='"+CurrentTime+"' where repeatmeetingid="+meetingid+" and begindate>'"+stopdate+"'");
    	
    	rs.execute("select * from workplan where meetingId in (select id from Meeting where repeatmeetingid="+meetingid+" and begindate>'"+stopdate+"')");
		weaver.WorkPlan.WorkPlanHandler wph = new weaver.WorkPlan.WorkPlanHandler();
		while(rs.next()){
			wph.delete(rs.getString("id")); //删除未开始会议的提醒流程
		}
    }
    
    /**
     * 批量生成重复会议
     * @param
     * @throws Exception
     */
    public static void batchCloneMeeting() throws Exception{
    	MeetingSetInfo meetingSetInfo = new MeetingSetInfo();
    	Date newdate = new Date() ;
    	long datetime = newdate.getTime() ;
		Timestamp timestamp = new Timestamp(datetime) ;
		String CurrentDate = (timestamp.toString()).substring(0,4) + "-" + (timestamp.toString()).substring(5,7) + "-" +(timestamp.toString()).substring(8,10);
		//提前days生成会议
		String enddate = getDayByDays(CurrentDate, meetingSetInfo.getDays());
		RecordSet rs = new RecordSet();
		RecordSet recordSet = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs, recordSet);
		rs.executeSql("select id, meetingid, begindate from Meeting_repeat where begindate <= '" + enddate+"' order by id asc");
		while(rs.next()){
			String meetingid = Util.null2String(rs.getString("meetingid"));
			String begindate = Util.null2String(rs.getString("begindate"));
			String id = Util.null2String(rs.getString("id"));
			//获取召集人/创建人
			recordSet.executeQuery("select caller,creater from meeting where id = ?",meetingid);
			boolean canGoOn = true;
			if(recordSet.next()){
				String caller = recordSet.getString(1);
				String creater = recordSet.getString(2);
				try{
					ResourceComInfo rci = new ResourceComInfo();
					if(Util.getIntValue(rci.getStatus(caller))>3 || Util.getIntValue(rci.getStatus(creater))>3){
						canGoOn = false;
					}
				}catch (Exception e){
					e.printStackTrace();
				}
			}
			if(begindate.compareTo(CurrentDate) >= 0 && canGoOn){
				cloneMeeting(meetingid, begindate);
			}
			recordSet.executeSql("delete from Meeting_repeat where id= "+ id);
		}
		new MeetingComInfo().removeMeetingInfoCache();
    }
    
    
    /**
     * 生成会议日程和会议提醒
     * @param meetingid
     * @param date
     * @throws Exception
     */
    public static void  creatWpAndSwfForMeeting(String meetingid, String date) throws Exception{
    	//生成会议日程和会议提醒
		createWPAndRemind(meetingid,date,"");
    }
    
    /**
     * 复制新增会议
     * @param meetingid meeting
     * @return
     * @throws Exception
     */
    public  static String  copyMeetingfromMeeting(String meetingid, User user) throws Exception{
		RecordSet rs=new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
		rs.executeProc("Meeting_SelectByID",meetingid);
		if(rs.next()){
			String creater=rs.getString("creater");
			int meetingstatus = Util.getIntValue(rs.getString("meetingstatus"));
			int repeatType = Util.getIntValue(rs.getString("repeatType"));
			String allUser= MeetingShareUtil.getAllUser(user);
			boolean canCopy = false;
			if(meetingstatus==2 || (meetingstatus!=0) && (meetingstatus!=1) && (meetingstatus!=3)){
				if(MeetingShareUtil.containUser(allUser,creater)){
					if(repeatType >0 && meetingstatus != 5){
						canCopy = true;
					}else if(repeatType <= 0){
						canCopy = true;
					}

				}
			}
			if(canCopy){
				return ""+copyMeeting(rs,true,null,user);
			}else{
				rs.writeLog("userid : ["+user.getUID()+"] 会议id：["+meetingid+"]has no right for copy。");
				return "-1";
			}
		}else{
			rs.writeLog("会议id：["+meetingid+"]不存在，复制生成会议失败。");
			return "-1";
		}

    }

	public  static String  copyMeetingfromMeeting(String meetingid, User user,String ip) throws Exception{
		RecordSet rs=new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
		rs.executeProc("Meeting_SelectByID",meetingid);
		if(rs.next()){
			String creater=rs.getString("creater");
			int meetingstatus = Util.getIntValue(rs.getString("meetingstatus"));
			int repeatType = Util.getIntValue(rs.getString("repeatType"));
			String allUser= MeetingShareUtil.getAllUser(user);
			boolean canCopy = false;
			if(meetingstatus==2 || (meetingstatus!=0) && (meetingstatus!=1) && (meetingstatus!=3)){
				if(MeetingShareUtil.containUser(allUser,creater)){
					if(repeatType >0 && meetingstatus != 5){
						canCopy = true;
					}else if(repeatType <= 0){
						canCopy = true;
					}

				}
			}
			if(canCopy){
				return ""+copyMeeting(rs,true,null,user,ip);
			}else{
				rs.writeLog("userid : ["+user.getUID()+"] 会议id：["+meetingid+"]has no right for copy。");
				return "-1";
			}
		}else{
			rs.writeLog("会议id：["+meetingid+"]不存在，复制生成会议失败。");
			return "-1";
		}

	}

	private static int copyMeeting(RecordSet rs,boolean isCopy,String date,User user,String ip) throws Exception{

		BizLogContext bizLogContext=new BizLogContext();

		MeetingViewer meetingViewer = new MeetingViewer();
		MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
		MeetingSetInfo meetingSetInfo=new MeetingSetInfo();
		MeetingUtil meetingUtil = new MeetingUtil();
		Timer timer = new Timer();
		RecordSet recordSet=new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs,recordSet);
		Date newdate = new Date() ;
		long datetime = newdate.getTime() ;
		Timestamp timestamp = new Timestamp(datetime) ;
		String CurrentDate = (timestamp.toString()).substring(0,4) + "-" + (timestamp.toString()).substring(5,7) + "-" +(timestamp.toString()).substring(8,10);
		String CurrentTime = (timestamp.toString()).substring(11,13) + ":" + (timestamp.toString()).substring(14,16) + ":" +(timestamp.toString()).substring(17,19);
		char flag = 2;
		String ProcPara = "";
		//基本信息
		String meetingid=rs.getString("id");
		String meetingtype = Util.null2String(rs.getString("meetingtype"));
		String name=Util.null2String(rs.getString("name"));//会议名称
		String caller=Util.null2String(rs.getString("caller"));//召集人,必填
		String contacter=Util.null2String(rs.getString("contacter"));//联系人,空值使用当前操作人
		String creater=Util.null2String(rs.getString("creater"));
		String secretDeadline=Util.null2String(rs.getString("secretDeadline"));
		String secretLevel = rs.getString("secretLevel");
		if(secretLevel.equals("")){
			secretLevel = MeetingUtil.DEFAULT_SECRET_LEVEL;
		}
		//会议室
		int roomType = rs.getInt("roomType");
		String address=Util.null2String(rs.getString("address"));//会议地点
		String customizeAddress = Util.null2String(rs.getString("customizeAddress"));
		String desc = Util.htmlFilter4UTF8(Util.null2String(rs.getString("desc_n")));//描述,可为空
		//时间
		int repeatType = Util.getIntValue(rs.getString("repeatType"),0);//是否是重复会议,0 正常会议.
		String begindate=Util.null2String(rs.getString("begindate"));//开始日期
		String enddate=Util.null2String(rs.getString("enddate"));//结束日期
		String repeatbegindate=Util.null2String(rs.getString("repeatbegindate"));//重复开始日期
		String repeatenddate=Util.null2String(rs.getString("repeatenddate"));//重复结束日期
		String begintime=Util.null2String(rs.getString("begintime"));//开始时间
		String endtime=Util.null2String(rs.getString("endtime"));//结束时间
		//重复策略字段
		int repeatdays = Util.getIntValue(rs.getString("repeatdays"),0);
		int repeatweeks = Util.getIntValue(rs.getString("repeatweeks"),0);
		String rptWeekDays=Util.null2String(rs.getString("rptWeekDays"));
		int repeatmonths = Util.getIntValue(rs.getString("repeatmonths"),0);
		int repeatmonthdays = Util.getIntValue(rs.getString("repeatmonthdays"),0);
		int repeatStrategy = Util.getIntValue(rs.getString("repeatStrategy"),0);

		//提醒方式和时间
		int remindType=1;//老的提醒方式
		String remindTypeNew=Util.null2String(rs.getString("remindTypeNew"));//新的提示方式
		int remindImmediately = Util.getIntValue(rs.getString("remindImmediately"),0);  //是否立即提醒
		int remindBeforeStart = Util.getIntValue(rs.getString("remindBeforeStart"),0);  //是否开始前提醒
		int remindBeforeEnd = Util.getIntValue(rs.getString("remindBeforeEnd"),0);  //是否结束前提醒
		int remindHoursBeforeStart = Util.getIntValue(rs.getString("remindHoursBeforeStart"),0);//开始前提醒小时
		int remindTimesBeforeStart = Util.getIntValue(Util.null2String(rs.getString("remindTimesBeforeStart")),0);  //开始前提醒时间
		int remindHoursBeforeEnd = Util.getIntValue(rs.getString("remindHoursBeforeEnd"),0);//结束前提醒小时
		int remindTimesBeforeEnd = Util.getIntValue(Util.null2String(rs.getString("remindTimesBeforeEnd")),0);  //结束前提醒时间
		//参会人员
		String hrmmembers=Util.null2String(rs.getString("hrmmembers"));//参会人员
		String hrmDepartments = Util.null2String(rs.getString("hrmDepartments"));//参会部门
		String hrmSubCompanys = Util.null2String(rs.getString("hrmSubCompanys"));//参会分部
		int totalmember=Util.getIntValue(rs.getString("totalmember"),0);//参会人数
		String othermembers=Util.null2String(rs.getString("othermembers"));//其他参会人员
		String crmmembers=Util.null2String(rs.getString("crmmembers"));//参会客户
		int crmtotalmember=Util.getIntValue(rs.getString("crmtotalmember"),0);//参会人数
		//其他信息
		String projectid=Util.null2String(rs.getString("projectid"));	//加入了项目id
		String accessorys = Util.null2String(rs.getString("accessorys"));//会议附件
		String addressdesc=rs.getString("addressdesc");
		int meetingstatus=0;

		int allowSignBack = 1;//启用会议签退
		int afterSignCanBack = 0;//会议签到后？分钟才能签退
		int defaultAllowSignTime = 5;//会议签到后？分钟才能签退
		int defaultAllowSignBackTime = 30;

		defaultAllowSignTime = Util.getIntValue(rs.getString("defaultAllowSignTime"), 0);
		allowSignBack = Util.getIntValue(rs.getString("allowSignBack"));
		afterSignCanBack = Util.getIntValue(rs.getString("afterSignCanBack"));
		defaultAllowSignBackTime = Util.getIntValue(rs.getString("defaultAllowSignBackTime"));

		Map signMap = new HashMap();
		signMap.put("defaultAllowSignTime",defaultAllowSignTime);
		signMap.put("allowSignBack",allowSignBack);
		signMap.put("afterSignCanBack",afterSignCanBack);
		signMap.put("defaultAllowSignBackTime",defaultAllowSignBackTime);

		//预防MySql更新的时候int型不能直接插入'',给一个默认值0
		String repeatMeetingId="0";
		//根据复制会议还是周期会议判断,相应修改对应的值
		if(isCopy){//复制会议,只修改创建人和联系人
			if(user != null){//有效人员.
				contacter=""+user.getUID();
				creater=""+user.getUID();
			}
			name+="("+SystemEnv.getHtmlLabelName(77, user.getLanguage()) +")";

		}else{//克隆周期会议
			begindate=date;
			enddate=date;
			//场景:多时区情况下,生成周期会议,可能存在当前时间不跨天,但是转换到服务器时间就跨天了,然后可能就会出现开始时间>结束时间
			//不管是往前跨,还是往后跨,都是根据开始日期来的,当出现这种情况的时候那么就需要将结束日期+1
			if(!endtime.equals("")&&begintime.compareTo(endtime) > 0){
				Calendar c = Calendar.getInstance();
				Date date1=null;
				try {
					date1 = new SimpleDateFormat("yy-MM-dd").parse(date);
				} catch (Exception e) {
					e.printStackTrace();
				}
				c.setTime(date1);
				int day=c.get(Calendar.DATE);
				c.set(Calendar.DATE,day+1);

				String dayAfter=new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
				enddate = dayAfter;
			}
			repeatType=0;//生成正常会议
			meetingstatus=2;//直接生成正常会议
			repeatMeetingId=meetingid;
			remindImmediately=0;//不支持立即提醒
		}
		//正常会议时,情况重复策略
		if(repeatType<=0){
			repeatType=0;
			repeatdays = 0;
			repeatweeks = 0;
			rptWeekDays="";
			repeatmonths = 0;
			repeatmonthdays = 0;
			repeatStrategy =0;
		}
		String SWFRemark="";
		if(meetingSetInfo.getRoomConflictChk()==1){//开启会议冲突提醒
			if(!isCopy){//周期会议
				String roomName=chkMeetingRoom(meetingid, address, begindate, begintime, enddate, endtime);
				if(!"0".equals(roomName)){//存在冲突
					SWFRemark+=Util.toMultiLangScreen("126845")+"，";//此会议由周期会议生成
					SWFRemark += " "+Util.toMultiLangScreen("81901")+":"; //会议时间
					SWFRemark += begindate+" "+begintime;
					SWFRemark +=" "+Util.toMultiLangScreen("2105")+":"+roomName+customizeAddress;
					if(meetingSetInfo.getRoomConflict()==1){//冲突处理仅提醒
						SWFRemark+=","+Util.toMultiLangScreen("19432")+"！";//会议起止时间内会议室使用冲突
					}else{//冲突禁止提交
						meetingstatus=0;//会议状态为草稿
						SWFRemark+=","+Util.toMultiLangScreen("19432,126846")+"!";//会议起止时间内会议室使用冲突，已将此会议生成为草稿状态！";
					}
				}
			}
		}
		//对参会人员，联系人，召集人 进行人员状态筛选
		ResourceComInfo resourceComInfo = new ResourceComInfo();
		if(!contacter.equals("")){
			int tempStatus=Util.getIntValue(resourceComInfo.getStatus(contacter));
			if(4<tempStatus){//离职人员不发送提醒
				contacter = user.getUID()+"";
			}
		}else{
//			contacter = user.getUID()+"";
		}
		if(!caller.equals("")){
			int tempStatus=Util.getIntValue(resourceComInfo.getStatus(caller));
			if(4<tempStatus){//离职人员不发送提醒
				caller = user.getUID()+"";
			}
		}else{
			caller = user.getUID()+"";
		}
		List<String> resourceIDList = Util.TokenizerString(hrmmembers, ",");
		hrmmembers = resourceIDList.stream().filter(item->{
			int tempStatus=Util.getIntValue(resourceComInfo.getStatus(item));
			if(4<tempStatus){
				return false;
			}else{
				return true;
			}
		}).collect(Collectors.joining(","));

		String description = ""+ SystemEnv.getHtmlLabelName(10000849,weaver.general.ThreadVarLanguage.getLang())+": "+name+"   "+ SystemEnv.getHtmlLabelName(81901,weaver.general.ThreadVarLanguage.getLang())+":"+begindate+" "+begintime+" "+ SystemEnv.getHtmlLabelName(2105,weaver.general.ThreadVarLanguage.getLang())+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
		if(StringUtils.isBlank(meetingtype)){
			meetingtype = null;
		}
		if(StringUtils.isBlank(projectid)){
			projectid = null;
		}
		ProcPara =  meetingtype;
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
		ProcPara += flag + creater;
		ProcPara += flag + CurrentDate;
		ProcPara += flag + CurrentTime;
		ProcPara += flag + ""+totalmember;
		ProcPara += flag + othermembers;
		ProcPara += flag + addressdesc;
		ProcPara += flag + description;
		ProcPara += flag + ""+remindType;
		ProcPara += flag + ""+remindBeforeStart;
		ProcPara += flag + ""+remindBeforeEnd;
		ProcPara += flag + ""+remindTimesBeforeStart;
		ProcPara += flag + ""+remindTimesBeforeEnd;
		ProcPara += flag + customizeAddress;
		String uuid = UUID.randomUUID().toString();
		ProcPara += flag + uuid;
		if (recordSet.getDBType().equals("oracle") || "mysql".equalsIgnoreCase(recordSet.getDBType())|| "postgresql".equalsIgnoreCase(recordSet.getDBType()))
		{
			recordSet.executeProc("Meeting_Insert",ProcPara);

			recordSet.executeQuery("SELECT id FROM Meeting where uuid = ?", uuid);
		}
		else
		{
			recordSet.executeProc("Meeting_Insert",ProcPara);
		}
		recordSet.next();
		String MaxID = recordSet.getString(1);
		//更新其他字段
		List updateValueList=new ArrayList();
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
				+" , repeatdays = ? "
				+" , repeatweeks = ?"
				+" , rptWeekDays = ?"
				+" , repeatbegindate = ? "
				+" , repeatenddate = ? "
				+" , repeatmonths = ?"
				+" , repeatmonthdays = ?"
				+" , repeatStrategy = ?"
				+" , roomType = ?"
				+" , secretLevel = ? "
				+" , secretDeadline = ? "
				+" , remindTypeNew = ? "
				+" , remindImmediately = ?"
				+" , remindHoursBeforeStart = ?"
				+" , remindHoursBeforeEnd = ?";
		if(recordSet.getDBType().equalsIgnoreCase("oracle")&&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")){
			updateSql+=" , hrmmembers = empty_clob() ";
		}else{
			updateSql+=" , hrmmembers = ? ";
			updateValueList.add(hrmmembers);
		}
		updateValueList.add(crmmembers);
		updateValueList.add(crmtotalmember);
		updateValueList.add(accessorys);
		updateValueList.add(meetingstatus);
		updateValueList.add(repeatMeetingId);
		updateValueList.add(hrmSubCompanys);
		updateValueList.add(hrmDepartments);
		updateValueList.add(MaxID);
		updateSql+=" , crmmembers = ? "
				+" , crmtotalmember = ?"
				+" , accessorys = ? "
				+" , meetingstatus = ? "
				+" , repeatMeetingId = ? "
				+ " , hrmSubCompanys = ? "
				+ " , hrmDepartments = ? "
				+" where id = ?";
		recordSet.executeUpdate(updateSql,updateValueList);
		if(recordSet.getDBType().equalsIgnoreCase("oracle")&&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")){
			meetingUtil.updateHrmmembers(MaxID,hrmmembers);
		}
		//对密级进行加密
		HrmClassifiedProtectionBiz hrmClassifiedProtectionBiz = new HrmClassifiedProtectionBiz();
		boolean isOpenSecret = hrmClassifiedProtectionBiz.isOpenClassification();
		if(isOpenSecret && !"".equals(secretLevel)){
			EncryptUtil encryptUtil = new EncryptUtil();
			Map<String,String> map = encryptUtil.getLevelCRC(MaxID,secretLevel);
			String encKey = map.get("encKey");
			String crc = map.get("crc");
			RecordSet rs1 = new RecordSet();
			rs1.executeUpdate("update meeting set encKey = ?,crc = ? where id = ? ", encKey,crc,MaxID);
		}

		//新加日志
		Map<String,Object> params = new HashMap<String,Object>();
		user = new User(Util.getIntValue(rs.getString("creater")));
		params.put("id",MaxID);
		params.put("name",name);
		params.put("creater",user.getUID());
		params.put("createDate",CurrentDate);
		params.put("createTime",CurrentTime);
		params.put("caller",caller);
		params.put("hrmmembers",hrmmembers);
		params.put("desc_n",desc);
		params.put("beginDateTime",begindate +" " + begintime);
		params.put("endDateTime",enddate +" " + endtime);
		params.put("address",address.equals("")?customizeAddress:address);
		params.put("addressName",address.equals("")?customizeAddress:new MeetingRoomComInfo().getMeetingRoomInfoname(address));
		bizLogContext.setDateObject(new Date());
		bizLogContext.setUserid(user.getUID());
		bizLogContext.setTargetId(MaxID);
		bizLogContext.setTargetName(name);
		bizLogContext.setNewValues(params);
		bizLogContext.setUsertype(Util.getIntValue(user.getLogintype()));
		bizLogContext.setBelongType(BizLogSmallType4Meeting.MEETING_BASE);//所属类型
		bizLogContext.setBelongTypeTargetId(MaxID);//所属类型id
		bizLogContext.setBelongTypeTargetName(name);//所属类型名称
		bizLogContext.setLogType(BizLogType.MEETING);
		bizLogContext.setLogSmallType(BizLogSmallType4Meeting.MEETING_BASE);
		bizLogContext.setOperateType(BizLogOperateType.ADD);
		bizLogContext.setParams(params);
		String mainId = bizLogContext.createMainid();
		bizLogContext.setMainId(mainId);
		bizLogContext.setClientIp(ip);
		LogUtil.writeBizLog(bizLogContext);

		//保存自定义字段
		MeetingFieldManager mfm=new MeetingFieldManager(1);
		mfm.editCustomData(rs,Util.getIntValue(MaxID),isCopy);
		//保存参会人员,直接拿主表保存参会人员数据. 过滤下参会人员离职
		//更新会议共享以及查看状态
		MeetingUtil.updateMM2andMV("",MaxID);
		//复制议程
		recordSet.executeProc("Meeting_Topic_SelectAll",""+meetingid);
		MeetingFieldManager mfm2=new MeetingFieldManager(2);
		mfm2.editCustomDataDetail(recordSet, Util.getIntValue(MaxID),user);
		//复制会议服务
		recordSet.executeQuery("select * from meeting_service_new where meetingid=? order by id asc",meetingid);
		MeetingFieldManager mfm3=new MeetingFieldManager(3);
		mfm3.editCustomDataDetail(recordSet, Util.getIntValue(MaxID),user);
		//设置会议权限
		meetingViewer.setMeetingShareById(""+MaxID);
		//设置相关文档和附件权限
		new MeetingUtil().meetingDocShare(""+MaxID);

		if(!"".equals(SWFRemark)){
			String SWFTitle=begindate+Util.toMultiLangScreen("126850")+":"; //周期会议冲突提醒
			SWFTitle += name;
			String SWFSubmiter="1";//系统发送
			timer.schedule(new SysRemindTimer(SWFTitle,Util.getIntValue(MaxID),Util.getIntValue(SWFSubmiter),creater,SWFRemark,secretLevel), 5*1000);
		}

		//会议签到
		if(signMap.containsKey("defaultAllowSignTime")){
			MeetingSignService meetingSignService = (MeetingSignServiceImpl) ServiceUtil.getService(MeetingSignServiceImpl.class, user);
			signMap.put("meetingid",MaxID);
			meetingSignService.saveSignCreateSet(signMap);
		}

		//如果正常会议,生成相应日程
		if(meetingstatus==2){
			createWPAndRemind(MaxID,null,"",true);
		}

		if(isCopy){//复制会议，同时复制排座信息
			MeetingSeatUtil.copyMeetingSeat(Util.getIntValue(meetingid),Util.getIntValue(MaxID));
		}


		return Util.getIntValue(MaxID);
	}

    
    /**
     * 根据模板会议生成新的会议
     * @param meetingid
     * @param date
     * @throws Exception
     */
    public static void  cloneMeeting(String meetingid, String date) throws Exception{
    	
    	RecordSet rs = new RecordSet();
    	RecordSet rs1 = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs, rs1);
    	String creater = "";
    	rs.executeSql("select * from meeting where (cancel <> '1' or cancel is null) and meetingstatus = 2 and  id ="+meetingid);
    	if(!rs.next()){
    		rs.writeLog("会议id：["+meetingid+"]生成重复会议失败，模板会议不存在，或者没有审批通过，或者已经取消。");
    	} else {
			creater = rs.getString("creater");
			User createrTmp = new User(Util.getIntValue(creater));
    		//生成周期会议策略
    		int repeatStrategy = Util.getIntValue(rs.getString("repeatStrategy"),0);
    		
    		//根据创建人获取分部id，用来处理工作日
			HrmScheduleDiffUtil hrmScheduleDiffUtil=new HrmScheduleDiffUtil();
			hrmScheduleDiffUtil.setUser(createrTmp);
			//系统管理员没有考勤时间，不用判断工作时间
			if(!createrTmp.getLoginid().equalsIgnoreCase("sysadmin")) {
				//如果是非工作日
				if(!hrmScheduleDiffUtil.getIsWorkday(date) ){
					//推迟到下一工作日
					if(repeatStrategy == 1){
						String tdate = getWorkDayByDays(date, 1, creater);
						rs.writeLog("会议id：["+meetingid+"]日期["+date+"]生成重复会议推迟到["+tdate+"]。");
						date=tdate;
					} else if(repeatStrategy == 2) {
						//取消会议
						rs.writeLog("会议id：["+meetingid+"]日期["+date+"]生成重复会议取消。");
						return;
					}
				}
			}

    		
    		copyMeeting(rs,false,date,null);
            return;
    	}
    }
    
    /**
     * 查询会议内容结果集,返回空,则未查询到值
     * @param meetingId
     * @return
     */
    private static RecordSet getMeetingData(String meetingId){
    	RecordSet rsData=new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rsData);
    	rsData.execute("select * from meeting where (cancel <> '1' or cancel is null) and id="+meetingId);
		if(rsData.next()){
			return rsData;
		}else{
			return null;
		}
    }

    /**
     * 复制会议
     * 或者克隆会议(生成周期会议)
     * @param rs  会议参考对象 结果集
     * @param isCopy  复制:true or 克隆:false
     * @param date		克隆时间
     * @param user	复制人
     * @return
     */
    private static int copyMeeting(RecordSet rs,boolean isCopy,String date,User user) throws Exception{

		BizLogContext bizLogContext=new BizLogContext();

    	MeetingViewer meetingViewer = new MeetingViewer();
		MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
		MeetingSetInfo meetingSetInfo=new MeetingSetInfo();
		MeetingUtil meetingUtil = new MeetingUtil();
		Timer timer = new Timer();
		RecordSet recordSet=new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(recordSet,rs);
		Date newdate = new Date() ;
		long datetime = newdate.getTime() ;
		Timestamp timestamp = new Timestamp(datetime) ;
		String CurrentDate = (timestamp.toString()).substring(0,4) + "-" + (timestamp.toString()).substring(5,7) + "-" +(timestamp.toString()).substring(8,10);
		String CurrentTime = (timestamp.toString()).substring(11,13) + ":" + (timestamp.toString()).substring(14,16) + ":" +(timestamp.toString()).substring(17,19);
		char flag = 2;
		String ProcPara = "";
    	//基本信息
		String meetingid=rs.getString("id");
		String meetingtype = Util.null2String(rs.getString("meetingtype"));
		String name=Util.null2String(rs.getString("name"));//会议名称
		String caller=Util.null2String(rs.getString("caller"));//召集人,必填
		String contacter=Util.null2String(rs.getString("contacter"));//联系人,空值使用当前操作人
		String creater=Util.null2String(rs.getString("creater"));
		String secretDeadline=Util.null2String(rs.getString("secretDeadline"));
		String secretLevel = rs.getString("secretLevel");
		if(secretLevel.equals("")){
			secretLevel = MeetingUtil.DEFAULT_SECRET_LEVEL;
		}
		//会议室
		int roomType = rs.getInt("roomType");
		String address=Util.null2String(rs.getString("address"));//会议地点
		String customizeAddress = Util.null2String(rs.getString("customizeAddress"));
		String desc = Util.htmlFilter4UTF8(Util.null2String(rs.getString("desc_n")));//描述,可为空
    	//时间
    	int repeatType = Util.getIntValue(rs.getString("repeatType"),0);//是否是重复会议,0 正常会议.
    	String begindate=Util.null2String(rs.getString("begindate"));//开始日期
    	String enddate=Util.null2String(rs.getString("enddate"));//结束日期
    	String repeatbegindate=Util.null2String(rs.getString("repeatbegindate"));//重复开始日期
    	String repeatenddate=Util.null2String(rs.getString("repeatenddate"));//重复结束日期
		String begintime=Util.null2String(rs.getString("begintime"));//开始时间
		String endtime=Util.null2String(rs.getString("endtime"));//结束时间
    	//重复策略字段
    	int repeatdays = Util.getIntValue(rs.getString("repeatdays"),0);
    	int repeatweeks = Util.getIntValue(rs.getString("repeatweeks"),0);
    	String rptWeekDays=Util.null2String(rs.getString("rptWeekDays"));
    	int repeatmonths = Util.getIntValue(rs.getString("repeatmonths"),0);
    	int repeatmonthdays = Util.getIntValue(rs.getString("repeatmonthdays"),0);
    	int repeatStrategy = Util.getIntValue(rs.getString("repeatStrategy"),0);
		
		//提醒方式和时间
		int remindType=1;//老的提醒方式
		String remindTypeNew=Util.null2String(rs.getString("remindTypeNew"));//新的提示方式
		int remindImmediately = Util.getIntValue(rs.getString("remindImmediately"),0);  //是否立即提醒 
		int remindBeforeStart = Util.getIntValue(rs.getString("remindBeforeStart"),0);  //是否开始前提醒
		int remindBeforeEnd = Util.getIntValue(rs.getString("remindBeforeEnd"),0);  //是否结束前提醒
		int remindHoursBeforeStart = Util.getIntValue(rs.getString("remindHoursBeforeStart"),0);//开始前提醒小时
		int remindTimesBeforeStart = Util.getIntValue(Util.null2String(rs.getString("remindTimesBeforeStart")),0);  //开始前提醒时间
	    int remindHoursBeforeEnd = Util.getIntValue(rs.getString("remindHoursBeforeEnd"),0);//结束前提醒小时
	    int remindTimesBeforeEnd = Util.getIntValue(Util.null2String(rs.getString("remindTimesBeforeEnd")),0);  //结束前提醒时间
	    //参会人员
	    String hrmmembers=Util.null2String(rs.getString("hrmmembers"));//参会人员
		String hrmDepartments = Util.null2String(rs.getString("hrmDepartments"));//参会部门
		String hrmSubCompanys = Util.null2String(rs.getString("hrmSubCompanys"));//参会分部
	    int totalmember=Util.getIntValue(rs.getString("totalmember"),0);//参会人数
		String othermembers=Util.null2String(rs.getString("othermembers"));//其他参会人员
		String crmmembers=Util.null2String(rs.getString("crmmembers"));//参会客户
		int crmtotalmember=Util.getIntValue(rs.getString("crmtotalmember"),0);//参会人数
		//其他信息
		String projectid=Util.null2String(rs.getString("projectid"));	//加入了项目id
		String accessorys = Util.null2String(rs.getString("accessorys"));//会议附件
		String addressdesc=rs.getString("addressdesc");
		int meetingstatus=0;
		//预防MySql更新的时候int型不能直接插入'',给一个默认值0
		String repeatMeetingId="0";
		//根据复制会议还是周期会议判断,相应修改对应的值
		if(isCopy){//复制会议,只修改创建人和联系人
			if(user != null){//有效人员.
				contacter=""+user.getUID();
				creater=""+user.getUID();
			}
			name+="("+SystemEnv.getHtmlLabelName(77, user.getLanguage()) +")";

		}else{//克隆周期会议
			begindate=date;
			enddate=date;
			//场景:多时区情况下,生成周期会议,可能存在当前时间不跨天,但是转换到服务器时间就跨天了,然后可能就会出现开始时间>结束时间
			//不管是往前跨,还是往后跨,都是根据开始日期来的,当出现这种情况的时候那么就需要将结束日期+1
			if(!endtime.equals("")&&begintime.compareTo(endtime) > 0){
				Calendar c = Calendar.getInstance();
				Date date1=null;
				try {
					date1 = new SimpleDateFormat("yy-MM-dd").parse(date);
				} catch (Exception e) {
					e.printStackTrace();
				}
				c.setTime(date1);
				int day=c.get(Calendar.DATE);
				c.set(Calendar.DATE,day+1);

				String dayAfter=new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
				enddate = dayAfter;
			}
			repeatType=0;//生成正常会议
			meetingstatus=2;//直接生成正常会议
			repeatMeetingId=meetingid;
			remindImmediately=0;//不支持立即提醒
		}
		//正常会议时,情况重复策略
		if(repeatType<=0){
			repeatType=0;
			repeatdays = 0;
	    	repeatweeks = 0;
	    	rptWeekDays="";
	    	repeatmonths = 0;
	    	repeatmonthdays = 0;
	    	repeatStrategy =0;
		}
		String SWFRemark="";
		if(meetingSetInfo.getRoomConflictChk()==1){//开启会议冲突提醒
			if(!isCopy){//周期会议
				String roomName=chkMeetingRoom(meetingid, address, begindate, begintime, enddate, endtime);
				if(!"0".equals(roomName)){//存在冲突
					SWFRemark+=Util.toMultiLangScreen("126845")+"，";//此会议由周期会议生成
					SWFRemark += " "+Util.toMultiLangScreen("81901")+":"; //会议时间
					SWFRemark += begindate+" "+begintime;
					SWFRemark +=" "+Util.toMultiLangScreen("2105")+":"+roomName+customizeAddress;
					if(meetingSetInfo.getRoomConflict()==1){//冲突处理仅提醒
						SWFRemark+=","+Util.toMultiLangScreen("19432")+"！";//会议起止时间内会议室使用冲突
					}else{//冲突禁止提交
						meetingstatus=0;//会议状态为草稿
						SWFRemark+=","+Util.toMultiLangScreen("19432,126846")+"!";//会议起止时间内会议室使用冲突，已将此会议生成为草稿状态！";   
					}
				}
			}
		}
		//对参会人员，联系人，召集人 进行人员状态筛选
		ResourceComInfo resourceComInfo = new ResourceComInfo();
		if(!contacter.equals("")){
			int tempStatus=Util.getIntValue(resourceComInfo.getStatus(contacter));
			if(4<tempStatus){//离职人员不发送提醒
				contacter = creater;
			}
		}else{
//			contacter = user.getUID()+"";
		}
		if(!caller.equals("")){
			int tempStatus=Util.getIntValue(resourceComInfo.getStatus(caller));
			if(4<tempStatus){//离职人员不发送提醒
				caller = creater;
			}
		}else{
			caller = creater;
		}
		List<String> resourceIDList = Util.TokenizerString(hrmmembers, ",");
		hrmmembers = resourceIDList.stream().filter(item->{
			int tempStatus=Util.getIntValue(resourceComInfo.getStatus(item));
			if(4<tempStatus){
				return false;
			}else{
				return true;
			}
		}).collect(Collectors.joining(","));

		String description = ""+ SystemEnv.getHtmlLabelName(10000849,weaver.general.ThreadVarLanguage.getLang())+": "+name+"   "+ SystemEnv.getHtmlLabelName(81901,weaver.general.ThreadVarLanguage.getLang())+":"+begindate+" "+begintime+" "+ SystemEnv.getHtmlLabelName(2105,weaver.general.ThreadVarLanguage.getLang())+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
		if(StringUtils.isBlank(meetingtype)){
			meetingtype = "0";
		}
		if(StringUtils.isBlank(projectid)){
			projectid = "0";
		}
		ProcPara =  meetingtype;
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
		ProcPara += flag + creater;
		ProcPara += flag + CurrentDate;
		ProcPara += flag + CurrentTime;
	    ProcPara += flag + ""+totalmember;
	    ProcPara += flag + othermembers;
	    ProcPara += flag + addressdesc;
	    ProcPara += flag + description;
	    ProcPara += flag + ""+remindType;
	    ProcPara += flag + ""+remindBeforeStart;
	    ProcPara += flag + ""+remindBeforeEnd;
	    ProcPara += flag + ""+remindTimesBeforeStart;
	    ProcPara += flag + ""+remindTimesBeforeEnd;
	    ProcPara += flag + customizeAddress;
		String uuid = UUID.randomUUID().toString();
		ProcPara += flag + uuid;
	    if (recordSet.getDBType().equals("oracle") || "mysql".equalsIgnoreCase(recordSet.getDBType())|| "postgresql".equalsIgnoreCase(recordSet.getDBType()))
		{
	    	recordSet.executeProc("Meeting_Insert",ProcPara);

			recordSet.executeQuery("SELECT id FROM Meeting where uuid = ?", uuid);
		}
		else
		{
			recordSet.executeProc("Meeting_Insert",ProcPara);
		}
	    recordSet.next();
		String MaxID = recordSet.getString(1);
		//更新其他字段
		List updateValueList=new ArrayList();
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
				+" , repeatdays = ? "
				+" , repeatweeks = ?"
				+" , rptWeekDays = ?"
				+" , repeatbegindate = ? "
				+" , repeatenddate = ? "
				+" , repeatmonths = ?"
				+" , repeatmonthdays = ?"
				+" , repeatStrategy = ?"
				+" , roomType = ?"
				+" , secretLevel = ? "
				+" , secretDeadline = ? "
				+" , remindTypeNew = ? "
				+" , remindImmediately = ?"
				+" , remindHoursBeforeStart = ?"
				+" , remindHoursBeforeEnd = ?";
		if(recordSet.getDBType().equalsIgnoreCase("oracle")&&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")){
			updateSql+=" , hrmmembers = empty_clob() ";
		}else{
			updateSql+=" , hrmmembers = ? ";
			updateValueList.add(hrmmembers);
		}
		updateValueList.add(crmmembers);
		updateValueList.add(crmtotalmember);
		updateValueList.add(accessorys);
		updateValueList.add(meetingstatus);
		updateValueList.add(repeatMeetingId);
		updateValueList.add(hrmSubCompanys);
		updateValueList.add(hrmDepartments);
		updateValueList.add(MaxID);
		updateSql+=" , crmmembers = ? "
				+" , crmtotalmember = ?"
				+" , accessorys = ? "
				+" , meetingstatus = ? "
				+" , repeatMeetingId = ? "
				+ " , hrmSubCompanys = ? "
				+ " , hrmDepartments = ? "
				+" where id = ?";
		recordSet.executeUpdate(updateSql,updateValueList);
		if(recordSet.getDBType().equalsIgnoreCase("oracle")&&Util.null2String(recordSet.getOrgindbtype()).equals("oracle")){
			meetingUtil.updateHrmmembers(MaxID,hrmmembers);
		}

		//对密级进行加密
		HrmClassifiedProtectionBiz hrmClassifiedProtectionBiz = new HrmClassifiedProtectionBiz();
		boolean isOpenSecret = hrmClassifiedProtectionBiz.isOpenClassification();
		if(isOpenSecret && !"".equals(secretLevel)){
			EncryptUtil encryptUtil = new EncryptUtil();
			Map<String,String> map = encryptUtil.getLevelCRC(MaxID,secretLevel);
			String encKey = map.get("encKey");
			String crc = map.get("crc");
			RecordSet rs1 = new RecordSet();
			rs1.executeUpdate("update meeting set encKey = ?,crc = ? where id = ? ", encKey,crc,MaxID);
		}

//    	String updateSql = "update Meeting set repeatType = " + repeatType
//						+" , repeatdays = "+ repeatdays
//						+" , repeatweeks = "+ repeatweeks
//						+" , rptWeekDays = '"+ rptWeekDays +"' "
//						+" , repeatbegindate = '"+ repeatbegindate +"' "
//						+" , repeatenddate = '"+ repeatenddate +"' "
//						+" , repeatmonths = "+ repeatmonths
//						+" , repeatmonthdays = "+ repeatmonthdays
//						+" , repeatStrategy = "+ repeatStrategy
//						+" , roomType = "+ roomType
//						+" , secretLevel = "+ secretLevel
//						+" , remindTypeNew = '"+ remindTypeNew+"' "
//						+" , remindImmediately = "+ remindImmediately
//						+" , remindHoursBeforeStart = "+ remindHoursBeforeStart
//						+" , remindHoursBeforeEnd = "+ remindHoursBeforeEnd
//						+" , hrmmembers = '"+ hrmmembers+"' "
//						+" , crmmembers = '"+ crmmembers+"' "
//						+" , crmtotalmember = "+ crmtotalmember
//						+" ,accessorys = '"+accessorys+"'"
//						+" ,meetingstatus = "+meetingstatus
//						+" ,repeatMeetingId = '"+repeatMeetingId+"' "
//						+" where id = " + MaxID;
//
//    	recordSet.executeSql(updateSql);

		//新加日志
		Map<String,Object> params = new HashMap<String,Object>();
		user = new User(Util.getIntValue(rs.getString("creater")));
		params.put("id",MaxID);
		params.put("name",name);
		params.put("creater",user.getUID());
		params.put("createDate",CurrentDate);
		params.put("createTime",CurrentTime);
		params.put("caller",caller);
		params.put("hrmmembers",hrmmembers);
		params.put("desc_n",desc);
		params.put("beginDateTime",begindate +" " + begintime);
		params.put("endDateTime",enddate +" " + endtime);
		params.put("address",address.equals("")?customizeAddress:address);
		params.put("addressName",address.equals("")?customizeAddress:new MeetingRoomComInfo().getMeetingRoomInfoname(address));
		bizLogContext.setDateObject(new Date());
		bizLogContext.setUserid(user.getUID());
		bizLogContext.setTargetId(MaxID);
		bizLogContext.setTargetName(name);
		bizLogContext.setNewValues(params);
		bizLogContext.setUsertype(Util.getIntValue(user.getLogintype()));
		bizLogContext.setBelongType(BizLogSmallType4Meeting.MEETING_BASE);//所属类型
		bizLogContext.setBelongTypeTargetId(MaxID);//所属类型id
		bizLogContext.setBelongTypeTargetName(name);//所属类型名称
		bizLogContext.setLogType(BizLogType.MEETING);
		bizLogContext.setLogSmallType(BizLogSmallType4Meeting.MEETING_BASE);
		bizLogContext.setOperateType(BizLogOperateType.ADD);
		bizLogContext.setParams(params);
		String mainId = bizLogContext.createMainid();
		bizLogContext.setMainId(mainId);
		bizLogContext.setClientIp(Util.null2String("127.0.0.1"));
		LogUtil.writeBizLog(bizLogContext);

		//保存自定义字段
		MeetingFieldManager mfm=new MeetingFieldManager(1);
		mfm.editCustomData(rs,Util.getIntValue(MaxID),isCopy);
		//保存参会人员,直接拿主表保存参会人员数据. 过滤下参会人员离职
		//更新会议共享以及查看状态
		MeetingUtil.updateMM2andMV("",MaxID);
		//复制议程
    	recordSet.executeProc("Meeting_Topic_SelectAll",""+meetingid);
    	MeetingFieldManager mfm2=new MeetingFieldManager(2);
    	mfm2.editCustomDataDetail(recordSet, Util.getIntValue(MaxID),user);
		//复制会议服务
    	recordSet.executeQuery("select * from meeting_service_new where meetingid=? order by id asc",meetingid);
    	MeetingFieldManager mfm3=new MeetingFieldManager(3);
    	mfm3.editCustomDataDetail(recordSet, Util.getIntValue(MaxID),user);
		//设置会议权限
		meetingViewer.setMeetingShareById(""+MaxID);
				//设置相关文档和附件权限
		new MeetingUtil().meetingDocShare(""+MaxID);

		if(!"".equals(SWFRemark)){
			String SWFTitle=begindate+Util.toMultiLangScreen("126850")+":"; //周期会议冲突提醒
			SWFTitle += name;
			String SWFSubmiter="1";//系统发送
			timer.schedule(new SysRemindTimer(SWFTitle,Util.getIntValue(MaxID),Util.getIntValue(SWFSubmiter),creater,SWFRemark,secretLevel), 5*1000);
		}
		//如果正常会议,生成相应日程
		if(meetingstatus==2){
			createWPAndRemind(MaxID,null,"",true);
		}

		if(isCopy){//复制会议，同时复制排座信息
			MeetingSeatUtil.copyMeetingSeat(Util.getIntValue(meetingid),Util.getIntValue(MaxID));
		}


		return Util.getIntValue(MaxID);
    }
    
    /**
     * 生成日程 和 会议提醒
     * @param meetingid
     * @param date
     * @param ip
     */
    public static void createWPAndRemind(String meetingid,String date,String ip) throws Exception{
    	createWPAndRemind(meetingid,date,ip,false);
    }
    
    /**
     * 周期会议 生成日程 和 会议提醒
     * @param meetingid
     * @param date
     * @param ip
     * @param isClone 是否由周期会议生成
     */
    public static void createWPAndRemind(String meetingid,String date,String ip,boolean isClone) throws Exception{
    	createWPAndRemind(meetingid,date,ip,isClone,false);
    }

	/**
	 * 周期会议 生成日程 和 会议提醒
	 * @param meetingid
	 * @param date
	 * @param ip
	 * @param isClone 是否由周期会议生成
	 */
	public static void createWPAndRemind(String meetingid,String date,String ip,boolean isClone,boolean isChange) throws Exception{
		createWPAndRemind(meetingid,date,ip,isClone,isChange,false);
	}
    
    
    /**
     * 周期会议 生成日程 和 会议提醒
     * @param meetingid
     * @param date
     * @param ip
     * @param isClone 是否由周期会议生成
     * @param isChange 是否会议变更
     */
    public static void createWPAndRemind(String meetingid, String date, String ip, boolean isClone, boolean isChange, boolean isReHrm) throws Exception {
        MeetingSetInfo meetingSetInfo = new MeetingSetInfo();
        RecordSet rs = new RecordSet();
        RecordSet rs1 = new RecordSet();
        RecordSet recordSet = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs, recordSet);
        rs.executeQuery("select * from meeting where (cancel <> '1' or cancel is null) and meetingstatus = 2 and  id =?", meetingid);
        if (!rs.next()) {
            rs.writeLog("会议id：[" + meetingid + "]生成日程和相关提醒失败，会议不存在，或者没有审批通过，或者已经取消。");
        } else {
            MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            Timer timer = new Timer();
            Date newdate = new Date();
            long datetime = newdate.getTime();
            Timestamp timestamp = new Timestamp(datetime);
            String CurrentDate = (timestamp.toString()).substring(0, 4) + "-" + (timestamp.toString()).substring(5, 7) + "-" + (timestamp.toString()).substring(8, 10);
            String CurrentTime = (timestamp.toString()).substring(11, 13) + ":" + (timestamp.toString()).substring(14, 16) + ":" + (timestamp.toString()).substring(17, 19);
            char flag = 2;
            String name = Util.null2String(rs.getString("name"));
            String secretLevel = Util.null2String(rs.getString("secretLevel"), MeetingUtil.DEFAULT_SECRET_LEVEL);
            String secretDeadline = Util.null2String(rs.getString("secretDeadline"));
            String caller = Util.null2String(rs.getString("caller"));
            String contacter = Util.null2String(rs.getString("contacter"));
            String address = Util.null2String(rs.getString("address"));
            String creater = Util.null2String(rs.getString("creater"));
            String begindate = Util.null2String(rs.getString("begindate"));
            String begintime = Util.null2String(rs.getString("begintime"));
            String desc = Util.spacetoHtml(Util.null2String(rs.getString("desc")));
            String enddate = Util.null2String(rs.getString("enddate"));
            String endtime = Util.null2String(rs.getString("endtime"));
            String customizeAddress = Util.null2String(rs.getString("customizeAddress"));
            String ewsid = Util.null2String(rs.getString("ewsid"));
            String ewsupdatedate = Util.null2String(rs.getString("ewsupdatedate"));
//    	    String createdate=Util.null2String(rs.getString("createdate"));
//            String createtime=Util.null2String(rs.getString("createtime"));

    	    String remindTypeNew=Util.null2String(rs.getString("remindTypeNew"));//新的提示方式
    		int remindImmediately = Util.getIntValue(rs.getString("remindImmediately"),0);  //是否立即提醒 
    		int remindBeforeStart = Util.getIntValue(rs.getString("remindBeforeStart"),0);  //是否开始前提醒
    		int remindBeforeEnd = Util.getIntValue(rs.getString("remindBeforeEnd"),0);  //是否结束前提醒
    		int remindHoursBeforeStart = Util.getIntValue(rs.getString("remindHoursBeforeStart"),0);//开始前提醒小时
    		int remindTimesBeforeStart = Util.getIntValue(Util.null2String(rs.getString("remindTimesBeforeStart")),0);  //开始前提醒时间
    	    int remindHoursBeforeEnd = Util.getIntValue(rs.getString("remindHoursBeforeEnd"),0);//结束前提醒小时
    	    int remindTimesBeforeEnd = Util.getIntValue(Util.null2String(rs.getString("remindTimesBeforeEnd")),0);  //结束前提醒时间
			String hrmDepartments = Util.null2String(rs.getString("hrmDepartments"));//参会部门
			String hrmSubCompanys = Util.null2String(rs.getString("hrmSubCompanys"));//参会分部
			int requestid = Util.getIntValue(rs.getString("requestid"));//流程id

			rs1.execute("select * from MeetingSet order by id");
			if (rs1.next()&&requestid>0) {
				MeetingSignService meetingSignService = (MeetingSignServiceImpl) ServiceUtil.getService(MeetingSignServiceImpl.class, new User(Util.getIntValue(caller)));
				int defaultAllowSignTime = Util.getIntValue(rs1.getString("defaultAllowSignTime"), 0);
				int allowSignBack = Util.getIntValue(rs1.getString("allowSignBack"));
				int afterSignCanBack = Util.getIntValue(rs1.getString("afterSignCanBack"));
				int defaultAllowSignBackTime = Util.getIntValue(rs1.getString("defaultAllowSignBackTime"));
				Map saveSignSetMap = new HashMap();
				saveSignSetMap.put("defaultAllowSignTime",defaultAllowSignTime);
				saveSignSetMap.put("allowSignBack",allowSignBack);
				saveSignSetMap.put("afterSignCanBack",afterSignCanBack);
				saveSignSetMap.put("defaultAllowSignBackTime",defaultAllowSignBackTime);
				saveSignSetMap.put("meetingid",meetingid);
				meetingSignService.saveSignCreateSet(saveSignSetMap);
			}

			UnifiedConversionInterface uci = new UnifiedConversionInterface();
			boolean needTimeZone = uci.getTimeZoneStatus();
			String timeZoneShow = "";
			if(needTimeZone && TimeZoneCastUtil.canCastZone){
				timeZoneShow = "(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")";
			}else{
				timeZoneShow = "";
			}
    	    String description= Util.toMultiLangScreen("84535,2103")+":"+name+" "+Util.toMultiLangScreen("81901")+":"+begindate+" "+begintime+timeZoneShow+" "+Util.toMultiLangScreen("2105")+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
    	    
    	    if(date!=null&&!"".equals(date)){
    	    	begindate=date;
    	    	enddate=date;
    	    }
			//更新会议共享以及查看状态(目的:防止分部或者部门在生成正常会议之前添加或者删除人员)
			//还没有生成正常状态下的会议只有召集人/联系人/创建人/审批人可以看到
			//在变更的情况下,meetingchangecmd中已经处理
			if(!isReHrm&&!isChange){
    	    	if(!hrmDepartments.equals("") || !hrmSubCompanys.equals("")){
					MeetingUtil.updateMM2andMV(creater,meetingid);
				}
			}

			MeetingUtil meetingUtil = new MeetingUtil();
			String realAddress = meetingUtil.dealRealMeetingAddress(address);
			RecordSet rss = new RecordSet();
			rss.executeUpdate("UPDATE meeting SET realaddress = ? WHERE id = ?",realAddress,meetingid);//记录一下这个会议的真实的realaddress。组合的话，就用组合+实体。

    	    //生成日程接收人和流程接收人的 接收者,接受者同用户提交的参会人顺序一致
    	    String SWFAccepter = "";
    	    //参会回执的时候 其他人员接受者;   为了日程接收人和参会人员一致,,日程接收人后面放回执时带的其他参会人员
    	    String otherMemberAccepter = "";
    	    String sql="select membermanager,othermember,isattend from Meeting_Member2 where meetingid=? order by id";
			recordSet.executeQuery(sql,meetingid);
			while(recordSet.next()){
				if((","+SWFAccepter+",").indexOf(","+recordSet.getString(1)+",")>-1){
					continue;
				}
				//818733 标准任务，841660中修复，变更的情况下，不会带出其他参会人员
				if(!(recordSet.getString(2)).equals("") && (","+SWFAccepter+",").indexOf(","+recordSet.getString(2)+",")<0 && !(!isReHrm && isChange)){
					otherMemberAccepter+=","+recordSet.getString(2);
				}
				//变更的情况下都需要添加的
				if((!recordSet.getString(3).equals("2") && !recordSet.getString(3).equals("3")) || (!isReHrm && isChange)){
					SWFAccepter+=","+recordSet.getString(1);
				}

			}
			SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
			Date bDate=sdf.parse(begindate+" "+begintime+":59");
			Date eDate=sdf.parse(enddate+" "+endtime+":00");
			//QC279547会议召集人也会生成日程以及提醒流程(如果召集人/联系人==创建人那么他也生成日程但是没有提醒,如果召集人/联系人在接收人中间那么就pass)
			//提醒方式现在不考虑召集人/联系人
			String workPlanAccepter = SWFAccepter;

			String workFlowAccepter = SWFAccepter;
			boolean hrmContainCaller = true;
			boolean hrmContainContacter = true;
			//生成日程人员:会议参会人以及召集人/联系人,如果召集人/联系人在参会人中就跳过
			//会议中联系人可以为空,召集人必填
			if((","+workPlanAccepter+",").indexOf(","+caller+",") < 0){
				if(meetingSetInfo.getCallAsHrm() == 1){
					workPlanAccepter+=","+caller;
				}
				hrmContainCaller = false;
			}
			if(!contacter.equals("") && ((","+workPlanAccepter+",").indexOf(","+contacter+",") < 0)){
				if(meetingSetInfo.getContacterAsHrm() == 1){
					workPlanAccepter+=","+contacter;
				}
				hrmContainContacter = false;
			}
			if(!otherMemberAccepter.equals("")){
				workPlanAccepter += otherMemberAccepter;
			}
			//生成流程人员:会议参会人以及召集人/联系人,如果召集人/联系人在参会人中或者召集人/联系人==创建人,那么就不发送提醒
			//联系人可能为空
			if(!caller.equals(creater) && (","+workFlowAccepter+",").indexOf(","+caller+",") < 0){
				workFlowAccepter+=","+caller;
			}
			if(!contacter.equals("") && !contacter.equals(creater) && (","+workFlowAccepter+",").indexOf(","+contacter+",") < 0){
				workFlowAccepter+=","+contacter;
			}
    	    if(!"".equals(workPlanAccepter)){
    	    	//SWFAccepter=SWFAccepter.substring(1);
				if(!"".equals(workPlanAccepter)) {
					workPlanAccepter = workPlanAccepter.substring(1);
				}
				if(!"".equals(workFlowAccepter)){
					workFlowAccepter = workFlowAccepter.substring(1);
				}

				//改成调WorkPlanBaseService来创建、编辑日程，便于会议生成日程的无侵入开发
			    Map<String,String> oldWorkPlanMap=new HashMap<String, String>();//考虑到某些客户，一个参会人生成一条日程，会议变更时需要变更多个日程
				boolean editWorkPlan=false;
			    if(isChange){
					recordSet.executeQuery("select distinct id,createrid from workplan where meetingid = ?",meetingid);
					while(recordSet.next()){
						oldWorkPlanMap.put(recordSet.getString("id"),recordSet.getString("createrid"));
						editWorkPlan=true;
					}
				}
				HashMap<String,Object> editWpParams=new HashMap<String, Object>();
				editWpParams.put("workPlanType",Constants.WorkPlan_Type_ConferenceCalendar);//日程类型
				editWpParams.put("planName",name);//标题
				editWpParams.put("urgentLevel",Constants.WorkPlan_Urgent_Normal);//紧急程度
				editWpParams.put("memberIDs",workPlanAccepter);//接收人
				editWpParams.put("beginDate",begindate);//开始日期
				editWpParams.put("endDate",enddate);//结束日期
				if(begintime!=null&&!"".equals(begintime)){
					editWpParams.put("beginTime",begintime);//开始时间
				} else{
					editWpParams.put("beginTime",Constants.WorkPlan_StartTime);//开始时间
				}
				if(endtime!=null&&!"".equals(endtime)){//结束时间
					editWpParams.put("endTime",endtime);
				} else{
					editWpParams.put("endTime",Constants.WorkPlan_EndTime);//结束时间
				}
				editWpParams.put("remindType","1");//提醒方式,会议不通过日程提醒
				editWpParams.put("remindBeforeStart","0");//是否开始前提醒
				editWpParams.put("remindBeforeEnd","0");//是否结束前提醒
				editWpParams.put("remindTimeBeforeStart","0");//开始前提醒时间
				editWpParams.put("remindTimeBeforeEnd","0");//结束前提醒时间
				editWpParams.put("remindDateBeforeStart","0");//开始前提醒日期
				editWpParams.put("remindDateBeforeEnd","0");//结束前提醒日期
				editWpParams.put("meetingIDs",meetingid);//相关会议
				editWpParams.put("description",description);//内容
				editWpParams.put("secretLevel",secretLevel);//密级等级
				editWpParams.put("secretDeadline",secretDeadline);//保密期限
				editWpParams.put(ParamConstant.PARAM_IP,ip);//IP
                //添加exchange相关参数
                editWpParams.put("isFrom","exchange_meeting");
                editWpParams.put("ewsid",ewsid);

				WorkPlanBaseService workPlanBaseService;
				if(editWorkPlan){//编辑
					HashMap<String ,Object> editWpParamsTemp;
					for(Map.Entry<String,String> entry:oldWorkPlanMap.entrySet()){
						editWpParamsTemp=new HashMap<String, Object>();
						editWpParamsTemp.putAll(editWpParams);
						editWpParamsTemp.put("workid",entry.getKey());

						workPlanBaseService=getService(new User(Integer.parseInt(entry.getValue())));
						workPlanBaseService.editWorkPlan(editWpParamsTemp);
					}

				}else{//新建
//                    workPlanBaseService = getService(new User(Integer.parseInt(creater)));
//                    workPlanBaseService.addWorkPlan(editWpParams);
                    WorkplanEwsService workplanEwsService = WorkPlanExchangeFactory.getInstance(creater,1);
	                    workplanEwsService.addWorkPlan(-1,editWpParams);
                }

                //在回执的情况下不考虑提醒方式对应的提醒 --08/29
				if(!"".equals(remindTypeNew) && !isReHrm&&!"".equals(workFlowAccepter)){//选择了提醒方式
					//在变更的情况下,立即提醒转移到变更cmd中执行,因为要可能发送取消/新会议/变更三种类型
					if(remindImmediately==1  && !isChange){//立即提醒
	    	    		if(bDate.getTime()>=newdate.getTime()){//开始时间大于当前时间 会议还未开始时 发送提醒
							remindByThread(meetingid,"");
	    	    		}
	    	    	}

	    	    }
	    	    //查询会议应用设置 是否启用会议创建提醒 && 会议未开始 &&非周期会议 && 不是会议变更（会议变更在meetingoperation.jsp发送变更提醒） && 不是从人员回执来的
				//查询会议应用设置 是否启用会议创建提醒 && 会议未开始 &&非周期会议 && 不是会议变更（会议变更在meetingoperation.jsp发送变更提醒） && 不是从人员回执来的
				int subId = Util.getIntValue(resourceComInfo.getSubCompanyID(creater)) ;
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
				if(recordSet.next()&&(begindate+" "+begintime).compareTo(CurrentDate+" "+CurrentTime)>0&&!isClone&&!isChange&&!isReHrm){
					String id =recordSet.getString(1);
					String wfId =recordSet.getString(2);
					createReceiptWf(meetingid,"",wfId,id,ip,false);

					if(meetingSetInfo.getCreateMeetingRemindChk()==1){
						//专门给召集人和联系人生成流程提醒
						String callerAndContacter = "";
						if(!hrmContainCaller){
							callerAndContacter = caller;
						}
						if(!hrmContainContacter && !contacter.equals("")){
							callerAndContacter += callerAndContacter.equals("")? contacter:","+contacter;
						}

						String SWFTitle=Util.toMultiLangScreen("24215")+":"; //文字,会议通知
						SWFTitle += name;
						SWFTitle += " "+Util.toMultiLangScreen("81901")+":"; //会议时间
						SWFTitle += begindate+" "+begintime;
						SWFTitle += timeZoneShow;
						SWFTitle +=" "+Util.toMultiLangScreen("2105")+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
						String SWFRemark="";
						String SWFSubmiter=creater;

						//支持无侵入修改流程信息
						//type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）
						beforeWfRemind(1,meetingid,SWFTitle,SWFSubmiter,SWFRemark,callerAndContacter,secretLevel,timer,5*1000);
					}

				}else if(meetingSetInfo.getCreateMeetingRemindChk()==1&&(begindate+" "+begintime).compareTo(CurrentDate+" "+CurrentTime)>0&&!isClone&&!isChange&&!"".equals(workFlowAccepter)&&!isReHrm){
				 //生成流程提醒
				String SWFTitle=Util.toMultiLangScreen("24215")+":"; //文字,会议通知
				SWFTitle += name;
				SWFTitle += " "+Util.toMultiLangScreen("81901")+":"; //会议时间
				SWFTitle += begindate+" "+begintime;
				SWFTitle += timeZoneShow;
				SWFTitle +=" "+Util.toMultiLangScreen("2105")+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
				String SWFRemark="";
				String SWFSubmiter=creater;

				//支持无侵入修改流程信息
				//type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）
				beforeWfRemind(1,meetingid,SWFTitle,SWFSubmiter,SWFRemark,workFlowAccepter,secretLevel,timer,5*1000);

				}	
    	    } else if("".equals(workPlanAccepter)) {
				//删除日程
				recordSet.execute("select id from workplan where meetingid = '"+meetingid+"'");

				while(recordSet.next()){
					weaver.WorkPlan.WorkPlanHandler wph = new weaver.WorkPlan.WorkPlanHandler();
					wph.delete(recordSet.getString("id"));
				}
			}
			//生成正常会议后，参会人为空的时候也对会议提醒表进行更新（meeting_remind）
			if(!"".equals(remindTypeNew) && !isReHrm){
				if(remindBeforeStart==1){//开始前提醒
					List beginDateTimeRemindList = Util.processTimeBySecond(begindate, begintime, (remindHoursBeforeStart*60+remindTimesBeforeStart)* -1 * 60);
					if(bDate.getTime()>=newdate.getTime()){//开始时间大于当前时间 会议还未开始时 发送提醒
						MeetingRemindUtil.remindAtTime(meetingid, (String)beginDateTimeRemindList.get(0)+" "+(String)beginDateTimeRemindList.get(1), "start");
					}
				}
				if(remindBeforeEnd==1){//结束前提醒
					List endDateTimeRemindList = Util.processTimeBySecond(enddate, endtime, (remindHoursBeforeEnd*60+remindTimesBeforeEnd) * -1 * 60);
					if(eDate.getTime()>=newdate.getTime()){//结束时间小于当前时间 会议还未结束时 发送提醒
						MeetingRemindUtil.remindAtTime(meetingid, (String)endDateTimeRemindList.get(0)+" "+(String)endDateTimeRemindList.get(1), "end");
					}
				}
			}
    	    //查询会议应用设置 是否启用会议服务提醒 && 会议未开始 &&非周期会议
    	    if(meetingSetInfo.getRemindMeetingServiceChk()==1&&(begindate+" "+begintime).compareTo(CurrentDate+" "+CurrentTime)>0&&!isClone&&!isChange){
	    	    //生成服务通知
	    	    SWFAccepter="";
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
	    	    for (String tempid:hrmidSet) {
	    	    	SWFAccepter+=","+tempid;
				}
	    	    if(!SWFAccepter.equals("")){
	    	    	SWFAccepter=SWFAccepter.substring(1);
	    	    	String SWFTitle=Util.toMultiLangScreen("2107")+":";//文字,会议服务
	    	    	SWFTitle += name;
					SWFTitle += " "+Util.toMultiLangScreen("81901")+":"; //会议时间
					SWFTitle += begindate+" "+begintime;
					SWFTitle += timeZoneShow;
					SWFTitle +=" "+Util.toMultiLangScreen("2105")+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
	    	    	SWFTitle += "-"+resourceComInfo.getResourcename(creater);
	    	    	String SWFRemark="";

					//支持无侵入修改流程信息
					//type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）
					beforeWfRemind(2,meetingid,SWFTitle,creater,SWFRemark,SWFAccepter,secretLevel,timer, 6*1000);

	    	    }
    	    }
			if (!isReHrm){
				//生成会议室负责人通知
				Set<String> reminderSet = new LinkedHashSet<String>();
				//获取所有开启提醒的fieldid
				Set fieldIdsSet = new HashSet();
				//获取fieldid和reminder
				MeetingRemindUtil.SetRemindAndFields(meetingid,fieldIdsSet,reminderSet,true);
				if(reminderSet.size() > 0){
					SWFAccepter=reminderSet.stream().collect(Collectors.joining(","));
					String SWFTitle=Util.toMultiLangScreen("518371")+":";//文字,会议室使用通知
					SWFTitle += name;
					SWFTitle += " "+Util.toMultiLangScreen("81901")+":"; //会议时间
					SWFTitle += begindate+" "+begintime;
					SWFTitle += timeZoneShow;
					SWFTitle +=" "+Util.toMultiLangScreen("2105")+":"+meetingRoomComInfo.getMeetingRoomInfoname(""+address)+customizeAddress;
					SWFTitle += "-"+resourceComInfo.getResourcename(creater);
					String SWFRemark="";

					//支持无侵入修改流程信息
					//type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）9：会议室负责人
					beforeWfRemind(9,meetingid,SWFTitle,creater,SWFRemark,SWFAccepter,secretLevel,timer, 6*1000);
				}
			}

            //exchange相关
            if(StringUtils.isBlank(ewsid)){
                ThreadPoolUtil.getThreadPool("MeetingToEWS", "5").execute(() -> {
                    MeetingExchangeUtil meetingExchangeUtil = new MeetingExchangeUtil();
                    if (meetingExchangeUtil.canUseExchange() ) {
                        meetingExchangeUtil.doMeeingToEWS(meetingid,creater, 1);
                    }
                });
            }
    	    //放到前面，afterMeetingNormal接口也可以获取到座位号
			MeetingSeatUtil.updateCopyMeetingSeat(Util.getIntValue(meetingid));

    	    Map params = new HashMap();
    	    params.put("meetingid",meetingid);
    	    params.put("isReHrm",isReHrm);
			MeetingBaseService meetingBaseService = (MeetingBaseServiceImpl) ServiceUtil.getService(MeetingBaseServiceImpl.class, new User(Integer.parseInt(creater)));
			//这里沟通后决定，回执的时候，不需要调用方法，防止二开的时候，重复推送数据等问题。
			// 只针对生成的正常会议进行afterMeetingNormal操作，我们这边有变更，回执会走该方法，过滤
			if(!isReHrm && !isChange){
				meetingBaseService.afterMeetingNormal(params);
			}
			MeetingUtil.updateModifyDateTime(meetingid);
            return;
    	}
    }

	/**
	 * 获取无侵入流程提醒数据
	 * @param type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）5:变更
	 * @param meetingId
	 * @param wfTitle 流程标题
	 * @param wfCreater 流程创建人
	 * @param wfRemark 流程签字意见
	 * @param wfAccepter 流程接收人
	 * @param wfSecretLevel 流程密级
	 * @return
	 */
	private static void beforeWfRemind(int type,String meetingId,String wfTitle,String wfCreater,String wfRemark,String wfAccepter,String wfSecretLevel,Timer timer,int delay) {
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
			timer.schedule(new SysRemindTimer(wfTitle,Util.getIntValue(meetingId),Util.getIntValue(wfCreater),wfAccepter,wfRemark,wfSecretLevel), delay);
		}

	}
    
    /**
     * 手机消息使用线程推送
     */
    private static void pushNotificationService(final List<String> useridlist,final String name,final Map<String,String> schedule){
    	new Thread(){
    		public void run() {
    			PushNotificationService pns = new PushNotificationService();
                pns.pushByUserid(StringUtils.join(useridlist, ','), ""+ SystemEnv.getHtmlLabelName(388940,weaver.general.ThreadVarLanguage.getLang())+":"+name, 1, schedule);
    		}
    	}.start();
    }

	/**
	 * 线程提醒方式
	 * @param meetingid
	 * @param accepter
	 */
	private static void remindByThread(final String meetingid,final String accepter){
		new Thread(){
			public void run() {
				MeetingRemindUtil.remindImmediately(meetingid,null,accepter);
			}
		}.start();
	}

	/**
	 * 线程提醒方式
	 * @param meetingid
	 * @param hrmmembers
	 * @param wfId
	 * @param relationWfId
	 * @param ip
	 * @param ischange
	 */
	public static void createReceiptWf(final String meetingid,final String hrmmembers, final String wfId, final String relationWfId, final String ip,final boolean ischange){
		new Thread(){
			public void run() {
				MeetingCreateWFUtil.createReceiptWF(meetingid,hrmmembers,wfId,relationWfId,ip,ischange);
			}
		}.start();
	}

	/**
	 * 线程提醒方式
	 * @param meetingid
	 * @param hrmmembers
	 * @param wfId
	 * @param relationWfId
	 * @param ip
	 * @param ischange
	 */
	public static void createReceiptWf(final User user, final String meetingid,final String hrmmembers, final String wfId, final String relationWfId, final String ip,final boolean ischange,final boolean isChild){
		new Thread(){
			public void run() {
				MeetingCreateWFUtil.createReceiptWF(user,meetingid,hrmmembers,wfId,relationWfId,ip,ischange,isChild,0);
			}
		}.start();
	}

	/**
	 * 线程提醒方式
	 * @param meetingid
	 * @param hrmmembers
	 * @param wfId
	 * @param relationWfId
	 * @param ip
	 * @param ischange
	 */
	public static void createReceiptWf(final User user, final String meetingid,final String hrmmembers, final String wfId, final String relationWfId, final String ip,final boolean ischange,final boolean icChild, final int childId){
		new Thread(){
			public void run() {
				MeetingCreateWFUtil.createReceiptWF(user,meetingid,hrmmembers,wfId,relationWfId,ip,ischange,icChild,childId);
			}
		}.start();
	}


	/**
	 * 线程提醒方式
	 * @param meetingid
	 * @param hrmmembers
	 * @param wfId
	 * @param relationWfId
	 * @param ip
	 * @param ischange
	 * @param isChild
	 */
	public static void createReceiptWf(final String meetingid,final String hrmmembers, final String wfId, final String relationWfId, final String ip,final boolean ischange, final boolean isChild){
		new Thread(){
			public void run() {
				MeetingCreateWFUtil.createReceiptWF(meetingid,hrmmembers,wfId,relationWfId,ip,ischange,isChild);
			}
		}.start();
	}
    
    public static void main(String args[]){
    	MeetingInterval mi = new MeetingInterval();
    	ArrayList begindatelist = mi.getBeginDate("2014-09-25", "2014-10-25", "2", 1, "1,3,6,7");
    	for(int i = 0; i < begindatelist.size(); i++){
    		//System.out.println(begindatelist.get(i));
    	}
    }
    
    /**
     * 检测会议室是否被占用
     * @param meetingid
     * @param meetingaddress
     * @param beginDate
     * @param beginTime
     * @param endDate
     * @param endTime
     * @return
     */
    public static synchronized String chkMeetingRoom(String meetingid,String meetingaddress,String beginDate,String beginTime,String endDate,String endTime){
    	String ret="0";
    	MeetingSetInfo meetingSetInfo=new MeetingSetInfo();
    	RecordSet rs=new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
    	List<String> arr=new ArrayList();
    	
    	if(meetingSetInfo.getRoomConflictChk() == 1 ){

    		rs.executeSql("select address,begindate,enddate,begintime,endtime,id from meeting where meetingstatus in (1,2) and repeatType = 0  and (cancel is null or cancel<>'1') and (begindate <= '"+endDate+"' and enddate >='"+beginDate+"')");
    		while(rs.next()) {
    			String begindatetmp = Util.null2String(rs.getString("begindate"));
    			String begintimetmp = Util.null2String(rs.getString("begintime"));
    			String enddatetmp = Util.null2String(rs.getString("enddate"));
    			String endtimetmp = Util.null2String(rs.getString("endtime"));
    			String addresstmp = Util.null2String(rs.getString("address"));
    			String mid = Util.null2String(rs.getString("id"));

    			String str1 = beginDate+" "+beginTime;
    			String str2 = enddatetmp+" "+endtimetmp;
    			String str3 = endDate+" "+endTime;
    			String str4 = begindatetmp+" "+begintimetmp;

    			String[] address=addresstmp.split(",");
    			for(int i=0;i<address.length;i++){
    				if(!"".equals(meetingaddress) && (","+meetingaddress+",").indexOf(","+address[i]+",")>-1 && !mid.equals(meetingid)) {
    					if((str1.compareTo(str2) < 0 && str3.compareTo(str4) > 0)) {
    						if(!arr.contains(address[i])&&!"".equals(address[i])){
    							arr.add(address[i]);
    						}
    					}
    				}
    			}
    		}
    		try {
				MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
    			for(int i=0;i<arr.size();i++){
    				String name=meetingRoomComInfo.getMeetingRoomInfoname(arr.get(i));
    				if("0".equals(ret)){
    					ret="["+name+"]";
    				}else{
    					ret+=",["+name+"]";
    				}
    			}
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	return ret;
    }

	/**
	 * 获取日程服务
	 * @param user
	 * @return
	 */
	private static WorkPlanBaseService getService(User user) {
		return ServiceUtil.getService(WorkPlanBaseServiceImpl.class, user);
	}

}


