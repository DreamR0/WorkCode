/*
 *
 * Copyright (c) 2001-2016 泛微软件.
 * 泛微协同商务系统,版权所有.
 * 
 */
package weaver.meeting.remind;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.api.workplan.util.TimeZoneCastUtil;
import com.caucho.json.Json;
import com.engine.common.util.ServiceUtil;
import com.engine.meeting.service.MeetingRemindService;
import com.engine.meeting.service.impl.MeetingRemindServiceImpl;
import com.engine.meeting.util.MeetingEncryptUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import weaver.conn.RecordSet;
import weaver.dateformat.UnifiedConversionInterface;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.Maint.MeetingRoomComInfo;
import weaver.meeting.MeetingUtil;
import weaver.meeting.defined.MeetingFieldComInfo;
import weaver.meeting.defined.MeetingFieldManager;

/**
 * @author HuangGuanGuan
 * Jan 21, 2015
 * 会议提醒帮助类
 */
public class MeetingRemindUtil {
	 
	private static IMeetingRemind getRemindObject(String classname){
		IMeetingRemind obj=null;
		if(classname!=null&&!"".equals(classname)){
			try {
				obj=(IMeetingRemind)Class.forName(classname).newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return obj;
	}
	
	/**
	 * 立即提醒
	 * @param meetingid
	 * @param mode
	 * @param touser
	 */
	public static void remindImmediately(String meetingid,String mode,String touser){

		try {
			Date nowdate = new Date() ;
			ResourceComInfo rci = new ResourceComInfo();
			MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			String nowTimeStr=sdf.format(nowdate);

			//支持无侵入修改提醒业务
			String addUsrIds="";//增加的人员
			MeetingRemindService remindService=getService();
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("meetingId",meetingid);
			params.put("mode",mode);
			params.put("touser",touser);
			params.put("nowTimeStr",nowTimeStr);
			new BaseBean().writeLog("==zj==(立即提醒)" + JSON.toJSONString(params));
			Map<String, Object> retMap=remindService.beforeRemind(params);
			if(null!=retMap){
				String executeStandardBusiness=Util.null2String(retMap.get("executeStandardBusiness"));

				if(!"true".equals(executeStandardBusiness)){//不执行标准业务
					return;
				}

				mode=Util.null2String(retMap.get("mode"));
				touser=Util.null2String(retMap.get("touser"));//替换的人员
				addUsrIds=Util.null2String(retMap.get("addUsrIds"));//增加的人员
				nowTimeStr=Util.null2String(retMap.get("nowTimeStr"));
			}

			if(mode==null||"".equals(mode)) mode="create";
			if(meetingid!=null&&!"".equals(meetingid)){
				RecordSet rs=new RecordSet();
				MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
				//正常提醒时,会议状态是正常状态
				rs.execute("select * from meeting where  (cancel <> '1' or cancel is null) and meetingstatus = 2 and id="+meetingid);
				if(rs.next()){
					String caller = rs.getString("caller");
					String contacter = rs.getString("contacter");
					String createrid = rs.getString("creater");
					String beginDateTime = rs.getString("beginDate") +" " + rs.getString("beginTime");
					String endDateTime = rs.getString("endDate") +" " + rs.getString("endTime");
					String name = rs.getString("name");
					String meetingType= rs.getString("meetingType");
					String createrDateTime = rs.getString("createdate")+" "+rs.getString("createtime");
					String desc = rs.getString("desc_n");
					String content = rs.getString("description");
					String address = rs.getString("address").equals("")?rs.getString("customizeAddress"):
							meetingRoomComInfo.getMeetingRoomInfoname(rs.getString("address"));
					address = Util.formatMultiLang(address);
					int requestid = rs.getInt("requestid");
					String uuid = rs.getString("uuid");
					if("start".equals(mode)){
						String tmp=rs.getString("begindate")+" "+rs.getString("begintime");
						if(nowTimeStr.compareTo(tmp)>0){
							return;
						}
					}else if("end".equals(mode)){
						String tmp=rs.getString("enddate")+" "+rs.getString("endtime");
						if(nowTimeStr.compareTo(tmp)>0){
							return;
						}
					}
					String remindType=rs.getString("remindTypeNew");
					Map meetingInfo = new HashMap();
					RecordSet rs1=new RecordSet();
					RecordSet rs2=new RecordSet();
					MeetingEncryptUtil.setDecryptData2DaoInfo(rs1,rs2);
					IMeetingRemind  remind=null;
					Set<String> hrmids=new LinkedHashSet<String>();
					String removeHrm = "";
					if(touser==null||"".equals(touser)){//没有传入发送者
						touser="";
						hrmids = (Set<String>) getReminders(meetingid,mode,requestid).get("reminderSet");
						removeHrm = Util.null2String(getReminders(meetingid,mode,requestid).get("removeHrm"));
					}else{
						StringTokenizer sthrmid = new StringTokenizer(touser, ",");
				        while (sthrmid.hasMoreTokens()) {
				            String id = sthrmid.nextToken();
				            if(id!=null&&!"".equals(id)){
				            	hrmids.add(id);
				            }
				        }
					}

					if(!"".equals(addUsrIds)){
						StringTokenizer sthrmid = new StringTokenizer(addUsrIds, ",");
						while (sthrmid.hasMoreTokens()) {
							String id = sthrmid.nextToken();
							if(id!=null&&!"".equals(id)){
								hrmids.add(id);
							}
						}
					}

					String currentRemindType="";
					//处理前后逗号,防止异常
					if(remindType.startsWith(",")){
						remindType=remindType.substring(1);
					}
					if(remindType.endsWith(",")){
						remindType=remindType.substring(0,remindType.length()-1);
					}
					if(!"".equals(remindType)){
						UnifiedConversionInterface uci = new UnifiedConversionInterface();
						boolean needTimeZone = uci.getTimeZoneStatus();
						//判断分权
						ManageDetachComInfo manageDetachComInfo=new ManageDetachComInfo();
						//是否开启会议分权
						boolean detachable=manageDetachComInfo.isUseMtiManageDetach();
						int subId = Util.getIntValue(new ResourceComInfo(true).getSubCompanyID(createrid)) ;
						boolean openCustomSet = false;
						rs1.executeQuery(" select isOpen from meeting_remind_detachBaseInfo where subcompanyid = ?",subId);
						if(rs1.next()){
							openCustomSet = rs1.getInt(1) == 1;
						}
						//额外参数
						Map<String, String> map=new HashMap<String, String>();
						map.put("mode", mode);
						map.put("meetingid", meetingid);
						map.put("caller", caller);
						map.put("beginDateTime", beginDateTime);
						map.put("endDateTime", endDateTime);
						map.put("beginDateTime4Mail", beginDateTime);
						map.put("endDateTime4Mail", endDateTime);
						map.put("name",name);
						map.put("creater", rci.getLastname(createrid));
						map.put("createrid", createrid);
						map.put("meetingType", meetingType);
						map.put("createrDateTime", createrDateTime);
						map.put("desc", desc);
						map.put("content", content);
						map.put("address", address);
						map.put("removeHrm", removeHrm);
						if(uuid.equals("")){
							uuid = UUID.randomUUID().toString();
							rs1.executeUpdate("update meeting set uuid = ? where id = ?",uuid,meetingid);
						}
						map.put("uuid", uuid);
						if("cancel".equals(mode)){
							map.put("method", "CANCEL");
						}else{
							map.put("method", "REQUEST");
						}
						rs1.execute("select * from meeting_remind_type where id in("+remindType+")");
						MeetingFieldManager mfm=new MeetingFieldManager(1); 
						List<String> templateList = mfm.getTemplateField();
						MeetingFieldComInfo mfComInfo=new MeetingFieldComInfo();
						while(rs1.next()){
							String title="";
							String msg="";
							boolean hastitle="1".equals(rs1.getString("hastitle"));
							currentRemindType=rs1.getString("id");
							remind=getRemindObject(rs1.getString("clazzname"));
							String type = "";
							if(remind!=null){
								new BaseBean().writeLog("==zj==(查询提醒模板)" + meetingType + " | " + remindType + " | " + mode);
								//qc2452784，如果该会议有选择会议类型，且是邮件提醒已经创建会议，就优先走自定义的提醒模板
								if (!"".equals(meetingType) && "3".equals(remindType) && "create".equals(mode)){
									String tableName = new BaseBean().getPropValue("qc2452784", "tableName");
									Boolean isMeeting = false;
									if(detachable && openCustomSet){
										RecordSet rs3 = new RecordSet();
										String sql = "select title,body,type from "+tableName+" where type=" + currentRemindType + " and modetype=" +mode+ " and subcompanyid = "+subId+" and meetingtype = "+meetingType;
										new BaseBean().writeLog("==zj==(查询提醒模板sql)" + sql);
										rs3.executeQuery(sql);
										if (rs3.next()){
											 isMeeting = true;
										 }
									}else{
										RecordSet rs3 = new RecordSet();
										String sql = "select title,body,type from "+tableName+" where type=" + currentRemindType + " and modetype=" +mode+ " and subcompanyid = "+subId+" and meetingtype = "+meetingType;
										new BaseBean().writeLog("==zj==(查询提醒模板sql)" + sql);
										rs3.executeQuery(sql);
										if (rs3.next()){
											isMeeting = true;
										}
									}
									new BaseBean().writeLog("==zj==(isMeeting是否有该会议类型模板)" + isMeeting);
									if (!isMeeting){
										//说明该会议类型没有自定义的提醒模板，就走默认的提醒模板
										if(detachable && openCustomSet){
											rs2.executeQuery("select title,body,type from meeting_remind_template where type=? and modetype=? and subcompanyid = ?",currentRemindType,mode,subId);
										}else{
											rs2.executeQuery("select title,body,type from meeting_remind_template where type=? and modetype=? and subcompanyid = 0",currentRemindType,mode);
										}
									}

								}else{
									//会议创建如果没有选择会议类型，就走默认的提醒模板
									if(detachable && openCustomSet){
										rs2.executeQuery("select title,body,type from meeting_remind_template where type=? and modetype=? and subcompanyid = ?",currentRemindType,mode,subId);
									}else{
										rs2.executeQuery("select title,body,type from meeting_remind_template where type=? and modetype=? and subcompanyid = 0",currentRemindType,mode);
									}
								}


								if(rs2.next()){
									title=rs2.getString("title");
									msg=rs2.getString("body");
									type=rs2.getString("type");
								}else{
									if(!"create".equals(mode)){
										if(detachable && openCustomSet){
											rs2.executeQuery("select title,body from meeting_remind_template where type=? and modetype='create' and subcompanyid = ?",currentRemindType,subId);
										}else{
											rs2.executeQuery("select title,body from meeting_remind_template where type=? and modetype='create' and subcompanyid = 0",currentRemindType);
										}

										if(rs2.next()){
											title=rs2.getString("title");
											msg=rs2.getString("body");

											new BaseBean().writeLog("==zj==(title和body)" + title + " | " + msg);
										}
									}
								}
								if(!"".equals(msg)){//找到模板
									if(needTimeZone && (msg.indexOf("#[begindate] #[begintime]") > -1 ||msg.indexOf("#[begindate]#[begintime]") > -1
											|| msg.indexOf("#[enddate] #[endtime]") > -1|| msg.indexOf("#[enddate]#[endtime]") > -1) && TimeZoneCastUtil.canCastZone){
										needTimeZone = true;
									}else{
										needTimeZone = false;
									}
									// 获取提醒参数参数，用于模板短信发送获取会议参数
									for(String fieldid:templateList){
										String fieldname = mfComInfo.getFieldname(fieldid);
										String fieldValue = rs.getString(fieldname);
										int fieldHtmlType = Util.getIntValue(mfComInfo.getFieldhtmltype(fieldid));
										int fieldType = Util.getIntValue(mfComInfo.getFieldType(fieldid));
										fieldValue=mfm.getRemindFieldvalue(Util.getIntValue(fieldid),fieldHtmlType, fieldType,fieldValue,7);
										meetingInfo.put(fieldname,fieldValue);
									}
									//替换模板参数
									for(String fieldid:templateList){
										String fieldname = mfComInfo.getFieldname(fieldid);
										String fieldValue = rs.getString(fieldname);
										int fieldHtmlType = Util.getIntValue(mfComInfo.getFieldhtmltype(fieldid));
										int fieldType = Util.getIntValue(mfComInfo.getFieldType(fieldid));
										fieldValue=mfm.getRemindFieldvalue(Util.getIntValue(fieldid),fieldHtmlType, fieldType,fieldValue,7);
										if(needTimeZone && (fieldname.equalsIgnoreCase("begintime") || fieldname.equalsIgnoreCase("endtime") )){
											fieldValue += "(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")";
											map.put("beginDateTime", beginDateTime+"(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")");
											map.put("endDateTime", endDateTime+"(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")");
										}
										msg=msg.replace("#["+fieldname+"]", fieldValue);
										if(hastitle){
											title=title.replace("#["+fieldname+"]", fieldValue);
										}
										if(msg.indexOf("#[")==-1 && title.indexOf("#[")==-1) break; //没有参数时提前结束循环
									}
									//消息中心支持多语言,所以将title数据库中置空来添加多语言标签
									if(type.equals("6") && title.equals("")){
										if(mode.equals("create")){
											map.put("title","388940");
										}else if(mode.equals("start")){
											map.put("title","388941");
										}else if(mode.equals("end")){
											map.put("title","388942");
										}else if(mode.equals("cancel")){
											map.put("title","390433");
										}else if(mode.equals("change")){
											map.put("title","127682");
										}

									}
									if(!type.equals("6")){
										msg = Util.formatMultiLang(msg);
										title = Util.formatMultiLang(title);
									}
									if(rs1.getString("clazzname").indexOf("RemindMail") > -1){
										msg = Util.null2String(msg).replace("<br/>", "<br/>").replace("＜br/＞", "<br/>")
												.replace("＜br＞","<br/>").replace("\n","<br/>");
									}else{
										msg = Util.null2String(msg).replace("＜br/＞", "<br/>").replace("＜br＞", "<br>");
									}
									hrmids = getNormalHrmids(hrmids);
									if(type.equals("2")){
										map.putAll(meetingInfo);
									}
									remind.sendRemind(hrmids,title, msg,map);
								}
							}
						}
					}
				}	
			}
		} catch (Exception e) {
			 
		}
	}
	/**
	 * 去除人员状态大于等于4的人员，4：解聘5：离职6：退休7：无效
	 * @param hrmids
	 */
	public static Set<String> getNormalHrmids(Set<String> hrmids){
		try {
			ResourceComInfo rci = new ResourceComInfo();
			Iterator<String> it = hrmids.iterator();
			for (int i = 0;i < hrmids.size();i++) {
				String hrmid = it.next();
				if(Util.getIntValue(rci.getStatus(hrmid)) > 3){
					it.remove();
					i--;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hrmids;
	}
	
	/**
	 * 用来向定时提醒表插入数据
	 * @param meetingid
	 * @param time
	 * @param mode
	 */
	public static void remindAtTime(String meetingid,String time,String mode){
		RecordSet rs=new RecordSet();
		rs.execute("insert into meeting_remind(meeting,remindTime,modetype) values("+meetingid+",'"+time+"','"+mode+"')");
	}

	/**
	 * 取消会议时的立即提醒
	 * @param meetingid
	 */
	public static void cancelMeeting(String meetingid){
		try {
			if(meetingid!=null&&!"".equals(meetingid)){

				//支持无侵入修改提醒业务
				MeetingRemindService remindService=getService();
				Map<String, Object> params = new HashMap<String, Object>();
				String touser="";
				String addUsrIds="";//增加的人员
				params.put("meetingId",meetingid);
				params.put("mode","cancel");
				params.put("touser",touser);
				Map<String, Object> retMap=remindService.beforeRemind(params);
				if(null!=retMap){
					String executeStandardBusiness=Util.null2String(retMap.get("executeStandardBusiness"));

					if(!"true".equals(executeStandardBusiness)){//不执行标准业务
						return;
					}

					touser=Util.null2String(retMap.get("touser"));
					addUsrIds=Util.null2String(retMap.get("addUsrIds"));

				}

				ResourceComInfo rci = new ResourceComInfo();
				MeetingRoomComInfo meetingRoomComInfo = new MeetingRoomComInfo();
				RecordSet rs=new RecordSet();
				Map meetingInfo = new HashMap();
				MeetingEncryptUtil.setDecryptData2DaoInfo(rs);
				//正常提醒时,会议状态是正常状态
				rs.execute("select * from meeting where  cancel = '1' and meetingstatus = 4 and id="+meetingid);
				if(rs.next()){
					String caller = rs.getString("caller");
					String contacter = rs.getString("contacter");
					String createrid = rs.getString("creater");
					String beginDateTime = rs.getString("beginDate") +" " + rs.getString("beginTime");
					String endDateTime = rs.getString("endDate") +" " + rs.getString("endTime");
					String name = rs.getString("name");
					String remindType=rs.getString("remindTypeNew");
					String meetingType= rs.getString("meetingType");
					String createrDateTime = rs.getString("createdate")+" "+rs.getString("createtime");
					String desc = rs.getString("desc_n");
					String content = rs.getString("description");
					String address = rs.getString("address").equals("")?rs.getString("customizeAddress"):
							meetingRoomComInfo.getMeetingRoomInfoname(rs.getString("address"));
					address = Util.formatMultiLang(address);
					int requestid = rs.getInt("requestid");
					String uuid = rs.getString("uuid");
					RecordSet rs1=new RecordSet();
					RecordSet rs2=new RecordSet();
					MeetingEncryptUtil.setDecryptData2DaoInfo(rs1,rs2);
					IMeetingRemind  remind=null;
					Set<String> hrmids=new LinkedHashSet<String>();
					if(touser==null||"".equals(touser)){//没有传入发送者
						touser="";
						hrmids = (Set<String>) getReminders(meetingid,"cancel",requestid).get("reminderSet");

					}else{
						StringTokenizer sthrmid = new StringTokenizer(touser, ",");
						while (sthrmid.hasMoreTokens()) {
							String id = sthrmid.nextToken();
							if(id!=null&&!"".equals(id)){
								hrmids.add(id);
							}
						}
					}

					if(!"".equals(addUsrIds)){
						StringTokenizer sthrmid = new StringTokenizer(addUsrIds, ",");
						while (sthrmid.hasMoreTokens()) {
							String id = sthrmid.nextToken();
							if(id!=null&&!"".equals(id)){
								hrmids.add(id);
							}
						}
					}

					String currentRemindType="";
					//处理前后逗号,防止异常
					if(remindType.startsWith(",")){
						remindType=remindType.substring(1);
					}
					if(remindType.endsWith(",")){
						remindType=remindType.substring(0,remindType.length()-1);
					}
					if(!"".equals(remindType)){
						//判断分权
						ManageDetachComInfo manageDetachComInfo=new ManageDetachComInfo();
						//是否开启会议分权
						boolean detachable=manageDetachComInfo.isUseMtiManageDetach();
						int subId = Util.getIntValue(new ResourceComInfo(true).getSubCompanyID(createrid)) ;
						boolean openCustomSet = false;
						rs1.executeQuery(" select isOpen from meeting_remind_detachBaseInfo where subcompanyid = ?",subId);
						if(rs1.next()){
							openCustomSet = rs1.getInt(1) == 1;
						}
						//额外参数
						Map<String, String> map=new HashMap<String, String>();
						map.put("meetingid", meetingid);
						map.put("caller", caller);
						map.put("beginDateTime", beginDateTime);
						map.put("endDateTime", endDateTime);
						map.put("beginDateTime4Mail", beginDateTime);
						map.put("endDateTime4Mail", endDateTime);
						map.put("name",name);
						map.put("creater", rci.getLastname(createrid));
						map.put("createrid", createrid);
						map.put("meetingType", meetingType);
						map.put("createrDateTime", createrDateTime);
						map.put("desc", desc);
						map.put("content", content);
						map.put("address", address);
						map.put("mode", "cancel");
						if(uuid.equals("")){
							uuid = UUID.randomUUID().toString();
							rs1.executeUpdate("update meeting set uuid = ? where id = ?",uuid,meetingid);
						}
						map.put("uuid", uuid);
						map.put("method", "CANCEL");
						rs1.execute("select * from meeting_remind_type where id in("+remindType+")");
						MeetingFieldManager mfm=new MeetingFieldManager(1); 
						List<String> templateList = mfm.getTemplateField();
						MeetingFieldComInfo mfComInfo=new MeetingFieldComInfo();
						while(rs1.next()){
							String title="";
							String msg="";
							String type = "";
							boolean hastitle="1".equals(rs1.getString("hastitle"));
							currentRemindType=rs1.getString("id");
							remind=getRemindObject(rs1.getString("clazzname"));
							if(remind!=null){
								if(detachable && openCustomSet){
									rs2.executeQuery("select title,body,type from meeting_remind_template where type=? and modetype='cancel' and subcompanyid = ?",currentRemindType,subId);
								}else{
									rs2.executeQuery("select title,body,type from meeting_remind_template where type=? and modetype='cancel' and subcompanyid = 0",currentRemindType);
								}
								if(rs2.next()){
									title=rs2.getString("title");
									msg=rs2.getString("body");
									type=rs2.getString("type");
								}
								if(!"".equals(msg)){//找到模板
									UnifiedConversionInterface uci = new UnifiedConversionInterface();
									boolean needTimeZone = uci.getTimeZoneStatus();
									if(needTimeZone && (msg.indexOf("#[begindate] #[begintime]") > -1 ||msg.indexOf("#[begindate]#[begintime]") > -1
											|| msg.indexOf("#[enddate] #[endtime]") > -1|| msg.indexOf("#[enddate]#[endtime]") > -1) && TimeZoneCastUtil.canCastZone){
										needTimeZone = true;
									}else{
										needTimeZone = false;
									}
									// 获取提醒参数参数，用于模板短信发送获取会议参数
									for(String fieldid:templateList){
										String fieldname = mfComInfo.getFieldname(fieldid);
										String fieldValue = rs.getString(fieldname);
										int fieldHtmlType = Util.getIntValue(mfComInfo.getFieldhtmltype(fieldid));
										int fieldType = Util.getIntValue(mfComInfo.getFieldType(fieldid));
										fieldValue=mfm.getRemindFieldvalue(Util.getIntValue(fieldid),fieldHtmlType, fieldType,fieldValue,7);
										meetingInfo.put(fieldname,fieldValue);
									}
									//替换模板参数
									for(String fieldid:templateList){
										String fieldname = mfComInfo.getFieldname(fieldid);
										String fieldValue = rs.getString(fieldname);
										int fieldHtmlType = Util.getIntValue(mfComInfo.getFieldhtmltype(fieldid));
										int fieldType = Util.getIntValue(mfComInfo.getFieldType(fieldid));
										fieldValue=mfm.getRemindFieldvalue(Util.getIntValue(fieldid),fieldHtmlType, fieldType,fieldValue,7);
										if(needTimeZone && (fieldname.equalsIgnoreCase("begintime") || fieldname.equalsIgnoreCase("endtime") )){
											fieldValue += "(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")";
											map.put("beginDateTime", beginDateTime+"(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")");
											map.put("endDateTime", endDateTime+"(GMT"+ DateFormatUtils.format(new Date(), "ZZ") +")");
										}
										msg=msg.replace("#["+fieldname+"]", fieldValue);
										if(hastitle){
											title=title.replace("#["+fieldname+"]", fieldValue);
										}
										if(msg.indexOf("#[")==-1 && title.indexOf("#[")==-1) break; //没有参数时提前结束循环
									}
									//消息中心支持多语言,所以将title数据库中置空来添加多语言标签
									if(type.equals("6") && title.equals("")){
										map.put("title","390433");
									}
									if(!type.equals("6")){
										msg = Util.formatMultiLang(msg);
										title = Util.formatMultiLang(title);
									}
									if(rs1.getString("clazzname").indexOf("RemindMail") > -1){
										msg = Util.null2String(msg).replace("<br>", "<br/>").replace("＜br/＞", "<br/>")
												.replace("＜br＞","<br/>").replace("\n","<br/>");
									}else{
										msg = Util.null2String(msg).replace("＜br/＞", "<br/>").replace("＜br＞", "<br>");
									}
									if(type.equals("2")) {
										map.putAll(meetingInfo);
									}
									hrmids = getNormalHrmids(hrmids);
									remind.sendRemind(hrmids,title, msg,map);
								}
							}
						}
					}
				}	
			}
		} catch (Exception e) {
			 e.printStackTrace();
		}
	}
	
	/**
	 * 回执提醒其他参会人员
	 * @param meetingid
	 * @param touser 必须指定其他人员
	 */
	public static void remindReceipt(String meetingid,String touser){
		if(touser!=null&&!"".equals(touser)){
			remindImmediately(meetingid, null, touser);
		}
	}

	/**
	 * 获取会议提醒服务
	 * @return
	 */
	private static MeetingRemindService getService() {
		return (MeetingRemindServiceImpl) ServiceUtil.getService(MeetingRemindServiceImpl.class, null);
	}

	/**
	 * 获取会议提醒服务
	 * @return
	 */
	private static MeetingRemindService getService(User user) {
		return (MeetingRemindServiceImpl) ServiceUtil.getService(MeetingRemindServiceImpl.class, user);
	}

	/**
	 * 获取无侵入流程提醒数据
	 * @param type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）5：变更
	 * @param meetingId
	 * @param wfTitle 流程标题
	 * @param wfCreater 流程创建人
	 * @param wfRemark 流程签字意见
	 * @param wfAccepter 流程接收人
	 * @param wfSecretLevel 流程密级
	 * @return
	 */
	public static Map<String,Object> beforeWfRemind(int type,String meetingId,String wfTitle,String wfCreater,String wfRemark,String wfAccepter,String wfSecretLevel) {
		return beforeWfRemind(type,meetingId,wfTitle,wfCreater,wfRemark,wfAccepter,wfSecretLevel,null);
	}
	/**
	 * 获取无侵入流程提醒数据
	 * @param type 1:创建提醒 2：服务提醒 3:会议取消提醒（发送给参会人员，包括不是创建人的召集人和联系人） 4：会议取消提醒（发送给服务人员）5：变更
	 * @param meetingId
	 * @param wfTitle 流程标题
	 * @param wfCreater 流程创建人
	 * @param wfRemark 流程签字意见
	 * @param wfAccepter 流程接收人
	 * @param wfSecretLevel 流程密级
	 * @param user 操作者
	 * @return
	 */
	public static Map<String,Object> beforeWfRemind(int type,String meetingId,String wfTitle,String wfCreater,String wfRemark,String wfAccepter,String wfSecretLevel,User user) {

		Map<String,Object> params=new HashMap<String, Object>();

		params.put("wfTitle",wfTitle);
		params.put("wfCreater",wfCreater);
		params.put("wfAccepter",wfAccepter);
		params.put("wfSecretLevel",wfSecretLevel);
		params.put("meetingId",meetingId);
		params.put("wfRemark",wfRemark);
		params.put("type",type);

		MeetingRemindService meetingRemindService = getService(user);

		Map<String,Object> result=null;

		try{
			result=meetingRemindService.beforeWfRemind(params);
		}catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 获取要发送的提醒人员
	 * @param meetingid
	 * @param mode
	 * @return
	 */
	public static Map<String,Object> getReminders(String meetingid, String mode,int requestid){
		Map<String,Object> reminderMap = new HashMap<>();
		Set<String> reminderSet = new LinkedHashSet<String>();
		//获取所有开启提醒的fieldid
		RecordSet rs = new RecordSet();
		Set fieldIdsSet = new HashSet();
		//获取fieldid和reminder
		SetRemindAndFields(meetingid,fieldIdsSet,reminderSet);
		String creater = "";
		String removeHrm = "";
		//处理会议信息字段
		setReminderSet(meetingid,reminderSet,fieldIdsSet,1);
		//处理会议服务字段
		setReminderSet(meetingid,reminderSet,fieldIdsSet,3);
		//处理会议议程字段
		setReminderSet(meetingid,reminderSet,fieldIdsSet,2);
		//在立即提醒的情况下，将创建人过滤掉
		if(mode.equals("create") && requestid < 1){
			rs.executeQuery("select creater from meeting where id = ?",meetingid);
			if(rs.next()) {
				creater = rs.getString("creater");
				if(reminderSet.contains(creater)) {
					removeHrm = creater;
				}
				reminderSet.remove(creater);
			}
		}

		reminderMap.put("reminderSet",reminderSet);
		reminderMap.put("removeHrm",removeHrm);

		return reminderMap;
	}

	/**
	 * 设置fieldIdsSet和reminderSet
	 * @param meetingid
	 * @param fieldIdsSet
	 * @param reminderSet
	 */
	public static void SetRemindAndFields(String meetingid,Set fieldIdsSet, Set reminderSet ){
		SetRemindAndFields(meetingid,fieldIdsSet,reminderSet,false);
	}

	/**
	 * @param meetingid
	 * @param fieldIdsSet
	 * @param reminderSet
	 * @param forShare 是否共享时取值，以及在会议室负责人单独发送时发送提醒
	 */
	public static void SetRemindAndFields(String meetingid,Set fieldIdsSet, Set reminderSet ,boolean forShare){
		RecordSet rs = new RecordSet();
		// 分权时取会议的创建人分部所设置的提醒设置
		// 判断分权
		ManageDetachComInfo manageDetachComInfo=new ManageDetachComInfo();
		// 是否开启会议分权
		boolean detachable=manageDetachComInfo.isUseMtiManageDetach();
		RecordSet recordSet = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs,recordSet);
		recordSet.executeProc("Meeting_SelectByID", meetingid);
		recordSet.next();
		String creater = recordSet.getString("creater");
		int subId = Util.getIntValue(new ResourceComInfo(true).getSubCompanyID(creater)) ;
		boolean openCustomSet = false;
		rs.executeQuery(" select isOpen from meeting_remind_detachBaseInfo where subcompanyid = ?",subId);
		if(rs.next()){
			openCustomSet = rs.getInt(1) == 1;
		}
		if(detachable && openCustomSet){
			rs.executeQuery("select fieldid from meeting_reminderFields where isOpen = 1 and subcompanyid = ?",subId);
		}else{
			rs.executeQuery("select fieldid from meeting_reminderFields where isOpen = 1 and subcompanyid = 0 ");
		}

		while (rs.next()){
			String fieldid = rs.getString(1);
			Arrays.asList(fieldid.split(",")).stream().filter(item-> !item.equals("")).forEach(item->{
				fieldIdsSet.add(item);
			});
		}
		//单独处理会议负责人：服务负责人，
		if(fieldIdsSet.contains("42")){
			fieldIdsSet.add("47");
		}
		if(forShare && fieldIdsSet.contains("5")){
			rs.executeQuery("select * from meeting where id = ?",meetingid);
			if(rs.next()) {
				//单独处理会议室：负责人
				String address = rs.getString("address");
				reminderSet.addAll(getAddressHrmids(address));
			}
		}
		fieldIdsSet.remove("5");//针对会议室，因为只有流程提醒，在这里remove，防止下面再次获取会议室address当做人员
	}

	/**
	 * 获取多个会议室的负责人
	 * @param address
	 * @return
	 */
	public static Set getAddressHrmids (String address){
		Set<String> reminderSet = new HashSet<String>();
		if (!address.equals("")) {
			MeetingRoomComInfo mrci = new MeetingRoomComInfo();
			Arrays.asList(address.split(",")).stream().filter(s ->
					!s.equals("")
			).forEach(item -> {
				String roomHrmids = mrci.getMeetingRoomInfohrmids(item);
				if (!roomHrmids.equals("")) {
					Arrays.asList(roomHrmids.split(",")).stream().forEach(item2 -> {
						reminderSet.add(item2);
					});
				}
			});
		}
		return reminderSet;
	}

	/**
	 * 比较两个对象（String）差异
	 * @param resource
	 * @param oldResource
	 * @return
	 */
	public static Map<String,Set> diffString(String resource, String oldResource){

		Set resourceSet = Arrays.asList(resource.split(",")).stream().collect(Collectors.toSet());
		Set oldResourceSet = Arrays.asList(oldResource.split(",")).stream().collect(Collectors.toSet());
		return diffSet(resourceSet,oldResourceSet);
	}

	/**
	 *
	 * @param resource
	 * @param oldResource
	 * @return
	 */
	public static Map<String,Set> diffSet(Set<String> resource, Set<String> oldResource){
		Map<String,Set> diffMap = new HashMap<String,Set>();
		//添加人员
		Set<String> addMemberSet = new HashSet<String>();
		//删除人员
		Set<String> delMemberSet = new HashSet<String>();
		//原封不到的人员
		Set<String> keepMemberSet = new HashSet<String>();
		resource.forEach(item->{
			if(!oldResource.contains(item)){
				addMemberSet.add(item);
			}else{
				keepMemberSet.add(item);
				oldResource.remove(item);
			}
		});
		delMemberSet = oldResource;
		diffMap.put("add",addMemberSet);
		diffMap.put("del",delMemberSet);
		diffMap.put("keep",keepMemberSet);
		return diffMap;
	}

	/**
	 * 根据fieldid，查询出所有发送人
	 * @param meetingid
	 * @param reminderSet
	 * @param fieldIdsSet
	 * @param scopeid
	 */
	public static void setReminderSet(String meetingid, Set reminderSet, Set fieldIdsSet, int scopeid){
		MeetingFieldComInfo meetingFieldComInfo = new MeetingFieldComInfo();
		List<String> groupList = null;
 		MeetingFieldManager hfm = null;
		List<String> fieldList = null;
		String hrmDepartments = "";//参会部门
		String hrmSubCompanys = "";//参会分部
		RecordSet rs = new RecordSet();
		RecordSet rs2 = new RecordSet();
		MeetingEncryptUtil.setDecryptData2DaoInfo(rs,rs2);
		try {
			hfm = new MeetingFieldManager(scopeid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(scopeid == 1){
			rs.executeQuery("select * from meeting where id = ?",meetingid);
		} else if (scopeid == 2){
			rs.executeQuery(" select * from Meeting_Topic where meetingid = ?",meetingid);
		} else if (scopeid == 3){
			rs.executeQuery(" select * from Meeting_Service_New where meetingid = ?",meetingid);
		}
		int j = 0;
		while(rs.next()){
			if(scopeid == 1 && j == 0){//防止多次执行
				hrmDepartments = rs.getString("hrmDepartments");
				hrmSubCompanys = rs.getString("hrmSubCompanys");
				//处理会议参会人员以及参会客户
				if(fieldIdsSet.contains("29")){
					String sql="select membermanager,othermember,isattend from Meeting_Member2 where meetingid=? and membertype = 1 order by id";
					rs2.executeQuery(sql,meetingid);
					while(rs2.next()){
						if(!rs2.getString(3).equals("2")){
							reminderSet.add(rs2.getString(1));
						}
						String otherMember = rs2.getString(2);
						if(!otherMember.equals("")){
							List<String> otherMemberList = Util.TokenizerString(otherMember,",");
							for (int i = 0; i < otherMemberList.size(); i++) {
								if(!otherMemberList.get(i).equals("")){
									reminderSet.add(otherMemberList.get(i));
								}
							}
						}
					}
					// 对参会人员，需要额外添加参会部门和参会分部
					String depAndSubHrms = MeetingUtil.getDepAndSubHrms(meetingid,hrmDepartments,hrmSubCompanys);
					if(!depAndSubHrms.equals("")){
						Set depAdnSubHrmsSet = Arrays.asList(depAndSubHrms.split(",")).stream().collect(Collectors.toSet());
						reminderSet.addAll(depAdnSubHrmsSet);
					}
					fieldIdsSet.remove("29");
				}
				if(fieldIdsSet.contains("32")){
					String sql="select membermanager,isattend from Meeting_Member2 where meetingid=? and membertype = 2 order by id";
					rs2.executeQuery(sql,meetingid);
					while(rs2.next()){
						if(!rs2.getString(3).equals("2")){
							reminderSet.add(rs2.getString(1));
						}
					}
					fieldIdsSet.remove("32");
				}
				j++;
			}

			groupList = hfm.getLsGroup();
			for (String groupid : groupList) {
				fieldList = hfm.getUseField(groupid);
				for (String fieldid : fieldList) {
					if(fieldIdsSet.contains(fieldid)){
						String fieldname = meetingFieldComInfo.getFieldname(fieldid);
						String fieldValue = rs.getString(fieldname);
						Arrays.asList(fieldValue.split(",")).stream().filter(item->!item.equals("")).forEach(item->{
							reminderSet.add(item);
						});
					}
				}
			}
		}
	}
	
}
