package com.engine.kq.cmd.group;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQAutoCheckComInfo;
import com.engine.kq.biz.KQFixedSchedulceComInfo;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.log.KQLog;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

public class SaveGroupCmd extends AbstractCommonCommand<Map<String, Object>> {

	private SimpleBizLogger logger;
	SimpleBizLogger.SubLogInfo subLogInfo;
	SimpleBizLogger.SubLogInfo subLogInfo1;
	public SaveGroupCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;

		if (params != null && params.containsKey("data")) {
			String datas = Util.null2String(params.get("data"));
			JSONObject jsonObj = JSON.parseObject(datas);
			String id = Util.null2String(jsonObj.get("id"));
			if(id.length()==0) {
					id = "-1";
			}
			this.logger = new SimpleBizLogger();
			BizLogContext logContext = new BizLogContext();
			logContext.setDateObject(new Date());
			logContext.setLogType(BizLogType.HRM_ENGINE);
			logContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_KQGROUP);
			logContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_KQGROUP);
			logContext.setParams(params);
			logger.setUser(user);//当前操作人

			String mainSql = " select * from kq_group where id= " + id;
			logger.setMainSql(mainSql);//主表sql
			logger.setMainPrimarykey("id");//主日志表唯一key
			logger.setMainTargetNameColumn("groupname");

			subLogInfo1 = logger.getNewSubLogInfo();
			String subSql1 = "select * from kq_fixedschedulce where groupid="+id;
			subLogInfo1.setSubPrimarykey("id");
			subLogInfo1.setSubTargetNameColumn("weekday");
			subLogInfo1.setGroupId("0");  //所属分组， 按照groupid排序显示在详情中， 不设置默认按照add的顺序。
			subLogInfo1.setSubGroupNameLabel(505654); //在详情中显示的分组名称，不设置默认显示明细x
			subLogInfo1.setSubSql(subSql1);
			logger.addSubLogInfo(subLogInfo1);

			subLogInfo = logger.getNewSubLogInfo();
			String subSql = " select * from kq_group_shiftcycle where groupid = " + id;
			subLogInfo.setSubSql(subSql);
			subLogInfo.setSubPrimarykey("id");
			subLogInfo.setSubTargetNameColumn("shiftcyclename");
			subLogInfo.setGroupId("1");  //所属分组， 按照groupid排序显示在详情中， 不设置默认按照add的顺序。
			subLogInfo.setSubGroupNameLabel(389225); //在详情中显示的分组名称，不设置默认显示明细x
			logger.addSubLogInfo(subLogInfo);
			logger.before(logContext);
		}
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
      new KQLog().info("SaveGroupCmd params:"+params);
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
      String calmethod = Util.null2s(Util.null2String(jsonObj.get("calmethod")),"1");//工作时长计算方式 1是打卡时间累加计算 2是打卡时间成对计算

      String ipscope_v4_pc =Util.null2String( jsonObj.get("ipscope_v4_pc"));//应用IP范围 ipv4 pc端
      String ipscope_v4_em =Util.null2String( jsonObj.get("ipscope_v4_em"));//应用IP范围 ipv4 移动端
      String ipscope_v6_pc =Util.null2String( jsonObj.get("ipscope_v6_pc"));//应用IP范围 ipv6 pc端
      String ipscope_v6_em =Util.null2String( jsonObj.get("ipscope_v6_em"));//应用IP范围 ipv6 移动端

			String locationcheck = Util.null2String(jsonObj.get("locationcheck"));//启用办公地点考勤
			//String locationcheckscope = Util.null2String(jsonObj.get("locationcheckscope"));//有效范围
			String wificheck = Util.null2String(jsonObj.get("wificheck"));//启用wifi考勤
			String outsidesign = Util.null2String(jsonObj.get("outsidesign"));//允许外勤打卡
			String validity = Util.null2String(jsonObj.get("validity"));//考勤组有效期
			String validityfromdate = Util.null2String(jsonObj.get("validityfromdate"));//考勤组有效期开始时间
			String validityenddate = Util.null2String(jsonObj.get("validityenddate"));//考勤组有效期结束时间
			String locationfacecheck = Util.null2String(jsonObj.getString("locationfacecheck"));//办公地点启用人脸识别拍照打卡
			String locationfacechecktype = Util.null2String(jsonObj.getString("locationfacechecktype"));//qc2473809 办公地点启用人脸识别拍照打卡方式
			String locationshowaddress = Util.null2String(jsonObj.getString("locationshowaddress"));//有效识别半径内显示同一地址
			String wififacecheck = Util.null2String(jsonObj.getString("wififacecheck"));//wifi启用人脸识别拍照打卡
			String wififacechecktype = Util.null2String(jsonObj.getString("wififacechecktype"));//qc2473809 wifi启用人脸识别拍照打卡方式
			String auto_checkin = Util.null2String(jsonObj.getString("auto_checkin"));//允许客户端设置自动考勤 上班卡
			String auto_checkout = Util.null2String(jsonObj.getString("auto_checkout"));//允许客户端设置自动考勤 下班卡
      String auto_checkin_before = Util.null2String(jsonObj.getString("auto_checkin_before"));//允许客户端设置自动考勤 上班卡开始分钟数
      String auto_checkin_after = Util.null2String(jsonObj.getString("auto_checkin_after"));//允许客户端设置自动考勤 上班卡结束分钟时
      String auto_checkout_before = Util.null2String(jsonObj.getString("auto_checkout_before"));//允许客户端设置自动考勤 下班卡 下班卡开始分钟数
      String auto_checkout_after = Util.null2String(jsonObj.getString("auto_checkout_after"));//允许客户端设置自动考勤 下班卡 下班卡结束分钟时

			//qc2473809
			if("".equals(id)) {
				sql = "select * from kq_group where groupname=? and (isDelete is null or isDelete !=1) ";
				rs.executeQuery(sql, groupname);
			} else {
				sql = "select * from kq_group where groupname=? and id != ? and (isDelete is null or isDelete !=1) ";
				rs.executeQuery(sql, groupname, id);
			}
			if(rs.next()) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(531673, user.getLanguage()));
				return retmap;
			}

      if("1".equalsIgnoreCase(signintype) || "3".equalsIgnoreCase(signintype)){
        //如果有移动端打卡，但是办公地点和wifi都没开启，也把自动打卡重置
        if(!"1".equalsIgnoreCase(locationcheck) && !"1".equalsIgnoreCase(wificheck)){
          auto_checkin = "0";
          auto_checkout = "0";
          auto_checkin_before = "30";
          auto_checkin_after = "30";
          auto_checkout_before = "5";
          auto_checkout_after = "60";
        }
      }else{
        //如果不是移动端打卡，需要把自动打卡重置
        auto_checkin = "0";
        auto_checkout = "0";
        auto_checkin_before = "30";
        auto_checkin_after = "30";
        auto_checkout_before = "5";
        auto_checkout_after = "60";
      }

			List<Object> lsParams = new ArrayList<>();
			if(tabKey.equals("1")){
				if(id.length()>0) {
					sql = " update kq_group set groupname=?,subcompanyid=?,excludeid=?,excludecount=?,kqtype=?," +
								" serialids=?,weekday=?,signstart=?,workhour=?,validity=?,validityfromdate=?,validityenddate=?,calmethod=? " +
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
					lsParams.add(calmethod.length()==0?"1":calmethod);
					lsParams.add(id);
					rs.executeUpdate(sql,lsParams);
					if(kqtype.equals("2")){
						this.saveKqTypeInfo();
					}
				}else {
					sql = " insert into kq_group (" +
								" groupname,subcompanyid,excludeid,excludecount,kqtype,serialids," +
								" weekday,signstart,workhour,signintype,validity,validityfromdate,validityenddate,locationcheckscope,calmethod) " +
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
					lsParams.add(calmethod.length()==0?"1":calmethod);
					rs.executeUpdate(sql,lsParams);

					rs.executeQuery("select max(id) from kq_group") ;
					if(rs.next()){
						id = rs.getString(1);
					}
				}
				params.put("id",id);
				this.saveKqTypeInfo();
			}else if(tabKey.equals("2")){
				sql = " update kq_group set signintype=?, ipscope_v4_pc=?,ipscope_v4_em=?,ipscope_v6_pc=?,ipscope_v6_em=?,locationcheck=?,wificheck=?,outsidesign=?, " +//locationcheckscope=?,
							" locationfacecheck=?,locationfacechecktype=?,locationshowaddress=?,wififacecheck=?,wififacechecktype=?,auto_checkin=?,auto_checkout=?,"
            + " auto_checkin_before=?,auto_checkin_after=?,auto_checkout_before=?,auto_checkout_after=? where id=? ";
				lsParams.add(signintype.length()==0?null:signintype);
				lsParams.add(ipscope_v4_pc.length()==0?null:ipscope_v4_pc);
				lsParams.add(ipscope_v4_em.length()==0?null:ipscope_v4_em);
				lsParams.add(ipscope_v6_pc.length()==0?null:ipscope_v6_pc);
				lsParams.add(ipscope_v6_em.length()==0?null:ipscope_v6_em);
				lsParams.add(locationcheck.length()==0?null:locationcheck);
				//lsParams.add(locationcheckscope.length()==0?null:locationcheckscope);
				lsParams.add(wificheck.length()==0?null:wificheck);
				lsParams.add(outsidesign.length()==0?null:outsidesign);
				lsParams.add(locationfacecheck.length()==0?null:locationfacecheck);
				lsParams.add(locationfacechecktype.length()==0?null:locationfacechecktype);//qc2473809
				lsParams.add(locationshowaddress.length()==0?null:locationshowaddress);
				lsParams.add(wififacecheck.length()==0?null:wififacecheck);
				lsParams.add(wififacechecktype.length()==0?null:wififacechecktype);//qc2473809
				lsParams.add(auto_checkin.length()==0?0:auto_checkin);
				lsParams.add(auto_checkout.length()==0?0:auto_checkout);
				lsParams.add(auto_checkin_before.length()==0?30:auto_checkin_before);
				lsParams.add(auto_checkin_after.length()==0?30:auto_checkin_after);
				lsParams.add(auto_checkout_before.length()==0?5:auto_checkout_before);
				lsParams.add(auto_checkout_after.length()==0?60:auto_checkout_after);
				String ori_auto_checkin = kQGroupComInfo.getAuto_checkin(id);
				String ori_auto_checkout = kQGroupComInfo.getAuto_checkout(id);

				//关于自动打卡开启或者关闭后的一些逻辑处理
        group_auto_check(id,auto_checkin,auto_checkout,ori_auto_checkin,ori_auto_checkout,retmap,kQGroupComInfo,locationcheck,wificheck,locationfacecheck,wififacecheck);
        if(!retmap.isEmpty()){
				  return retmap;
        }

				lsParams.add(id);
				rs.executeUpdate(sql,lsParams);
			}

			kQGroupComInfo.removeCache();
			if(kqtype.equals("1")){
				kqFixedSchedulceComInfo.removeCache();
			}

			String mainSql = " select * from kq_group where id= " + id;
			logger.setMainSql(mainSql);//主表sql

			String subSql1 = "select * from kq_fixedschedulce where groupid="+id;
			subLogInfo1.setSubSql(subSql1);

			String subSql = " select * from kq_group_shiftcycle where groupid = " + id;
			subLogInfo.setSubSql(subSql);

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

  /**
   * 关于自动打卡开启或者关闭后的一些逻辑处理
   * @param id
   * @param auto_checkin
   * @param auto_checkout
   * @param ori_auto_checkin
   * @param ori_auto_checkout
   * @param retmap
   * @param kQGroupComInfo
   * @param locationcheck
   * @param wificheck
   */
  public void group_auto_check(String id, String auto_checkin, String auto_checkout,
      String ori_auto_checkin, String ori_auto_checkout,
      Map<String, Object> retmap, KQGroupComInfo kQGroupComInfo, String locationcheck,
      String wificheck, String locationfacecheck, String wififacecheck) {
    KQAutoCheckComInfo kqAutoCheckComInfo = new KQAutoCheckComInfo();
    RecordSet rs = new RecordSet();

    if(!"1".equalsIgnoreCase(auto_checkin) && !"1".equalsIgnoreCase(auto_checkout)){
      //如果自动打卡 都关闭了，那么自定义设置的也需要被清空
      String del_cus_autoset = "delete from kq_autocheck_set where groupid = ? ";
      rs.executeUpdate(del_cus_autoset,id);
      kqAutoCheckComInfo.removeCache();
    }else{
      String tmp_kqtype = kQGroupComInfo.getKqtype(id);
      if("3".equalsIgnoreCase(tmp_kqtype)){
        //如果是弹性工作制
        retmap.put("status", "-1");
        retmap.put("message",  SystemEnv.getHtmlLabelName(519990,user.getLanguage()));
      }
      boolean has_location_list = has_location_list(id);
      boolean has_wifi_list = has_wifi_list(id);
      if("1".equalsIgnoreCase(locationcheck)){
        //地理位置的开关开启了
        if("1".equalsIgnoreCase(wificheck)){
          //wifi的开关开启了
          if(!has_location_list && !has_wifi_list){
            retmap.put("status", "-1");
            retmap.put("message",  SystemEnv.getHtmlLabelName(519989,user.getLanguage()));
          }
          if("1".equalsIgnoreCase(wififacecheck)){
            //如果开启了人脸打卡
            retmap.put("status", "-1");
            retmap.put("message",  SystemEnv.getHtmlLabelName(521384,user.getLanguage()));
          }
        }else{
          if(!has_location_list){
            retmap.put("status", "-1");
            retmap.put("message",  SystemEnv.getHtmlLabelName(519989,user.getLanguage()));
          }
        }
        if("1".equalsIgnoreCase(locationfacecheck)){
          //如果开启了人脸打卡
          retmap.put("status", "-1");
          retmap.put("message",  SystemEnv.getHtmlLabelName(521384,user.getLanguage()));
        }
      }else{
        //地理位置没开启
        if("1".equalsIgnoreCase(wificheck)){
          //wifi的开关开启了
          if(!has_wifi_list){
            retmap.put("status", "-1");
            retmap.put("message",  SystemEnv.getHtmlLabelName(519989,user.getLanguage()));
          }
          if("1".equalsIgnoreCase(wififacecheck)){
            //如果开启了人脸打卡
            retmap.put("status", "-1");
            retmap.put("message",  SystemEnv.getHtmlLabelName(521384,user.getLanguage()));
          }
        }
      }

      if(!ori_auto_checkin.equalsIgnoreCase(auto_checkin)){
        //如果上班自动打卡变化了，需要去更新掉自定义的设置
        String up_cus_autoset = "update kq_autocheck_set set auto_checkin=? where groupid = ? ";
        rs.executeUpdate(up_cus_autoset, auto_checkin,id);
      }
      if(!ori_auto_checkout.equalsIgnoreCase(ori_auto_checkout)){
        //如果下班自动打卡变化了，需要去更新掉自定义的设置
        String up_cus_autoset = "update kq_autocheck_set set auto_checkout=? where groupid = ? ";
        rs.executeUpdate(up_cus_autoset, auto_checkout,id);
      }
      kqAutoCheckComInfo.removeCache();
    }
  }

  /**
   * 判断当前考勤组是否有考勤wifi列表
   * @param id
   * @return
   */
  public boolean has_wifi_list(String id) {
    RecordSet rs = new RecordSet();
    String wifi_list_sql = "select 1 from kq_wifi where groupid = ? ";
    rs.executeQuery(wifi_list_sql,id);
    if(!rs.next()){
      return false;
    }
    return true;
  }

  /**
   * 判断当前考勤组是否有考勤地理位置列表
   * @param id
   * @return
   */
  public boolean has_location_list(String id) {

	  RecordSet rs = new RecordSet();
    String location_list_sql = "select 1 from kq_location where groupid = ? ";
    rs.executeQuery(location_list_sql,id);
    if(!rs.next()){
      return false;
    }
    return true;
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

	@Override
	public List<BizLogContext> getLogContexts() {
		return logger.getBizLogContexts();
	}
}
