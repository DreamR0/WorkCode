package com.engine.kq.cmd.group;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.util.PageUidFactory;
import com.engine.kq.util.UtilKQ;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.GCONST;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

public class GetGroupFormCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetGroupFormCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		List<Map<String,Object>> grouplist = new ArrayList<Map<String,Object>>();
		Map<String,Object> groupitem = null;
		List<Object> itemlist = null;
		Map<String, Object> otherparam = null;
		RecordSet rs = new RecordSet();
		String sql = "";
		try{			
			//必要的权限判断
			if(!HrmUserVarify.checkUserRight("HrmKQGroup:Add",user)) {
		  	retmap.put("status", "-1");
		  	retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
		  	return retmap;
		  }
			//qc2473809 考勤打卡支持真实身份认证功能需求开发
			ChgPasswdReminder reminder=new ChgPasswdReminder();
			RemindSettings settings=reminder.getRemindSettings();
			//qc2473809

			KQGroupComInfo kQGroupComInfo = new KQGroupComInfo();
			ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
			boolean hrmdetachable = manageDetachComInfo.isUseHrmManageDetach();//是否开启了人力资源模块的管理分权

			String id = Util.null2String(params.get("id"));
			String subcompanyid = Util.null2String(params.get("subcompanyid"));//所属分部
		  String tabKey = Util.null2String(params.get("tabKey"));
			Map<String, List<HrmFieldBean>> fieldGroups= new LinkedHashMap<String, List<HrmFieldBean>>();
			List<HrmFieldBean> lsField = new ArrayList<HrmFieldBean>();
			HrmFieldBean hrmFieldBean = null;
			List<SearchConditionOption> options = null;

			String groupname = "";//考勤组名称
			String kqtype = "1";//考勤类型 默认固定班制
			String excludeid = "";//考勤组排除人员
			String excludecount = "";//考勤组排除人员是否参与统计
			String serialids = "";//考勤班次
			List<Integer> lsWeekday = new ArrayList<>();//考勤工作日
			String weekday = "";
			String signstart = "";//考勤开始时间
			String workhour = "";//工作时长
			String signintype = "1";//打卡方式
			String ipscope = "";//应用IP范围
			String ipscope_v4_pc = "";//应用IP范围 v4 pc端
			String ipscope_v4_em = "";//应用IP范围 v4 移动端
			String ipscope_v6_pc = "";//应用IP范围 v6 pc端
			String ipscope_v6_em = "";//应用IP范围 v6 移动端
			String locationcheck = "";//启用办公地点考勤
			//String locationcheckscope = "300";//有效范围
			String wificheck = "";//启用wifi考勤
			String outsidesign = "";//允许外勤打卡
			String validity = "";//考勤组有效期
			String validityfromdate = "";//考勤组有效期开始时间
			String validityenddate = "";//考勤组有效期结束时间
			String locationfacecheck = "";//办公地点启用人脸识别拍照打卡
			String  locationfacechecktype = "";//办公地点启用人脸识别拍照打卡方式    qc2473809
			String locationshowaddress = "";//有效识别半径内显示同一地址
			String wififacecheck = "";//wifi启用人脸识别拍照打卡
			String wififacechecktype = "";//wifi启用人脸识别拍照打卡方式			qc2473809
			String auto_checkin = "0";//允许客户端设置自动考勤 上班卡
      String auto_checkin_before = "30";//允许客户端设置自动考勤 上班卡开始分钟数
      String auto_checkin_after = "30";//允许客户端设置自动考勤 上班卡结束分钟时
      String auto_checkout = "0";//允许客户端设置自动考勤 下班卡
      String auto_checkout_before = "5";//允许客户端设置自动考勤 下班卡 下班卡开始分钟数
      String auto_checkout_after = "60";//允许客户端设置自动考勤 下班卡 下班卡结束分钟时
      String calmethod = "1";//工作时长计算方式 1是打卡时间累加计算 2是打卡时间成对计算

			if(id.length()>0){
				sql = "select * from kq_group where id=?";
				rs.executeQuery(sql,id);
				if(rs.next()){
					groupname = Util.null2String(rs.getString("groupname"));
					excludeid = Util.null2String(rs.getString("excludeid"));
					excludecount = Util.null2String(rs.getString("excludecount"));
					subcompanyid = Util.null2String(rs.getString("subcompanyid"));
					kqtype = Util.null2String(rs.getString("kqtype"));
					serialids=Util.null2String(rs.getString("serialids"));
					weekday = Util.null2String(rs.getString("weekday"));
					signstart = Util.null2String(rs.getString("signstart"));
					workhour = Util.null2String(rs.getString("workhour"));
					signintype = Util.null2String(rs.getString("signintype"));
					ipscope = Util.null2String(rs.getString("ipscope"));
          ipscope_v4_pc = Util.null2String(rs.getString("ipscope_v4_pc"));
          ipscope_v4_em = Util.null2String(rs.getString("ipscope_v4_em"));
          ipscope_v6_pc = Util.null2String(rs.getString("ipscope_v6_pc"));
          ipscope_v6_em = Util.null2String(rs.getString("ipscope_v6_em"));
					locationcheck = Util.null2String(rs.getString("locationcheck"));
					//locationcheckscope = Util.null2String(rs.getString("locationcheckscope"));
					wificheck = Util.null2String(rs.getString("wificheck"));
					outsidesign = Util.null2String(rs.getString("outsidesign"));
					validity = Util.null2String(rs.getString("validity"));
					validityfromdate = Util.null2String(rs.getString("validityfromdate"));
					validityenddate = Util.null2String(rs.getString("validityenddate"));
					locationfacecheck = Util.null2String(rs.getString("locationfacecheck"));
					locationfacechecktype = Util.null2String(rs.getString("locationfacechecktype"));//qc2473809
					locationshowaddress = Util.null2String(rs.getString("locationshowaddress"));
					wififacecheck = Util.null2String(rs.getString("wififacecheck"));
					wififacechecktype = Util.null2String(rs.getString("wififacechecktype"));//qc2473809
					auto_checkin = Util.null2String(rs.getString("auto_checkin"),"0");
          auto_checkin_before = Util.null2String(rs.getString("auto_checkin_before"),"30");
          auto_checkin_after = Util.null2String(rs.getString("auto_checkin_after"),"30");
          auto_checkout = Util.null2String(rs.getString("auto_checkout"),"0");
          auto_checkout_before = Util.null2String(rs.getString("auto_checkout_before"),"5");
          auto_checkout_after = Util.null2String(rs.getString("auto_checkout_after"),"60");
          calmethod = Util.null2s(rs.getString("calmethod"),"1");
				}
			}

			if(tabKey.equals("1")){
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("groupname");
				hrmFieldBean.setFieldlabel("388700");
				hrmFieldBean.setFieldhtmltype("1");
				hrmFieldBean.setType("1");
				hrmFieldBean.setFieldvalue(groupname);
				hrmFieldBean.setViewAttr(3);
				hrmFieldBean.setRules("required|string");
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("excludeid");
				hrmFieldBean.setFieldlabel("388703");
				hrmFieldBean.setFieldhtmltype("3");
				hrmFieldBean.setType("17");
				hrmFieldBean.setFieldvalue(excludeid);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("excludecount");
				hrmFieldBean.setFieldlabel("507794");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(excludecount);
				lsField.add(hrmFieldBean);

				//增加有效期设置
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("validity");
				hrmFieldBean.setFieldlabel("15030");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(validity);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("validityfromdate");
				hrmFieldBean.setFieldlabel("742");
				hrmFieldBean.setFieldhtmltype("3");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(validityfromdate);
				hrmFieldBean.setRules("required|string");
				hrmFieldBean.setViewAttr(3);
				hrmFieldBean.setIsFormField(true);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("validityenddate");
				hrmFieldBean.setFieldlabel("743");
				hrmFieldBean.setFieldhtmltype("3");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(validityenddate);
				hrmFieldBean.setRules("required|string");
				hrmFieldBean.setViewAttr(3);
				hrmFieldBean.setIsFormField(true);
				lsField.add(hrmFieldBean);

				String defaultSubcompanyid = "";
				if(hrmdetachable){
					CheckSubCompanyRight newCheck=new CheckSubCompanyRight();
					int[] subcomids = newCheck.getSubComByUserRightId(user.getUID(),"HrmKQGroup:Add",0);
					ManageDetachComInfo detachComInfo = new ManageDetachComInfo();
					if(detachComInfo.isUseHrmManageDetach()){
						defaultSubcompanyid = detachComInfo.getHrmdftsubcomid();
					}else{
						rs.executeProc("SystemSet_Select","");
						if(rs.next()){
							if(subcompanyid.length()==0||subcompanyid.equals("0")){
								defaultSubcompanyid = Util.null2String(rs.getString("dftsubcomid"));
							}
						}
					}

					boolean hasRight = false;
					for (int i = 0; subcomids!=null&& i < subcomids.length; i++) {
						if((""+subcomids[i]).equals(defaultSubcompanyid)){
							hasRight = true;
							break;
						}
					}

					if(!hasRight){
						defaultSubcompanyid = "";
					}

					hrmFieldBean = new HrmFieldBean();
					hrmFieldBean.setFieldname("subcompanyid");
					hrmFieldBean.setFieldlabel("19799");
					hrmFieldBean.setFieldhtmltype("3");
					hrmFieldBean.setType("169");
					hrmFieldBean.setFieldvalue(subcompanyid.length()==0&&id.length()==0?defaultSubcompanyid:subcompanyid);
					hrmFieldBean.setViewAttr(3);
					hrmFieldBean.setRules("required|integer");
					lsField.add(hrmFieldBean);
				}

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("kqtype");
				hrmFieldBean.setFieldlabel("388704");
				hrmFieldBean.setFieldhtmltype("5");
				hrmFieldBean.setType("3");
				hrmFieldBean.setFieldvalue(kqtype);

				options = new ArrayList<SearchConditionOption>();
				options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(500385, user.getLanguage()),kqtype.equals("1")));
				options.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(500386, user.getLanguage()),kqtype.equals("2")));
				options.add(new SearchConditionOption("3",SystemEnv.getHtmlLabelName(500387, user.getLanguage()),kqtype.equals("3")));
				hrmFieldBean.setSelectOption(options);
				hrmFieldBean.setViewAttr(id.length()>0?1:2);
				lsField.add(hrmFieldBean);

				//固定班制
				retmap.put("fixedSchedulce",getFixedSchedulce());
				//排班制
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("serialids");
				hrmFieldBean.setFieldlabel("389098");
				hrmFieldBean.setFieldhtmltype("3");
				hrmFieldBean.setType("mkqshift");
				hrmFieldBean.setFieldvalue(serialids);
				lsField.add(hrmFieldBean);
				retmap.put("shiftSchedulceTable",getShiftSchedulceTable());

				//自由班制
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("weekday");
				hrmFieldBean.setFieldlabel("389097");
				hrmFieldBean.setFieldhtmltype("5");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(weekday);
				options = new ArrayList<SearchConditionOption>();
				for(int i=0;i<7;i++){
					options.add(new SearchConditionOption(""+i,UtilKQ.getWeekDay(i,user.getLanguage()),lsWeekday.contains(i)));
				}
				hrmFieldBean.setSelectOption(options);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("signstart");
				hrmFieldBean.setFieldlabel("16039");
				hrmFieldBean.setFieldhtmltype("3");
				hrmFieldBean.setType("19");
				hrmFieldBean.setFieldvalue(signstart);
				hrmFieldBean.setViewAttr(3);
				hrmFieldBean.setRules("required|string");
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("workhour");
				hrmFieldBean.setFieldlabel("390053");
				hrmFieldBean.setFieldhtmltype("1");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(workhour);
				hrmFieldBean.setViewAttr(3);
				hrmFieldBean.setRules("required|numeric");
				otherparam = new HashMap<String, Object>();
				otherparam.put("min","1");
				otherparam.put("precision",1);
				otherparam.put("max","24");
				hrmFieldBean.setOtherparam(otherparam);
				lsField.add(hrmFieldBean);

        if(id.length()>0){
//          编辑的时候只有弹性的显示这个说明
          if(kqtype.equals("3")){
            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("calmethod");
            hrmFieldBean.setFieldlabel("529683");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("3");
            hrmFieldBean.setFieldvalue(calmethod);

            options = new ArrayList<SearchConditionOption>();
            options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(529686, user.getLanguage()),calmethod.equals("1")));
            options.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(529685, user.getLanguage()),calmethod.equals("2")));
            hrmFieldBean.setSelectOption(options);
            hrmFieldBean.setViewAttr(3);
            lsField.add(hrmFieldBean);
          }
        }else{

          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("calmethod");
          hrmFieldBean.setFieldlabel("529683");
          hrmFieldBean.setFieldhtmltype("5");
          hrmFieldBean.setType("3");
          hrmFieldBean.setFieldvalue(calmethod);

          options = new ArrayList<SearchConditionOption>();
          options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(529686, user.getLanguage()),calmethod.equals("1")));
          options.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(529685, user.getLanguage()),calmethod.equals("2")));
          hrmFieldBean.setSelectOption(options);
          hrmFieldBean.setViewAttr(3);
          lsField.add(hrmFieldBean);
        }

        if(id.length()>0){
//          编辑的时候只有弹性的显示这个说明
          if(kqtype.equals("3")){
            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("desc");
            hrmFieldBean.setFieldlabel("85");
            hrmFieldBean.setFieldhtmltype("8");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(SystemEnv.getHtmlLabelName(520468, Util.getIntValue(user.getLanguage()))
                +"<br/>"+SystemEnv.getHtmlLabelName(520469, Util.getIntValue(user.getLanguage()))
                +"<br/>"+SystemEnv.getHtmlLabelName(520859, Util.getIntValue(user.getLanguage())));
            hrmFieldBean.setViewAttr(1);
            lsField.add(hrmFieldBean);
          }
        }else{
          //新增的时候需要前端控制显示这个字段
          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("desc");
          hrmFieldBean.setFieldlabel("85");
          hrmFieldBean.setFieldhtmltype("8");
          hrmFieldBean.setType("1");
          hrmFieldBean.setFieldvalue(SystemEnv.getHtmlLabelName(520468, Util.getIntValue(user.getLanguage()))
              +"<br/>"+SystemEnv.getHtmlLabelName(520469, Util.getIntValue(user.getLanguage()))
              +"<br/>"+SystemEnv.getHtmlLabelName(520859, Util.getIntValue(user.getLanguage())));
          hrmFieldBean.setViewAttr(1);
          lsField.add(hrmFieldBean);
        }

				fieldGroups.put("1361", lsField);
			}else if(tabKey.equals("2")){
				//signintype,ipscope,locationcheck,locationcheckscope,wificheck
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("signintype");
				hrmFieldBean.setFieldlabel("388708");
				hrmFieldBean.setFieldhtmltype("5");
				hrmFieldBean.setType("3");
				hrmFieldBean.setFieldvalue(signintype);
				options = new ArrayList<SearchConditionOption>();
				options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(389938, user.getLanguage()),signintype.equals("1")));
				options.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(388710, user.getLanguage()),signintype.equals("2")));
				options.add(new SearchConditionOption("3",SystemEnv.getHtmlLabelName(389939, user.getLanguage()),signintype.equals("3")));
				options.add(new SearchConditionOption("4",SystemEnv.getHtmlLabelName(502765, user.getLanguage()),signintype.equals("4")));
				hrmFieldBean.setSelectOption(options);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("ipscope_v4_pc");
				hrmFieldBean.setFieldlabel("388712");
				hrmFieldBean.setFieldlabelname(SystemEnv.getHtmlLabelName(388712, Util.getIntValue(user.getLanguage()))+"( ipv4 )");
				hrmFieldBean.setFieldhtmltype("1");
				hrmFieldBean.setType("1");
				hrmFieldBean.setFieldvalue(ipscope_v4_pc);
				lsField.add(hrmFieldBean);

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("ipscope_v4_em");
        hrmFieldBean.setFieldlabel("388712");
        hrmFieldBean.setFieldlabelname(SystemEnv.getHtmlLabelName(388712, Util.getIntValue(user.getLanguage()))+"( ipv4 )");
        hrmFieldBean.setFieldhtmltype("1");
        hrmFieldBean.setType("1");
        hrmFieldBean.setFieldvalue(ipscope_v4_em);
        lsField.add(hrmFieldBean);

