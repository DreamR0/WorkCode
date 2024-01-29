package com.engine.meeting.cmd.meetingroom;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.util.ConditionFactory;
import com.api.browser.util.ConditionType;
import com.api.integration.Base;
import com.api.workflow.bean.PageTabInfo;
import com.engine.common.biz.AbstractBizLog;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.Command;
import com.engine.core.interceptor.CommandContext;
import com.engine.meeting.util.MeetingFieldsUtil;
import com.engine.meeting.util.MeetingSelectOptionsUtil;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.city.CityComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.Maint.MeetingRoomComInfo;
import weaver.meeting.Maint.MeetingRoomReport;
import weaver.meeting.Maint.MeetingSetInfo;
import weaver.meeting.MeetingShareUtil;
import weaver.meeting.MeetingUtil;
import weaver.meeting.ModuleLinkUtil;
import weaver.meeting.defined.MeetingFieldComInfo;
import weaver.systeminfo.SystemEnv;

import java.text.SimpleDateFormat;
import java.util.*;

public class GetRoomReportDataCmd extends AbstractCommonCommand<Map<String, Object>> {
	
    public GetRoomReportDataCmd(User user, Map<String, Object> params){
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
		Map<String, Object> apidatas = new HashMap<String, Object>();
		try{
			String mrname= Util.null2String(params.get("roomname"));
			String equipment=Util.null2String(params.get("equipment"));
			String mrtype=Util.null2String(params.get("mrtype"));
			int subids=Util.getIntValue((String)params.get("subid"));
			int bywhat=Util.getIntValue((String)params.get("bywhat"),4);
			String currentdate=Util.null2String(params.get("currentdate"));
			boolean ismobile=Util.null2String(params.get("ismobile")).equals("1")?true:false;
			String roomid=Util.null2String(params.get("roomid"));
			MeetingSetInfo meetingSetInfo = new MeetingSetInfo();
			String usedColor=meetingSetInfo.getUsedColor();
			String agreementColor=meetingSetInfo.getAgreementColor();
			String conflictedColor=meetingSetInfo.getConflictedColor();
			String usedColorFont=meetingSetInfo.getUsedColorFont();
			String agreementColorFont=meetingSetInfo.getAgreementColorFont();
			String conflictedColorFont=meetingSetInfo.getConflictedColorFont();
			int weekStartDay = meetingSetInfo.getWeekStartDay();
			if(usedColor.equals(""))usedColor="E3F6D8";
			if(agreementColor.equals(""))agreementColor="FFE4C4";
			if(conflictedColor.equals(""))conflictedColor="FBDFEB";

			int dspUnit = meetingSetInfo.getDspUnit();
			Calendar today = Calendar.getInstance();
			Calendar temptoday1 = Calendar.getInstance();
			Calendar temptoday2 = Calendar.getInstance();

			if(!currentdate.equals("")) {
				int tempyear = Util.getIntValue(currentdate.substring(0,4)) ;
				int tempmonth = Util.getIntValue(currentdate.substring(5,7))-1 ;
				int tempdate = Util.getIntValue(currentdate.substring(8,10)) ;
				today.set(tempyear,tempmonth,tempdate);
			}

			int currentyear=today.get(Calendar.YEAR);
			int currentmonth=today.get(Calendar.MONTH);
			int currentday=today.get(Calendar.DATE);


			currentyear=today.get(Calendar.YEAR);
			currentmonth=today.get(Calendar.MONTH)+1;
			currentday=today.get(Calendar.DATE);
			if(bywhat==2){
				currentdate = Util.add0(currentyear,4)+"-"+Util.add0(currentmonth,2);
			}else{
				currentdate = Util.add0(currentyear,4)+"-"+Util.add0(currentmonth,2)+"-"+Util.add0(currentday,2) ;
			}
			temptoday1.set(currentyear,currentmonth-1,currentday) ;
			temptoday2.set(currentyear,currentmonth-1,currentday) ;
			Calendar calendar = Calendar.getInstance();
			calendar.set(currentyear, currentmonth - 1, currentday);
			calendar.add(Calendar.MONTH, 1);
			calendar.set(Calendar.DATE, 1);
			calendar.add(Calendar.DATE, -1);
			int daysOfThisMonth = calendar.get(Calendar.DATE);
			switch (bywhat) {
				case 2:
					today.add(Calendar.MONTH,1) ;
					break ;
				case 3:
					today.add(Calendar.WEEK_OF_YEAR,1) ;
					break;
				case 4:
					today.add(Calendar.DATE,1) ;
					break;
			}

			currentyear=today.get(Calendar.YEAR);
			currentmonth=today.get(Calendar.MONTH)+1;
			currentday=today.get(Calendar.DATE);
			List roomInfoList=new ArrayList();
			MeetingRoomReport mrr=new MeetingRoomReport();
			HashMap mrrHash= new HashMap();
			mrrHash=mrr.getMapping(currentdate,bywhat);
//			if(bywhat==3){
//				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//				Date d = null;
//				d=format.parse(currentdate);
//				Calendar cal = Calendar.getInstance();
//				cal.setTime(d);
//				cal.setFirstDayOfWeek(Calendar.MONDAY);
//				cal.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
//				String weekFirst=format.format(cal.getTime());//获取所选日期所在周的周一
//				mrrHash= mrr.getMapping(weekFirst,bywhat);
//			}else{
//				mrrHash=mrr.getMapping(currentdate,bywhat);
//			}
			ArrayList meetingroomids = new ArrayList() ;
			ArrayList meetingroomnames = new ArrayList() ;
			String sqlwhere = "";

			if(subids > 0){
				sqlwhere += " and a.subCompanyId = "+ subids ;
			}
			if(!"".equals(mrtype)){
				sqlwhere += " and a.mrtype = '" + mrtype + "' ";
			}
			if(!"".equals(equipment)){
				sqlwhere += " and a.equipment like '%" + equipment + "%' ";
			}
			if(!"".equals(mrname.trim())){
				sqlwhere += " and a.name like '%" + mrname + "%' ";
			}
			//作用:手机端会议室使用情况在初始化的时候是显示所有的会议室的,在当前选中天的情况下进行筛选的时候是根据已经取得的useMap来进行筛选
			//但是如果筛选完会议室后再变更日期的时候那么就根据筛选后的会议室来进行查询
			//
			if(!roomid.equals("")){
				if(roomid.startsWith(",")){
					roomid = roomid.substring(1,roomid.length());
				}
				if(roomid.endsWith(",")){
					roomid = roomid.substring(0,roomid.length()-1);
				}
				sqlwhere += " and a.id in ("+roomid+") ";
			}
			sqlwhere += " and (a.status=1 or a.status is null ) ";

			String otherSqlWhere= Util.null2String(params.get("otherSqlWhere"));//其他会议室筛选条件
			if(!"".equals(otherSqlWhere)){
				sqlwhere +=" and ("+otherSqlWhere+") ";
			}


			String sql = "select a.* from MeetingRoom a where 1=1 " + MeetingShareUtil.getRoomShareSqlNew(user) + sqlwhere + " order by dsporder,name ";
			RecordSet RecordSet=new RecordSet();
			RecordSet.executeSql(sql);
			List imageUrlList;
			List imglist;
			String imgid;
			while(RecordSet.next()){
				imageUrlList=new ArrayList();
				String tmpmeetingroomid=RecordSet.getString("id");
				String tmpmeetingroomname=RecordSet.getString("name");
				String tmpimg=RecordSet.getString("images");
				int tmpScreenType =RecordSet.getInt("screenShowType");
				if(!tmpimg.isEmpty()){
					imglist = Util.TokenizerString(tmpimg, ",");
					for(int i=0;i<imglist.size();i++){
						imgid=Util.null2String(imglist.get(i));
						if(!imgid.isEmpty()){
							imageUrlList.add(ModuleLinkUtil.getFileDownload(imgid));
						}
					}

				}

				meetingroomids.add(tmpmeetingroomid) ;
				meetingroomnames.add(tmpmeetingroomname) ;
				List title=getMeetRoomTitle(""+tmpmeetingroomid, new MeetingRoomComInfo(),user);
				Map roomInfoMap=new HashMap();
				roomInfoMap.put("id", tmpmeetingroomid);
				roomInfoMap.put("name", tmpmeetingroomname);
				roomInfoMap.put("title", title);
				roomInfoMap.put("img", imageUrlList);
				roomInfoMap.put("screenShowType", tmpScreenType>0?true:false);
				roomInfoList.add(roomInfoMap);
			}

			//月模式
			if(bywhat==2){
				List list =new ArrayList();
				for(int k=0;k<meetingroomids.size();k++){
					Map roommap=new HashMap();
					String tmproomid=(String) meetingroomids.get(k);
					String roomnames=new MeetingRoomComInfo().getMeetingRoomInfoname(""+tmproomid);

					HashMap tempMap = (HashMap)mrrHash.get((String)meetingroomids.get(k));
					ArrayList ids = (ArrayList)tempMap.get("ids");
					ArrayList beginDates = (ArrayList)tempMap.get("beginDates");
					ArrayList endDates = (ArrayList)tempMap.get("endDates");
					ArrayList names = (ArrayList)tempMap.get("names");
					ArrayList totalmembers = (ArrayList)tempMap.get("totalmembers");
					ArrayList begintimes = (ArrayList)tempMap.get("begintimes");
					ArrayList callers = (ArrayList)tempMap.get("callers");
					ArrayList endtimes = (ArrayList)tempMap.get("endtimes");
					ArrayList contacters = (ArrayList)tempMap.get("contacters");
					ArrayList cancels = (ArrayList)tempMap.get("cancels");
					ArrayList meetingStatuss = (ArrayList)tempMap.get("meetingStatus");
					ArrayList meetingTotalMembers = (ArrayList)tempMap.get("meetingTotalMembers");
					List dayList = new ArrayList();
					for(int j=0; j<daysOfThisMonth; j++)
					{
						Map dateMap=new HashMap();
						String bgcolor="";
						String fontcolor="";
						List tdTitle = new ArrayList();
						int cnt = 0;
						String tmpdate = currentdate + "-"+Util.add0(j+1,2) ;
						String temp = getDayOccupied(tmpdate, beginDates, begintimes, endDates, endtimes, cancels);
						if("2".equals(temp))
						{
							//tdTitle = SystemEnv.getHtmlLabelName(82890,user.getLanguage()) ;
						}
						boolean existDSP=false;//待审批
						List meetingList=new ArrayList();
						for (int h=0 ;h<ids.size();h++)
						{
							String beginDate = (String)beginDates.get(h);
							String endDate = (String)endDates.get(h);

							String name = (String)names.get(h);
							String totalmember = (String)totalmembers.get(h);
							String caller = (String)callers.get(h);
							String contacter = (String)contacters.get(h);
							String begintime = (String)begintimes.get(h);
							String endtime = (String)endtimes.get(h);
							String cancel = (String)cancels.get(h);
							String meetingStatus = (String)meetingStatuss.get(h);
							if(cancel.equals("1"))continue;
							if(tmpdate.compareTo(beginDate)>=0 && tmpdate.compareTo(endDate)<=0)
							{
								if("1".equals(meetingStatus)){
									existDSP=true;
								}
								cnt++;
								//RecordSet rs = new RecordSet();
								String total = (String)meetingTotalMembers.get(h);
//								String id = (String)ids.get(h);
//								rs.executeQuery("select totalmember from meeting where id = ?",id);
//								while(rs.next()){
//									total = rs.getString("totalmember");
//								}
								if(Util.getIntValue(total) > Util.getIntValue(totalmember)){
									totalmember = total;
								}
								tdTitle.add(getMeetRoomTitle(name,totalmember,caller,contacter,beginDate,endDate,begintime,endtime,user));
								Map meetingMap=new HashMap();
								meetingMap.put("id", ids.get(h));
								meetingMap.put("name", name);
								meetingList.add(meetingMap);
							}
						}
						if("2".equals(temp))
						{
							bgcolor="#"+conflictedColor;
							fontcolor=conflictedColorFont;
						}
						else if("1".equals(temp))
						{
							bgcolor="#"+usedColor;
							fontcolor=usedColorFont;
							if(existDSP){
								bgcolor="#"+agreementColor;
								fontcolor=agreementColorFont;
							}
						}
						if(meetingList.size()>0){
							dateMap.put("date", tmpdate);
							dateMap.put("fontcolor", fontcolor);
							dateMap.put("bgcolor", bgcolor);
							dateMap.put("meetings", meetingList);
							dateMap.put("title", tdTitle);
							dateMap.put("content", cnt);
							dayList.add(dateMap);
						}
					}
					if(dayList.size()>0){
						Map map=new HashMap();
						map.put("roomid", tmproomid);
						map.put("info", dayList);
						list.add(map);
					}
				}
				apidatas.put("datas", list);
			}else if(bywhat==3){//周模式
				List list =new ArrayList();
				for(int k=0;k<meetingroomids.size();k++){
					String tmproomid=(String) meetingroomids.get(k);
					String roomnames=new MeetingRoomComInfo().getMeetingRoomInfoname(""+tmproomid);

					HashMap tempMap = (HashMap)mrrHash.get((String)meetingroomids.get(k));
					ArrayList ids = (ArrayList)tempMap.get("ids");
					ArrayList beginDates = (ArrayList)tempMap.get("beginDates");
					ArrayList endDates = (ArrayList)tempMap.get("endDates");
					ArrayList names = (ArrayList)tempMap.get("names");
					ArrayList totalmembers = (ArrayList)tempMap.get("totalmembers");
					ArrayList begintimes = (ArrayList)tempMap.get("begintimes");
					ArrayList callers = (ArrayList)tempMap.get("callers");
					ArrayList endtimes = (ArrayList)tempMap.get("endtimes");
					ArrayList contacters = (ArrayList)tempMap.get("contacters");
					ArrayList cancels = (ArrayList)tempMap.get("cancels");
					ArrayList meetingStatuss = (ArrayList)tempMap.get("meetingStatus");
					ArrayList meetingTotalMembers = (ArrayList)tempMap.get("meetingTotalMembers");

					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					Date d = format.parse(currentdate);
					Calendar cal = Calendar.getInstance();
					cal.setTime(d);
					String selectDayOfWeek = String.valueOf(cal
							.get(Calendar.DAY_OF_WEEK)); //一周第几天
					//周计划显示
					int offsetDays = Integer.parseInt(selectDayOfWeek) - 1;
					cal.add(Calendar.DAY_OF_WEEK, -1
							* Integer.parseInt(selectDayOfWeek) + 1);
					if(weekStartDay==1){
						if(selectDayOfWeek.equals("1")){
							cal.add(Calendar.DATE, -6);
						}else{
							cal.add(Calendar.DATE, 1);
						}
					}
					String weekFirst = Util.add0(cal.get(Calendar.YEAR), 4) + "-" +
							Util.add0(cal.get(Calendar.MONTH) + 1, 2) + "-" +
							Util.add0(cal.get(Calendar.DAY_OF_MONTH), 2);


//					cal.setTime(d);
//					cal.setFirstDayOfWeek(Calendar.MONDAY);
//					cal.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
//					String weekFirst=format.format(cal.getTime());//获取所选日期所在周的周一
					List dayList = new ArrayList();
					for(int j=0; j<7; j++)
					{
						Map dateMap=new HashMap();
						String bgcolor="";
						String fontcolor="";
						List tdTitle = new ArrayList();
						int cnt = 0;
						String tmpdate = TimeUtil.dateAdd(weekFirst,j) ;
						String temp = getDayOccupied(tmpdate, beginDates, begintimes, endDates, endtimes, cancels);
						if("2".equals(temp))
						{
							//tdTitle = SystemEnv.getHtmlLabelName(82890,user.getLanguage()) ;
						}
						boolean existDSP=false;//待审批
						List meetingList=new ArrayList();
						for (int h=0 ;h<ids.size();h++)
						{
							String beginDate = (String)beginDates.get(h);
							String endDate = (String)endDates.get(h);

							String name = (String)names.get(h);
							String totalmember = (String)totalmembers.get(h);
							String caller = (String)callers.get(h);
							String contacter = (String)contacters.get(h);
							String begintime = (String)begintimes.get(h);
							String endtime = (String)endtimes.get(h);
							String cancel = (String)cancels.get(h);
							String meetingStatus = (String)meetingStatuss.get(h);
							if(cancel.equals("1"))continue;
							if(tmpdate.compareTo(beginDate)>=0 && tmpdate.compareTo(endDate)<=0)
							{
								if("1".equals(meetingStatus)){
									existDSP=true;
								}
								cnt++;
								//RecordSet rs = new RecordSet();
								String total = (String)meetingTotalMembers.get(h);
//								String id = (String)ids.get(h);
//								rs.executeQuery("select totalmember from meeting where id = ?",id);
//								while(rs.next()){
//									total = rs.getString("totalmember");
//								}
								if(Util.getIntValue(total) > Util.getIntValue(totalmember)){
									totalmember = total;
								}
								tdTitle.add(getMeetRoomTitle(name,totalmember,caller,contacter,beginDate,endDate,begintime,endtime,user));
								Map meetingMap=new HashMap();
								meetingMap.put("id", ids.get(h));
								meetingMap.put("name", name);
								meetingList.add(meetingMap);
							}
						}
						if("2".equals(temp))
						{
							bgcolor="#"+conflictedColor;
							fontcolor=conflictedColorFont;
						}
						else if("1".equals(temp))
						{
							bgcolor="#"+usedColor;
							fontcolor=usedColorFont;
							if(existDSP){
								bgcolor="#"+agreementColor;
								fontcolor=agreementColorFont;
							}
						}
						if(meetingList.size()>0){
							dateMap.put("date", tmpdate);
							dateMap.put("fontcolor", fontcolor);
							dateMap.put("bgcolor", bgcolor);
							dateMap.put("meetings", meetingList);
							dateMap.put("title", tdTitle);
							dateMap.put("content", cnt);
							dayList.add(dateMap);
						}
					}
					if(dayList.size()>0){
						Map map=new HashMap();
						map.put("roomid", tmproomid);
						map.put("info", dayList);
						list.add(map);
					}
				}
				apidatas.put("datas", list);
			}else if(bywhat==4){//日模式
				List list =new ArrayList();
				for(int k=0;k<meetingroomids.size();k++){
					Map roommap=new HashMap();
					String tmproomid=(String) meetingroomids.get(k);

					String roomnames=new MeetingRoomComInfo().getMeetingRoomInfoname(""+tmproomid);

					HashMap tempMap = (HashMap)mrrHash.get((String)meetingroomids.get(k));
					ArrayList ids = (ArrayList)tempMap.get("ids");
					ArrayList beginDates = (ArrayList)tempMap.get("beginDates");
					ArrayList endDates = (ArrayList)tempMap.get("endDates");
					ArrayList names = (ArrayList)tempMap.get("names");
					ArrayList totalmembers = (ArrayList)tempMap.get("totalmembers");
					ArrayList begintimes = (ArrayList)tempMap.get("begintimes");
					ArrayList callers = (ArrayList)tempMap.get("callers");
					ArrayList endtimes = (ArrayList)tempMap.get("endtimes");
					ArrayList contacters = (ArrayList)tempMap.get("contacters");
					ArrayList cancels = (ArrayList)tempMap.get("cancels");
					ArrayList meetingStatuss = (ArrayList)tempMap.get("meetingStatus");
					ArrayList meetingTotalMembers = (ArrayList)tempMap.get("meetingTotalMembers");

					List dayList = new ArrayList();
					for(int j=meetingSetInfo.getTimeRangeStart()*dspUnit; j<(meetingSetInfo.getTimeRangeEnd()+1)*dspUnit; j++)
					{
						Map dateMap=new HashMap();
						String bgcolor="";
						String fontcolor="";
						List tdTitle = new ArrayList();

						String time=(dspUnit==1?(Util.add0(j,2)+":00"):getTimesBg(j-1,dspUnit));
						String tempTimeBg = currentdate+" "+(dspUnit==1?(Util.add0(j,2)+":00"):getTimesBg(j-1,dspUnit)) ;
						String tempTimeed = currentdate+" "+(dspUnit==1?(Util.add0(j,2)+":59"):getTimesEd(j,dspUnit)) ;

						String temp=getHourOccupied(currentdate, ""+j, beginDates, begintimes, endDates, endtimes, cancels,dspUnit);
						if("2".equals(temp))
						{
							//tdTitle = SystemEnv.getHtmlLabelName(82890,user.getLanguage()) ;
						}

						boolean existDSP=false;//待审批
						int cnt = 0;
						List meetingList=new ArrayList();
						for (int h=0 ;h<ids.size();h++)
						{
							String beginDate = (String)beginDates.get(h);
							String endDate = (String)endDates.get(h);

							String name = (String)names.get(h);
							String totalmember = (String)totalmembers.get(h);
							String caller = (String)callers.get(h);
							String contacter = (String)contacters.get(h);
							String begintime = (String)begintimes.get(h);
							String endtime = (String)endtimes.get(h);
							String cancel = (String)cancels.get(h);
							String meetingStatus = (String)meetingStatuss.get(h);
							if(cancel.equals("1"))continue;

							String tempBeginDateTime = beginDate+" "+begintime;
							String tempEndDateTime = endDate+" "+endtime;

							if((tempTimeed).compareTo(tempBeginDateTime)>=0&& (tempTimeBg).compareTo(tempEndDateTime)<0)
							{
								if("1".equals(meetingStatus)){
									existDSP=true;
								}
								cnt++;
//								RecordSet rs = new RecordSet();
								String total = (String)meetingTotalMembers.get(h);
//								String id = (String)ids.get(h);
//								rs.executeQuery("select totalmember from meeting where id = ?",id);
//								while(rs.next()){
//									total = rs.getString("totalmember");
//								}
								if(Util.getIntValue(total) > Util.getIntValue(totalmember)){
									totalmember = total;
								}
								tdTitle.add(getMeetRoomTitle(name,totalmember,caller,contacter,beginDate,endDate,begintime,endtime,user));
								Map meetingMap=new HashMap();
								meetingMap.put("id", ids.get(h));
								meetingMap.put("name", name);
								meetingList.add(meetingMap);
							}
						}
						if("2".equals(temp))
						{
							bgcolor="#"+conflictedColor;
							fontcolor=conflictedColorFont;
						}
						else if("1".equals(temp))
						{
							bgcolor="#"+usedColor;
							fontcolor=usedColorFont;
							if(existDSP){
								bgcolor="#"+agreementColor;
								fontcolor=agreementColorFont;
							}
						}
						if(meetingList.size()>0){
							dateMap.put("time", time);
							dateMap.put("fontcolor", fontcolor);
							dateMap.put("bgcolor", bgcolor);
							dateMap.put("meetings", meetingList);
							dateMap.put("title", tdTitle);
							dateMap.put("content", cnt);
							dayList.add(dateMap);
						}
					}
					if(dayList.size()>0){
						Map map=new HashMap();
						map.put("roomid", tmproomid);
						map.put("info", dayList);
						list.add(map);
					}
				}
				apidatas.put("datas", list);
				apidatas.put("dspUnit", dspUnit);
			}
			apidatas.put("bywhat", bywhat);
			apidatas.put("rooms", roomInfoList);
		}catch(Exception e){
			e.printStackTrace();
		}

		return apidatas;
	}
	/**
	 * 获取会议室title
	 * @param name
	 * @param totalmember
	 * @param caller
	 * @param contacter
	 * @param beginDate
	 * @param endDate
	 * @param begintime
	 * @param endtime
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public List getMeetRoomTitle(String name,String totalmember,String caller,String contacter,String beginDate,
								 String endDate,String begintime,String endtime,User user) {
		List retList = new ArrayList();
		try{
			String returnStr = "" ;
			MeetingFieldComInfo mfc=new MeetingFieldComInfo();
			ResourceComInfo rc=new ResourceComInfo();
			retList.add(SystemEnv.getHtmlLabelName(Util.getIntValue(mfc.getLabel("2")),user.getLanguage())+":" + name);
			retList.add(SystemEnv.getHtmlLabelName(Util.getIntValue(mfc.getLabel("31")),user.getLanguage())+":" + totalmember);
			retList.add(SystemEnv.getHtmlLabelName(Util.getIntValue(mfc.getLabel("3")),user.getLanguage())+":" + rc.getResourcename(caller));
			MeetingUtil meetingUtil = new MeetingUtil();
			boolean isuesCon = meetingUtil.isUse("contacter");
			if(isuesCon) {
				retList.add(SystemEnv.getHtmlLabelName(Util.getIntValue(mfc.getLabel("4")),user.getLanguage())+":"+ rc.getResourcename(contacter));
			}
			retList.add(SystemEnv.getHtmlLabelName(Util.getIntValue(mfc.getLabel("18")),user.getLanguage())+":" + beginDate+" "+begintime);
			retList.add(SystemEnv.getHtmlLabelName(Util.getIntValue(mfc.getLabel("20")),user.getLanguage())+":"+ endDate+" "+endtime);
		}catch (Exception e){
			e.printStackTrace();
		}
		return retList ;
	}

	/**
	 * 获取会议室title
	 * @param key
	 * @param mr
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public List getMeetRoomTitle(String key,MeetingRoomComInfo mr,User user){
		List retList = new ArrayList();
		try{
			String msg = "";
			RecordSet recordSet = new RecordSet();
			recordSet.executeQuery("select * from meetingroom where id = ?",key);
			recordSet.next();
			int allowMinNum =Util.getIntValue(recordSet.getString("allowMinNum"),0);
			int allowMaxNum = Util.getIntValue(recordSet.getString("allowMaxNum"),0);
			String mycity = Util.null2String(recordSet.getString("mycity"));
			String mybuilding = Util.null2String(recordSet.getString("mybuilding"));
			String myfloor = Util.null2String(recordSet.getString("myfloor"));
			if(allowMaxNum>0 || allowMinNum >0){
				msg = allowMinNum+"-"+allowMaxNum+SystemEnv.getHtmlLabelName(127,user.getLanguage());
			}
            CityComInfo cityComInfo = new CityComInfo();
			ResourceComInfo rc=new ResourceComInfo();
			if(StringUtils.isNotBlank(mr.getMeetingRoomInfoname(key))){
				retList.add((SystemEnv.getHtmlLabelName(780, user.getLanguage())+(user.getLanguage()==8?" ":"")+SystemEnv.getHtmlLabelName(195, user.getLanguage()))+":" + mr.getMeetingRoomInfoname(key));
			}
			if(StringUtils.isNotBlank(mr.getMeetingRoomInfodesc(key))){
				retList.add((SystemEnv.getHtmlLabelName(780, user.getLanguage())+(user.getLanguage()==8?" ":"")+SystemEnv.getHtmlLabelName(433, user.getLanguage()))+":" + mr.getMeetingRoomInfodesc(key));
			}
			if(StringUtils.isNotBlank(rc.getLastnames(mr.getMeetingRoomInfohrmids(key)))){
				retList.add(SystemEnv.getHtmlLabelName(2156, user.getLanguage())+":" + rc.getLastnames(mr.getMeetingRoomInfohrmids(key)));
			}
			if(StringUtils.isNotBlank(mr.getMeetingRoomInfoequipment(key))){
				retList.add((SystemEnv.getHtmlLabelName(780, user.getLanguage())+(user.getLanguage()==8?" ":"")+SystemEnv.getHtmlLabelName(1326, user.getLanguage()))+":" +mr.getMeetingRoomInfoequipment(key));
			}
			if(StringUtils.isNotBlank(msg)){
				retList.add(SystemEnv.getHtmlLabelName(30138, user.getLanguage())+":" +msg);
			}

			if(StringUtils.isNotBlank(mycity) && Util.getIntValue(mycity,0)>0){
				//==zj  会议日历预览 用建模表 来获取城市名称
				RecordSet rs = new RecordSet();
				String tableName = new BaseBean().getPropValue("qc2085194","tablename");
				String csm = "";
				String sql = "select * from "+tableName+" where id = "+mycity;
				new BaseBean().writeLog("==zj==(会议预览sql)"+sql);
				rs.executeQuery(sql);
				if (rs.next()){
					csm = rs.getString("csm");
				}

			    retList.add(SystemEnv.getHtmlLabelName(	528825,user.getLanguage())+":"+csm/*cityComInfo.getCityname(mycity)*/);
            }
            if(StringUtils.isNotBlank(mybuilding)){
                retList.add(SystemEnv.getHtmlLabelName(	528827,user.getLanguage())+":"+mybuilding);
            }
            if(StringUtils.isNotBlank(myfloor)){
                retList.add(SystemEnv.getHtmlLabelName(	528826,user.getLanguage())+":"+myfloor);
            }
		}catch(Exception e){
			e.printStackTrace();
		}
		return retList ;
	}

	/**
	 * 指定日期的占用情况
	 * @param thisDate
	 * @param beginDateList
	 * @param beginTimeList
	 * @param endDateList
	 * @param endTimeList
	 * @param cancelList
	 * @return
	 */
	public String getDayOccupied(String thisDate, List beginDateList, List beginTimeList, List endDateList, List endTimeList, List cancelList)
	{
		String[] minute = new String[24 * 60];
		for (int i = 0; i < beginDateList.size(); i++)
		{
			String beginDate = (String)beginDateList.get(i);
			String beginTime = (String)beginTimeList.get(i);
			String endDate = (String)endDateList.get(i);
			String endTime = (String)endTimeList.get(i);
			String cancel = (String)cancelList.get(i);
			if(!"1".equals(cancel) && beginDate.compareTo(thisDate) <= 0 && thisDate.compareTo(endDate) <= 0)
			{
				if(beginDate.compareTo(thisDate) < 0)
				{
					beginTime = "00:00";
				}
				if(thisDate.compareTo(endDate) < 0)
				{
					endTime = "23:59";
				}
				int beginMinuteOfDay = getMinuteOfDay(beginTime)+1;
				int endMinuteOfDay  = getMinuteOfDay(endTime);
				while(beginMinuteOfDay <= endMinuteOfDay)
				{
					if("1".equals(minute[beginMinuteOfDay]))
					{
						return "2";
					}
					else
					{
						minute[beginMinuteOfDay] = "1";
					}
					beginMinuteOfDay++;
				}
			}
		}

		for(int i = 0; i < 24 * 60; i++)
		{
			if("1".equals(minute[i]))
			{
				return "1";
			}
		}
		return "0";
	}

	/**
	 * 指定小时内的占用情况
	 * @param thisDate
	 * @param thisHour
	 * @param beginDateList
	 * @param beginTimeList
	 * @param endDateList
	 * @param endTimeList
	 * @param cancelList
	 * @param dspUnix
	 * @return
	 */
	public String getHourOccupied(String thisDate, String thisHour, List beginDateList, List beginTimeList, List endDateList, List endTimeList, List cancelList,int dspUnix)
	{
		String[] minute = new String[24 * 60];
		String starttime1 = dspUnix==1?((thisHour.length()==1?"0"+thisHour:thisHour)+":00"):getTimesBg(Util.getIntValue(thisHour)-1,dspUnix);
		String endtime1 = dspUnix==1?((thisHour.length()==1?"0"+thisHour:thisHour)+":59"):getTimesEd(Util.getIntValue(thisHour),dspUnix);

		for (int i = 0; i < beginDateList.size(); i++)
		{
			String beginDate = (String)beginDateList.get(i);
			String beginTime = (String)beginTimeList.get(i);
			String endDate = (String)endDateList.get(i);
			String endTime = (String)endTimeList.get(i);
			String cancel = (String)cancelList.get(i);

			if
					(
					!"1".equals(cancel)
							&& (beginDate.compareTo(thisDate) < 0 || (beginDate.compareTo(thisDate) == 0 && beginTime.compareTo(endtime1) <= 0))
							&& (thisDate.compareTo(endDate) < 0 || (thisDate.compareTo(endDate) == 0 && (starttime1).compareTo(endTime) <= 0))
					)
			{
				if(beginDate.compareTo(thisDate) < 0 || beginTime.compareTo(starttime1) < 0)
				{
					beginTime = starttime1;
				}
				if(thisDate.compareTo(endDate) < 0 || (endtime1).compareTo(endTime) < 0)
				{
					endTime = endtime1;
				}

				int beginMinuteOfHour = beginTime.indexOf("59")>0?getMinuteOfDay(beginTime):getMinuteOfDay(beginTime) + 1;
				int endMinuteOfHour  = getMinuteOfDay(endTime);

				while(beginMinuteOfHour <= endMinuteOfHour)
				{
					if("1".equals(minute[beginMinuteOfHour]))
					{
						return "2";
					}
					else
					{
						minute[beginMinuteOfHour] = "1";
					}
					beginMinuteOfHour++;
				}
			}
		}
		for(int i = 0; i < 24 * 60; i++)
		{
			if("1".equals(minute[i]))
			{
				return "1";
			}
		}
		return "0";
	}

	/**
	 *
	 * @param time
	 * @return
	 */
	private int getMinuteOfDay(String time)
	{
		List timeList = Util.TokenizerString(time, ":");
		return (Util.getIntValue((String)timeList.get(0)) * 60 + Util.getIntValue((String)timeList.get(1)));
	}
	/**
	 * 根据时间占用粒度获取所在时间范围起点
	 * @param j
	 * @param dspUnit
	 * @return
	 */
	public String getTimesBg(int j,int dspUnit){
		int totalminutes = (j+1) * (60/dspUnit);
		int hours = (int)totalminutes/60;
		int minute = totalminutes%60;
		String times = (hours > 9 ? (""+hours) :("0"+hours)) +":"+  (minute > 9 ? (""+minute) :("0"+minute));
		return times;
	}
	/**
	 * 根据时间占用粒度获取所在时间范围终点
	 * @param j
	 * @param dspUnit
	 * @return
	 */
	public String getTimesEd(int j,int dspUnit){
		int totalminutes = (j+1) * (60/dspUnit);
		int hours = (int)totalminutes/60;
		int minute = totalminutes%60;
		if(minute==0){
			minute=59;
			hours-=1;
		}else{
			minute-=1;
		}
		String times = (hours > 9 ? (""+hours) :("0"+hours)) +":"+  (minute > 9 ? (""+minute) :("0"+minute));
		return times;
	}
}
