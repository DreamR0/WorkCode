package com.engine.kq.cmd.group;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQFixedSchedulceComInfo;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.util.UtilKQ;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveGroupCmd extends AbstractCommonCommand<Map<String, Object>> {

	public SaveGroupCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{			
			//必要的权限判断
			if(!HrmUserVarify.checkUserRight("HrmKQGroup:Add",user)) {
		  	retmap.put("status", "-1");
		  	retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
		  	return retmap;
		  }

			KQGroupComInfo kQGroupComInfo = new KQGroupComInfo();
			KQFixedSchedulceComInfo kqFixedSchedulceComInfo = new KQFixedSchedulceComInfo();
			String datas = Util.null2String(params.get("data"));
			JSONObject jsonObj = JSON.parseObject(datas);
			String tabKey = Util.null2String(params.get("tabKey"));//分组id
			String id = Util.null2String(jsonObj.get("id"));//考勤组id
			String groupname = Util.null2String(jsonObj.get("groupname"));//考勤组名称
			String kqtype = Util.null2String(jsonObj.get("kqtype"));//考勤类型
			String subcompanyid = Util.null2String(jsonObj.get("subcompanyid"));//所属分部
			String excludeid = Util.null2String(jsonObj.get("excludeid"));//考勤组排除人员
			String excludecount = Util.null2String(jsonObj.get("excludecount"));//考勤组排除人员是否参与统计
			String signintype = Util.null2String(jsonObj.get("signintype"));//打卡方式
			String serialids = Util.null2String(jsonObj.get("serialids"));//考勤班次
			String weekday = Util.null2String(jsonObj.get("weekday"));//考勤类型
			String signstart = Util.null2String(jsonObj.get("signstart"));//考勤类型
			String workhour = Util.null2String(jsonObj.get("workhour"));//工作时长
			String ipscope =Util.null2String( jsonObj.get("ipscope"));//应用IP范围
			String locationcheck = Util.null2String(jsonObj.get("locationcheck"));//启用办公地点考勤
			//String locationcheckscope = Util.null2String(jsonObj.get("locationcheckscope"));//有效范围
			String wificheck = Util.null2String(jsonObj.get("wificheck"));//启用wifi考勤
			String outsidesign = Util.null2String(jsonObj.get("outsidesign"));//允许外勤打卡
			String validity = Util.null2String(jsonObj.get("validity"));//考勤组有效期
			String validityfromdate = Util.null2String(jsonObj.get("validityfromdate"));//考勤组有效期开始时间
			String validityenddate = Util.null2String(jsonObj.get("validityenddate"));//考勤组有效期结束时间
			String locationfacecheck = Util.null2String(jsonObj.getString("locationfacecheck"));//办公地点启用人脸识别拍照打卡
			String locationshowaddress = Util.null2String(jsonObj.getString("locationshowaddress"));//有效识别半径内显示同一地址
			String wififacecheck = Util.null2String(jsonObj.getString("wififacecheck"));//wifi启用人脸识别拍照打卡
			//qc2505403
			String officeIpCheck = Util.null2String(jsonObj.getString("officeIpCheck"));//当使用web打卡时，检测ip是否在办公地址范围内

			List<Object> lsParams = new ArrayList<>();
			if(tabKey.equals("1")){
				if(id.length()>0) {
					sql = " update kq_group set groupname=?,subcompanyid=?,excludeid=?,excludecount=?,kqtype=?," +
								" serialids=?,weekday=?,signstart=?,workhour=?,validity=?,validityfromdate=?,validityenddate=?,officeIpCheck=? " +
								" where id=? ";
					lsParams.add(groupname.length()==0?null:groupname);
					lsParams.add(subcompanyid.length()==0?null:subcompanyid);
					lsParams.add(excludeid.length()==0?null:excludeid);
					lsParams.add(excludecount.length()==0?null:excludecount);
					lsParams.add(kqtype.length()==0?null:kqtype);
					lsParams.add(serialids.length()==0?null:serialids);
					lsParams.add(weekday.length()==0?null:weekday);
					lsParams.add(signstart.length()==0?null:signstart);
					lsParams.add(workhour.length()==0?null:workhour);
					lsParams.add(validity.length()==0?null:validity);
					lsParams.add(validityfromdate.length()==0?null:validityfromdate);
					lsParams.add(validityenddate.length()==0?null:validityenddate);
					//qc2505403
					lsParams.add(officeIpCheck.length()==0?null:officeIpCheck);
					lsParams.add(id);
					rs.executeUpdate(sql,lsParams);
					if(kqtype.equals("2")){
						this.saveKqTypeInfo();
					}
				}else {
					sql = " insert into kq_group (" +
								" groupname,subcompanyid,excludeid,excludecount,kqtype,serialids," +
								" weekday,signstart,workhour,signintype,validity,validityfromdate,validityenddate,locationcheckscope,officeIpCheck) " +
								" values(?,?,?,?,?,?, ?,?,?,1,?,?,?,300,?)";
					lsParams.add(groupname.length()==0?null:groupname);
					lsParams.add(subcompanyid.length()==0?null:subcompanyid);
					lsParams.add(excludeid.length()==0?null:excludeid);
					lsParams.add(excludecount.length()==0?null:excludecount);
					lsParams.add(kqtype.length()==0?null:kqtype);
					lsParams.add(serialids.length()==0?null:serialids);

					lsParams.add(weekday.length()==0?null:weekday);
					lsParams.add(signstart.length()==0?null:signstart);
					lsParams.add(workhour.length()==0?null:workhour);
					lsParams.add(validity.length()==0?null:validity);
					lsParams.add(validityfromdate.length()==0?null:validityfromdate);
					lsParams.add(validityenddate.length()==0?null:validityenddate);
					//qc2505403
					lsParams.add(officeIpCheck.length()==0?null:officeIpCheck);
					rs.executeUpdate(sql,lsParams);

					rs.executeQuery("select max(id) from kq_group") ;
					if(rs.next()){
						id = rs.getString(1);
					}
				}
				params.put("id",id);
				this.saveKqTypeInfo();
			}else if(tabKey.equals("2")){
				sql = " update kq_group set signintype=?, ipscope=?,locationcheck=?,wificheck=?,outsidesign=?, " +//locationcheckscope=?,
							" locationfacecheck=?,locationshowaddress=?,wififacecheck=?,officeIpCheck=? where id=? ";
				lsParams.add(signintype.length()==0?null:signintype);
				lsParams.add(ipscope.length()==0?null:ipscope);
				lsParams.add(locationcheck.length()==0?null:locationcheck);
				//lsParams.add(locationcheckscope.length()==0?null:locationcheckscope);
				lsParams.add(wificheck.length()==0?null:wificheck);
				lsParams.add(outsidesign.length()==0?null:outsidesign);
				lsParams.add(locationfacecheck.length()==0?null:locationfacecheck);
				lsParams.add(locationshowaddress.length()==0?null:locationshowaddress);
				lsParams.add(wififacecheck.length()==0?null:wififacecheck);
				//qc2505403
				lsParams.add(officeIpCheck.length()==0?null:officeIpCheck);
				lsParams.add(id);
				rs.executeUpdate(sql,lsParams);
			}

			kQGroupComInfo.removeCache();
			if(kqtype.equals("1")){
				kqFixedSchedulceComInfo.removeCache();
			}
			retmap.put("id", id);
			retmap.put("status", "1");
			retmap.put("message", SystemEnv.getHtmlLabelName(18758, user.getLanguage()));
		}catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
	}

	private Map<String,Object> saveKqTypeInfo(){
		Map<String,Object> kqTypeInfo = new HashMap<>();
		RecordSet rs = new RecordSet();
		BatchRecordSet batchRecordSet = new BatchRecordSet();
		String sql = "";
		try{
			String groupid = Util.null2String(params.get("id"));
			JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
			String kqtype = Util.null2String(jsonObj.get("kqtype"));//考勤类型

			if(kqtype.equals("1")){//固定班次
				JSONArray fixedSchedulce = jsonObj.getJSONArray("fixedSchedulce");//固定班次明细表

				if(fixedSchedulce==null || fixedSchedulce.size()>7) return kqTypeInfo;
				for(int i=0;fixedSchedulce!=null&&i<fixedSchedulce.size();i++){
					jsonObj = (JSONObject)fixedSchedulce.get(i);
					String weekday = ""+i;//星期几
					String serialid = Util.null2String(jsonObj.get("serialid"));
					int id = 0;

					sql = " select id from kq_fixedschedulce where weekday = ? and groupid = ? ";
					rs.executeQuery(sql, weekday, groupid);
					if (rs.next()) {
						id = rs.getInt("id");
					}

					if(id>0){
						sql = "update kq_fixedschedulce set serialid =? where id=? ";
						rs.executeUpdate(sql,serialid.length()==0?null:serialid,id);
					}else{
						sql = "insert into kq_fixedschedulce(weekday,serialid,groupid) values(?,?,?)";
						rs.executeUpdate(sql,weekday,serialid.length()==0?null:serialid,groupid);
					}
				}
			}
			else if(kqtype.equals("2")){//排班制
				JSONArray shiftSchedulce = jsonObj.getJSONArray("shiftSchedulce");//固定班次明细表

				List<String> lsDelete = new ArrayList<>();
				sql = " select id from kq_group_shiftcycle where groupid = ? ";
				rs.executeQuery(sql, groupid);
				while(rs.next()){
					lsDelete.add(rs.getString("id"));
				}

				List<List<Object>> paramUpdate = new ArrayList<>();
				List<List<Object>> paramInsert = new ArrayList<>();
				List<Object> shiftSchedulceParams = null;
				for(int i=0;shiftSchedulce!=null&&i<shiftSchedulce.size();i++){
					jsonObj = (JSONObject)shiftSchedulce.get(i);
					String id = Util.null2String(jsonObj.get("id"));
					String shiftcyclename = Util.null2String(jsonObj.get("shiftcyclename"));
					String shiftcycleday = Util.null2String(jsonObj.get("shiftcycleday"));
					String shiftcycleserialids = Util.null2String(jsonObj.get("serial"));

					shiftSchedulceParams = new ArrayList<>();
					if(id.length()>0){
						shiftSchedulceParams.add(shiftcyclename);
						shiftSchedulceParams.add(shiftcycleday);
						shiftSchedulceParams.add(shiftcycleserialids);
						shiftSchedulceParams.add(id);
						paramUpdate.add(shiftSchedulceParams);
						lsDelete.remove(id);
					}else{
						shiftSchedulceParams.add(shiftcyclename);
						shiftSchedulceParams.add(shiftcycleday);
						shiftSchedulceParams.add(shiftcycleserialids);
						shiftSchedulceParams.add(groupid);
						paramInsert.add(shiftSchedulceParams);
					}
				}
				sql = " update kq_group_shiftcycle set shiftcyclename=?, shiftcycleday=?,shiftcycleserialids=? where id = ? ";
				batchRecordSet.executeBatchSql(sql, paramUpdate);

				sql = " insert into kq_group_shiftcycle (shiftcyclename, shiftcycleday, shiftcycleserialids,groupid) values (?,?,?,?)";
				batchRecordSet.executeBatchSql(sql, paramInsert);

				for(int i=0;i<lsDelete.size();i++) {
					sql = " delete from kq_group_shiftcycle where id = ? ";
					rs.executeUpdate(sql, lsDelete.get(i));
				}
			}
//			else if(kqtype.equals("3")){//自由班制
//				String weekday = Util.null2String(jsonObj.get("weekday"));//考勤类型
//				String signstart = Util.null2String(jsonObj.get("signstart"));//考勤类型
//				String[] weekdays = Util.splitString(weekday,",");
//
//				//删除取消的星期
//				sql = "delete from kq_freeschedulce where weekday not in (?) and groupid=?";
//				rs.executeUpdate(sql,weekday,groupid);
//
//				//更新开始时间
//				sql = "update kq_freeschedulce set signstart=? where weekday in (?) and groupid=?";
//				rs.executeUpdate(sql,signstart,weekday,groupid);
//
//				List<List<Object>> lsParams = new ArrayList<List<Object>>();
//				for(int i=0;i<weekdays.length;i++){
//					sql = "select count(1) from kq_freeschedulce where weekday=? and groupid=?";
//					rs.executeQuery(sql,weekdays[i],groupid);
//					if(rs.next()){
//						if(rs.getInt(1)<=0){
//							List<Object> params = new ArrayList<Object>();
//							params.add(weekdays[i]);
//							params.add(signstart);
//							params.add(groupid);
//							lsParams.add(params);
//						}
//					}
//				}
//				if(lsParams.size()>0){
//					sql = "insert into kq_freeschedulce(weekday,signstart,groupid) values(?,?,?)";
//					rs.executeUpdate(sql,lsParams);
//				}
//			}
		}catch (Exception e){
			writeLog(e);
		}
		return kqTypeInfo;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

}