//        hrmFieldBean = new HrmFieldBean();
//        hrmFieldBean.setFieldname("ipscope_v6_pc");
//        hrmFieldBean.setFieldlabel("388712");
//        hrmFieldBean.setFieldlabelname(SystemEnv.getHtmlLabelName(388712,weaver.general.Util.getIntValue(user.getLanguage()))+"( ipv6 )");
//        hrmFieldBean.setFieldhtmltype("1");
//        hrmFieldBean.setType("1");
//        hrmFieldBean.setFieldvalue(ipscope_v6_pc);
//        lsField.add(hrmFieldBean);
//
//        hrmFieldBean = new HrmFieldBean();
//        hrmFieldBean.setFieldname("ipscope_v6_em");
//        hrmFieldBean.setFieldlabel("388712");
//        hrmFieldBean.setFieldlabelname(SystemEnv.getHtmlLabelName(388712,weaver.general.Util.getIntValue(user.getLanguage()))+"( ipv6 )");
//        hrmFieldBean.setFieldhtmltype("1");
//        hrmFieldBean.setType("1");
//        hrmFieldBean.setFieldvalue(ipscope_v6_em);
//        lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("outsidesign");
				hrmFieldBean.setFieldlabel("390302");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(outsidesign);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("locationcheck");
				hrmFieldBean.setFieldlabel("388713");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(locationcheck);
				lsField.add(hrmFieldBean);

