package com.engine.kq.cmd.report;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.bean.WeaRadioGroup;
import com.api.hrm.util.HrmAdvancedSearchUtil;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.common.service.HrmCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.biz.KQReportFieldComInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.systeminfo.SystemEnv;

/**
 * 获取排班查询条件
 * @author pzy
 *
 */
public class GetSearchConditionCmd extends AbstractCommonCommand<Map<String, Object>>{

	public GetSearchConditionCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {Map<String,Object> retmap = new HashMap<String,Object>();
		List<Object> lsCondition = new ArrayList<Object>();
    List<SearchConditionItem> cusCondition = new ArrayList<SearchConditionItem>();
		String[] options = null;
		String[] selectLinkageDatas = null;
		try{
			HrmAdvancedSearchUtil hrmAdvancedSearchUtil = new HrmAdvancedSearchUtil();
			HrmCommonService hrmCommonService = new HrmCommonServiceImpl();

			String reportType = Util.null2String(params.get("reportType"));
			if(reportType.length()==0)reportType = "month";

			//展示列、时间范围、数据范围
			WeaRadioGroup wrg = null;
			options = new String[]{"1,15537,false","2,15539,false","3,15541,true","7,27347,false","4,21904,false","5,15384,false","8,81716,false","6,32530,false"};
			wrg = hrmAdvancedSearchUtil.getAdvanceCondition("typeselect","19482",options,null,user);
			wrg.setLabelcol(3);
			wrg.setFieldcol(20);
			Map<String,Object> selectLinks = new HashMap<String, Object>();
			List<String>  domkey =  new  ArrayList<String>();
			Map<String,Object> map = new HashMap<String, Object>();
			selectLinks.put("conditionType", "RANGEPICKER");
			domkey = new  ArrayList<String>();
			domkey.add("fromDate");
			domkey.add("toDate");
			selectLinks.put("domkey", domkey);
			map.put("6", selectLinks);
			wrg.setSelectLinkageDatas(map);
			lsCondition.add(wrg);

			if(hrmCommonService.isManager(user.getUID())){
				options = new String[]{"0,140,true","1,141,false","2,124,false","3,1867,false","4,15089,false"};
			}else{
				options = new String[]{"0,140,true","1,141,false","2,124,false","3,1867,false"};
			}

			selectLinkageDatas = new String[]{"1,subCompanyId,141,3,194","2,departmentId,124,3,57","3,resourceId,1867,3,17","4,allLevel,389995,4,1"};
			if(user.getUID()!=1){
				new BaseBean().writeLog("==zj==(考勤汇总报表默认查询子账号开关)" + new BaseBean().getPropValue("qc2280379","isChildOpen"));
				if("1".equals(new BaseBean().getPropValue("qc2280379","isChildOpen"))){
					selectLinkageDatas[2]="3,resourceId,1867,3,17,"+user.getUID()+","+true;
				}else {
					selectLinkageDatas[2]="3,resourceId,1867,3,17,"+user.getUID();
				}
				new BaseBean().writeLog("==zj==(selectLinkageDatas)" + JSON.toJSONString(selectLinkageDatas[2]));
				options[0]="0,140,false";
				options[3]="3,1867,true";
			}
			wrg = hrmAdvancedSearchUtil.getAdvanceCondition("viewScope","34102",options,selectLinkageDatas,user);
			wrg.setLabelcol(3);
			wrg.setFieldcol(20);
			lsCondition.add(wrg);

      WeaRadioGroup wrg1 = new WeaRadioGroup();

      ChgPasswdReminder reminder = new ChgPasswdReminder();
      RemindSettings settings = reminder.getRemindSettings();
      String checkUnJob = Util.null2String(settings.getCheckUnJob(), "0");
      List<String> statusList = Lists.newArrayList();
      if ("1".equals(checkUnJob)) {//启用后，只有有“离职人员查看”权限的用户才能检索非在职人员
        if (HrmUserVarify.checkUserRight("hrm:departureView", user)) {
          statusList.add("9,332,false");
        }
      } else {
        statusList.add("9,332,false");
      }
      statusList.add("0,15710,false");
      statusList.add("1,15711,false");
      statusList.add("2,480,false");
      statusList.add("3,15844,false");
      if ("1".equals(checkUnJob)) {//启用后，只有有“离职人员查看”权限的用户才能检索非在职人员
        if (HrmUserVarify.checkUserRight("hrm:departureView", user)) {
          statusList.add("4,6094,false");
          statusList.add("5,6091,false");
          statusList.add("6,6092,false");
          statusList.add("7,2245,false");
        }
      } else {
        statusList.add("4,6094,false");
        statusList.add("5,6091,false");
        statusList.add("6,6092,false");
        statusList.add("7,2245,false");
      }
      statusList.add("8,1831,true");
      options = new String[statusList.size()];
      for(int i = 0 ; i < statusList.size() ; i++){
        String statusStr = statusList.get(i);
        options[i] = statusStr;
      }

      wrg1 = hrmAdvancedSearchUtil.getAdvanceCondition("status","602",options,null,user);
      wrg1.setLabelcol(3);
      wrg1.setFieldcol(20);
      lsCondition.add(wrg1);

			KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
			List<Map<String, Object>> leaveRules = kqLeaveRulesBiz.getAllLeaveRules();
			List<Object> showColumns = new ArrayList<>();
			String cascadekey = "";
			List<Map<String,Object>> selectOptions = new ArrayList<>();
			Map<String,Object> selectOption = null;

			Map<String,Object> mapShowColumns = new HashMap<>();
			List<String> lsCascadekey = null;
			if(reportType.equals("month")){
				KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
				while (kqReportFieldComInfo.next()){
					if(!kqReportFieldComInfo.getIsDefinedColumn().equals("1"))continue;
					if(!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("month"))continue;
          if("overtime_nonleave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname()) || "overtime_4leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())){
            continue;
          }
					if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0){
						continue;
					}
					lsCascadekey = null;
					if(Util.null2String(kqReportFieldComInfo.getCascadekey()).length()>0){
						lsCascadekey = Util.splitString2List(kqReportFieldComInfo.getCascadekey(),",");
					}
					if(lsCascadekey !=null && lsCascadekey.size()>0) {
						mapShowColumns.put(kqReportFieldComInfo.getFieldname(), lsCascadekey);
					}
				}
				retmap.put("showColumns", mapShowColumns);
			}else{
				//考勤日历考
				KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
				while (kqReportFieldComInfo.next()){
					if(!kqReportFieldComInfo.getIsDefinedColumn().equals("1"))continue;
					if(!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("daily"))continue;
          if("overtime_nonleave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname()) || "overtime_4leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())){
            continue;
          }
					if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0){
						continue;
					}
					cascadekey = "";
					if(kqReportFieldComInfo.getFieldname().equals("beLate")){
						showColumns.add(selectOptions);
						selectOptions = new ArrayList<>();
						cascadekey = "beLateMins";
					}else if(kqReportFieldComInfo.getFieldname().equals("leaveEearly")){
						cascadekey = "leaveEarlyMins";
					}else if(kqReportFieldComInfo.getFieldname().equals("graveBeLate")){
						cascadekey = "graveBeLateMins";
					}else if(kqReportFieldComInfo.getFieldname().equals("graveLeaveEarly")){
						cascadekey = "graveLeaveEarlyMins";
					}else if(kqReportFieldComInfo.getFieldname().equals("absenteeism")){
						cascadekey = "absenteeismMins";
					}else if(kqReportFieldComInfo.getFieldname().equals("signin1")){
						cascadekey = "signout1";
					}else if(kqReportFieldComInfo.getFieldname().equals("signin2")){
						cascadekey = "signout2";
					}else if(kqReportFieldComInfo.getFieldname().equals("signin3")){
						cascadekey = "signout3";
					}

					selectOption = new HashMap<>();
					selectOption.put("key",kqReportFieldComInfo.getFieldname());
					selectOption.put("cascadekey",cascadekey);
					selectOption.put("showname",SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(), user.getLanguage()));
					selectOption.put("selected",kqReportFieldComInfo.getDefaultShow().equals("1"));
					selectOptions.add(selectOption);
				}
				showColumns.add(selectOptions);
				retmap.put("showColumns", showColumns);
			}

      createSearchConditionItemList(cusCondition);

			retmap.put("status", "1");
			retmap.put("conditions", lsCondition);
      retmap.put("cusCondition", cusCondition);
		}catch (Exception e) {
			writeLog(e);
			retmap.put("status", "-1");
			retmap.put("message", ""+ SystemEnv.getHtmlLabelName(10004510,weaver.general.ThreadVarLanguage.getLang())+"");
		}
		return retmap;
	}

  /**
   * 这里只处理非下拉框字段
   * @param cusCondition
   */
  public void createSearchConditionItemList(List<SearchConditionItem> cusCondition) {

  }

  @Override
	public BizLogContext getLogContext() {
		return null;
	}

}