//				hrmFieldBean = new HrmFieldBean();
//				hrmFieldBean.setFieldname("locationcheckscope");
//				hrmFieldBean.setFieldlabel("388714");
//				hrmFieldBean.setFieldhtmltype("5");
//				hrmFieldBean.setType("1");
//				hrmFieldBean.setFieldvalue(locationcheckscope);
//				options = new ArrayList<SearchConditionOption>();
//				options.add(new SearchConditionOption("50","50"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("50")));
//				options.add(new SearchConditionOption("100","100"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("100")));
//				options.add(new SearchConditionOption("200","200"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("200")));
//				options.add(new SearchConditionOption("300","300"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("300")));
//				options.add(new SearchConditionOption("400","400"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("400")));
//				options.add(new SearchConditionOption("500","500"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("500")));
//				options.add(new SearchConditionOption("600","600"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("600")));
//				options.add(new SearchConditionOption("700","700"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("700")));
//				options.add(new SearchConditionOption("800","800"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("800")));
//				options.add(new SearchConditionOption("900","900"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("900")));
//				options.add(new SearchConditionOption("1000","1000"+SystemEnv.getHtmlLabelName(125675, user.getLanguage()),locationcheckscope.equals("1000")));
//				hrmFieldBean.setSelectOption(options);
//				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("locationfacecheck");
				hrmFieldBean.setFieldlabel("507921");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("1");
				hrmFieldBean.setFieldvalue(locationfacecheck);
				/*hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(507989,user.getLanguage()));*/
				lsField.add(hrmFieldBean);

				//qc2473809 增加办公室人脸打卡类型
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("locationfacechecktype");
				hrmFieldBean.setFieldlabel("-2023817018");
				hrmFieldBean.setFieldhtmltype("5");
				hrmFieldBean.setType("1");
				hrmFieldBean.setFieldvalue(locationfacechecktype);
				options = new ArrayList<SearchConditionOption>();
				options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(-2023817020,user.getLanguage())));
				if("1".equals(settings.getSecondCL())){
					options.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(-2023817019,user.getLanguage())));
				}

				hrmFieldBean.setSelectOption(options);
				hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(-2023817017,user.getLanguage()));
				lsField.add(hrmFieldBean);



				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("locationshowaddress");
				hrmFieldBean.setFieldlabel("507922");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(locationshowaddress);
				hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(507990,user.getLanguage()));
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("wificheck");
				hrmFieldBean.setFieldlabel("388715");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("2");
				hrmFieldBean.setFieldvalue(wificheck);
				lsField.add(hrmFieldBean);

				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("wififacecheck");
				hrmFieldBean.setFieldlabel("507921");
				hrmFieldBean.setFieldhtmltype("4");
				hrmFieldBean.setType("1");
				hrmFieldBean.setFieldvalue(wififacecheck);
				lsField.add(hrmFieldBean);

				//qc2473809 这里设置下拉框选项
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname("wififacechecktype");
				hrmFieldBean.setFieldlabel("-2023817018");
				hrmFieldBean.setFieldhtmltype("5");
				hrmFieldBean.setType("1");
				hrmFieldBean.setFieldvalue(wififacechecktype);
				options = new ArrayList<SearchConditionOption>();
				options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(-2023817020,user.getLanguage())));
				if("1".equals(settings.getSecondCL())){
					options.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(-2023817019,user.getLanguage())));
				}

				hrmFieldBean.setSelectOption(options);
				hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(-2023817017,user.getLanguage()));
				lsField.add(hrmFieldBean);

				if(!"3".equalsIgnoreCase(kqtype)){
          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("auto_checkin");
          hrmFieldBean.setFieldlabel("518736");
          hrmFieldBean.setFieldhtmltype("4");
          hrmFieldBean.setType("2");
          hrmFieldBean.setFieldvalue(Util.null2s(auto_checkin,"0"));
          hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(521383,user.getLanguage()));
          lsField.add(hrmFieldBean);

          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("auto_checkin_before");
          hrmFieldBean.setFieldlabel("518737");
          hrmFieldBean.setFieldhtmltype("1");
          hrmFieldBean.setType("2");
          hrmFieldBean.setFieldvalue(Util.null2s(auto_checkin_before,"30"));
          hrmFieldBean.setRules("required|integer");
          otherparam = new HashMap<String, Object>();
          otherparam.put("min","1");
          hrmFieldBean.setOtherparam(otherparam);
          lsField.add(hrmFieldBean);

          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("auto_checkin_after");
          hrmFieldBean.setFieldlabel("518737");
          hrmFieldBean.setFieldhtmltype("1");
          hrmFieldBean.setType("2");
          hrmFieldBean.setFieldvalue(Util.null2s(auto_checkin_after,"30"));
          hrmFieldBean.setRules("required|integer");
          otherparam = new HashMap<String, Object>();
          otherparam.put("min","0");
          hrmFieldBean.setOtherparam(otherparam);
          lsField.add(hrmFieldBean);

          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("auto_checkout");
          hrmFieldBean.setFieldlabel("518738");
          hrmFieldBean.setFieldhtmltype("4");
          hrmFieldBean.setType("2");
          hrmFieldBean.setFieldvalue(Util.null2s(auto_checkout,"0"));
          hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(521383,user.getLanguage()));
          lsField.add(hrmFieldBean);

          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("auto_checkout_before");
          hrmFieldBean.setFieldlabel("518739");
          hrmFieldBean.setFieldhtmltype("1");
          hrmFieldBean.setType("2");
          hrmFieldBean.setFieldvalue(Util.null2s(auto_checkout_before,"5"));
          hrmFieldBean.setRules("required|integer");
          otherparam = new HashMap<String, Object>();
          otherparam.put("min","0");
          hrmFieldBean.setOtherparam(otherparam);
          lsField.add(hrmFieldBean);

          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname("auto_checkout_after");
          hrmFieldBean.setFieldlabel("518739");
          hrmFieldBean.setFieldhtmltype("1");
          hrmFieldBean.setType("2");
          hrmFieldBean.setFieldvalue(Util.null2s(auto_checkout_after,"60"));
          hrmFieldBean.setRules("required|integer");
          otherparam = new HashMap<String, Object>();
          otherparam.put("min","0");
          hrmFieldBean.setOtherparam(otherparam);
          lsField.add(hrmFieldBean);
        }

        fieldGroups.put("20331", lsField);
        retmap.put("locationSessionKey",getKQLocationList(""));
        retmap.put("wifiSessionKey",getKQWifiList(""));
        Map<String,Object> helpTipMap = Maps.newHashMap();
        helpTipMap.put("auto_checkin_before", SystemEnv.getHtmlLabelName(521211, user.getLanguage()));
        helpTipMap.put("auto_checkout_before", SystemEnv.getHtmlLabelName(521211, user.getLanguage()));
        retmap.put("helpTipMap",helpTipMap);

			}

			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			SearchConditionItem searchConditionItem = null;
			Iterator<Map.Entry<String, List<HrmFieldBean>>> iter = fieldGroups.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, List<HrmFieldBean>> entry = iter.next();
				String grouplabel = entry.getKey();
				List<HrmFieldBean> fields = entry.getValue();
				groupitem = new HashMap<String, Object>();
				groupitem.put("title", SystemEnv.getHtmlLabelNames(grouplabel, user.getLanguage()));
				groupitem.put("defaultshow", true);

				itemlist = new ArrayList<Object>();
				for (int j = 0; j < fields.size(); j++) {
					hrmFieldBean = fields.get(j);
					searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
					if(hrmFieldBean.getFieldname().equals("subcompanyid")){
						searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "HrmKQGroup:Add");
						searchConditionItem.getBrowserConditionParam().getCompleteParams().put("rightStr", "HrmKQGroup:Add");
					}
					itemlist.add(searchConditionItem);
				}
				groupitem.put("items", itemlist);
				grouplist.add(groupitem);
			}
			retmap.put("formField", grouplist);
			retmap.put("status", "1");
			int operatelevel = -1;
			if(hrmdetachable){
				if(subcompanyid.length()>0 && !subcompanyid.equalsIgnoreCase("0")){
					CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
					operatelevel=checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"HrmKQGroup:Add",Util.getIntValue(subcompanyid,-1));
				}
			}else{
				operatelevel = 2;
			}

			if(user.getUID() == 1 || subcompanyid.length()==0 || subcompanyid.equalsIgnoreCase("0")){
				operatelevel = 2;
			}
			if(operatelevel > 0){
				retmap.put("canAdd", true);
			}else{
				retmap.put("canAdd", false);
			}
      retmap.put("kqtype", kqtype);
    }catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
	}

	public String getKQLocationList(String locationname){
		String sessionKey = "";
		String groupId = Util.null2String(params.get("id"));
		String backFields = " id,locationname,longitude,latitude,address,checkscope  ";
		String sqlFrom  = " kq_location ";
		String sqlWhere = " where groupid= "+groupId;
		String orderby = " id " ;
		String tableString = "";

    if (locationname.length() > 0){
      sqlWhere += " and locationname like '%"+locationname+"%' ";
    }

		String pageUid = PageUidFactory.getHrmPageUid("KQLocationList");

		tableString=""+
						"<table pageUid=\""+pageUid+"\" pagesize=\"10\" tabletype=\"checkbox\">"+
						"<sql backfields=\""+backFields+"\" sqlform=\""+sqlFrom+"\" sqlprimarykey=\"id\" sqlorderby=\""+orderby+"\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"/>"+
						"<head>"+
						"				<col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(388717,user.getLanguage())+"\" column=\"locationname\"/>"+
						"				<col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(801,user.getLanguage())+"\" column=\"longitude\" orderkey=\"longitude\"/>"+
						"				<col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(802,user.getLanguage())+"\" column=\"latitude\" orderkey=\"latitude\"/>"+
						"				<col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(507863,user.getLanguage())+"\" column=\"checkscope\" orderkey=\"checkscope\"/>"+
						"				<col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(104,user.getLanguage())+"\" column=\"id\" orderkey=\"id\"/>"+
            "		    <col width=\"0%\"  hide=\"true\" display=\"true\"   text=\"\" column=\"address\"/>"+
						"</head>"+
						"</table>";

		//主要用于 显示定制列以及 表格 每页展示记录数选择
		sessionKey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
		Util_TableMap.setVal(sessionKey, tableString);

		return sessionKey;
	}

	public String getKQWifiList(String wifiname){
		String sessionKey = "";
		String groupId = Util.null2String(params.get("id"));
		String backFields = " id,wifiname,mac  ";
		String sqlFrom  = " kq_wifi ";
		String sqlWhere = " where groupid= "+groupId;
		String orderby = " id " ;
		String tableString = "";

    if (wifiname.length() > 0){
      sqlWhere += " and wifiname like '%"+wifiname+"%' ";
    }

		String pageUid = PageUidFactory.getHrmPageUid("KQLocationList");

		tableString=""+
						"<table pageUid=\""+pageUid+"\" pagesize=\"10\" tabletype=\"checkbox\">"+
						"<sql backfields=\""+backFields+"\" sqlform=\""+sqlFrom+"\" sqlprimarykey=\"id\" sqlorderby=\""+orderby+"\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"/>"+
						"<head>"+
						"				<col width=\"35%\" text=\""+SystemEnv.getHtmlLabelName(195,user.getLanguage())+"\" column=\"wifiname\"/>"+
						"				<col width=\"35%\" text=\""+SystemEnv.getHtmlLabelName(32072,user.getLanguage())+"\" column=\"mac\"/>"+
						"				<col width=\"30%\" text=\""+SystemEnv.getHtmlLabelName(104,user.getLanguage())+"\" column=\"id\" orderkey=\"id\"/>"+
						"</head>"+
						"</table>";

		//主要用于 显示定制列以及 表格 每页展示记录数选择
		sessionKey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
		Util_TableMap.setVal(sessionKey, tableString);

		return sessionKey;
	}

	/**
	 * 固定班制
	 * @return
	 */
	public Map<String,Object> getFixedSchedulce(){
		Map<String,Object> fixedSchedulce = new HashMap<>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{
			String groupid = Util.null2String(params.get("id"));
			//固定班次
			Map<String, Object> table = new HashMap<String, Object>();
			List<Object> columns = new ArrayList<Object>();
			Map<String, Object> column = null;
			LinkedList<Map<String, Object>> datas = new LinkedList<Map<String, Object>>();
			Map<String, Object> data = null;
			ShiftManagementToolKit shiftManagementToolKit = new ShiftManagementToolKit();

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(28387, user.getLanguage()));
			column.put("dataIndex", "weekday");
			column.put("width", "30%");
			columns.add(column);

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(388984, user.getLanguage()));
			column.put("dataIndex", "serialinfo");
			column.put("width", "40%");
			columns.add(column);

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(104, user.getLanguage()));
			column.put("dataIndex", "operate");
			column.put("width", "30%");
			columns.add(column);

			if(groupid.length()>0){
				List<Integer> lsWeeks = new ArrayList<>();

				sql = " select * from kq_fixedschedulce where groupid = ? order by weekday asc ";
				rs.executeQuery(sql,groupid);
				while(rs.next()){
					data = new HashMap<String, Object>();
					data.put("id",rs.getString("id"));
					data.put("weekday",UtilKQ.getWeekDay(rs.getInt("weekday"),user.getLanguage()));
					data.put("serialid",rs.getString("serialid"));
					data.put("serialinfo",shiftManagementToolKit.getShiftOnOffWorkSections(rs.getString("serialid"),user.getLanguage()));
					datas.add(data);
					lsWeeks.add(rs.getInt("weekday"));
				}

				//加强性修改
				for(int i=0;i<7;i++){
					if(!lsWeeks.contains(i)){
						data = new HashMap<String, Object>();
						data.put("id",i);
						data.put("weekday",UtilKQ.getWeekDay(i,user.getLanguage()));
						data.put("serialid","");
						data.put("serialinfo",SystemEnv.getHtmlLabelName(26593, user.getLanguage()));
						datas.add(data);
					}
				}
			}else{
				for(int i=0;i<7;i++){
					data = new HashMap<String, Object>();
					data.put("id",i);
					data.put("weekday",UtilKQ.getWeekDay(i,user.getLanguage()));
					data.put("serialid","");
					data.put("serialinfo",SystemEnv.getHtmlLabelName(26593, user.getLanguage()));
					datas.add(data);
				}
			}
			fixedSchedulce.put("columns",columns);
			fixedSchedulce.put("datas",datas);

		}catch (Exception e){
			writeLog(e);
		}
		return fixedSchedulce;
	}

	/**
	 * 排班制
	 * @return
	 */
	public Map<String,Object> getShiftSchedulceTable(){
		Map<String,Object> shiftSchedulce = new HashMap<>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{
			String groupid = Util.null2String(params.get("id"));
			//排班制
			List<Object> columns = new ArrayList<Object>();
			Map<String, Object> column = null;
			LinkedList<Map<String, Object>> datas = new LinkedList<Map<String, Object>>();
			Map<String, Object> data = null;

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(388722, user.getLanguage()));
			column.put("dataIndex", "shiftcyclename");
			column.put("width", "30%");
			columns.add(column);

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(500480, user.getLanguage()));
			column.put("dataIndex", "serial");
			column.put("width", "30%");
			columns.add(column);

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(500481, user.getLanguage()));
			column.put("dataIndex", "shiftcycleday");
			column.put("width", "20%");
			columns.add(column);

			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(30585, user.getLanguage()));
			column.put("dataIndex", "operate");
			column.put("width", "20%");
			columns.add(column);

			if(groupid.length()>0){
				sql = " select id,shiftcyclename,shiftcycleserialids,groupid from kq_group_shiftcycle where groupid = ? order by id asc ";
				rs.executeQuery(sql,groupid);
				while(rs.next()){
					data = new HashMap<String, Object>();
					data.put("id",rs.getString("id"));
					data.put("shiftcyclename",rs.getString("shiftcyclename"));
					data.put("serial",rs.getString("shiftcycleserialids"));
					data.put("shiftcycleday",Util.splitString(Util.null2String(rs.getString("shiftcycleserialids")),",").length);
					datas.add(data);
				}
			}
			shiftSchedulce.put("columns",columns);
			shiftSchedulce.put("datas",datas);
		}catch (Exception e){
			writeLog(e);
		}
		return shiftSchedulce;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

}
