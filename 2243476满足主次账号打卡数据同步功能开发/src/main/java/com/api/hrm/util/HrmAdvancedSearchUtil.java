package com.api.hrm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.api.browser.bean.BrowserBean;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.bean.SelectOption;
import com.api.hrm.bean.WeaRadioGroup;

import com.customization.qc2243476.KqUtil;
import weaver.filter.XssUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.systeminfo.SystemEnv;

public class HrmAdvancedSearchUtil {

	
/*
 *时间：不限，本周，本月，上个月，本季，本年，上一年，指定日期范围 
 */
	
  public WeaRadioGroup  getAdvanceDate(String label,User user){
	  
		List<Object> option = new ArrayList<Object>();
		Map<String,Object> selectLinks = new HashMap<String, Object>();
		List<String>  domkey =  new  ArrayList<String>();
		Map<String,Object> map = new HashMap<String, Object>();
		WeaRadioGroup   wrg = new WeaRadioGroup();
		wrg.setLabel(label);
		
		option.add(new SelectOption("0",SystemEnv.getHtmlLabelName(763, user.getLanguage()) ,true));  
		option.add(new SelectOption("2",SystemEnv.getHtmlLabelName(15539, user.getLanguage())));
		option.add(new SelectOption("3",SystemEnv.getHtmlLabelName(15541, user.getLanguage())));
		option.add(new SelectOption("7",SystemEnv.getHtmlLabelName(27347, user.getLanguage())));
		option.add(new SelectOption("4",SystemEnv.getHtmlLabelName(21904, user.getLanguage())));
		option.add(new SelectOption("5",SystemEnv.getHtmlLabelName(15384, user.getLanguage())));
		option.add(new SelectOption("8",SystemEnv.getHtmlLabelName(81716, user.getLanguage())));
		option.add(new SelectOption("6",SystemEnv.getHtmlLabelName(32530, user.getLanguage())));

		wrg.setOptions(option);
		
		domkey.add("dateselect");
		wrg.setDomkey(domkey);
		
		selectLinks.put("conditionType", "RANGEPICKER");
		selectLinks.put("viewAttr", 3);
		domkey = new  ArrayList<String>();
		domkey.add("fromdate");
		domkey.add("enddate");
		selectLinks.put("domkey", domkey);
		map.put("6", selectLinks);
		wrg.setSelectLinkageDatas(map);
		
	return wrg;
  }
  
	
/*
*日期：本年，上一年，指定年份 
*/
  public WeaRadioGroup  getAdvanceDate2(String label,User user){
	  
		List<Object> option = new ArrayList<Object>();
		Map<String,Object> selectLinks = new HashMap<String, Object>();
		List<String>  domkey =  new  ArrayList<String>();
		Map<String,Object> map = new HashMap<String, Object>();
		
		
		WeaRadioGroup   wrg = new WeaRadioGroup();
		wrg.setLabel(label);
		
		option.add(new SelectOption("5",SystemEnv.getHtmlLabelName(15384, user.getLanguage()),true));
		option.add(new SelectOption("8",SystemEnv.getHtmlLabelName(81716, user.getLanguage())));
		option.add(new SelectOption("6",SystemEnv.getHtmlLabelName(385642, Util.getIntValue(user.getLanguage()))));
		wrg.setOptions(option);
		
		domkey.add("dateselect");
		wrg.setDomkey(domkey);
		
		selectLinks.put("conditionType", "DATEPICKER");     
		selectLinks.put("viewAttr", 3);
		selectLinks.put("format", "YYYY");
		domkey = new  ArrayList<String>();
		domkey.add("year");
		selectLinks.put("domkey", domkey);
		map.put("6", selectLinks);
		wrg.setSelectLinkageDatas(map);
	  
	return wrg;
  }
  
  
  /*
  *日期：本年，上一年，指定年份 
  *domkey
  *dateselect
  */
  
  public WeaRadioGroup  getAdvanceDate3(String label,User user,String  domkey,String dateselect){
	  
		List<Object> option = new ArrayList<Object>();
		Map<String,Object> selectLinks = new HashMap<String, Object>();
		List<String>  domkeylist =  new  ArrayList<String>();
		Map<String,Object> map = new HashMap<String, Object>();
		
		
		WeaRadioGroup   wrg = new WeaRadioGroup();
		wrg.setLabel(label);
		
		option.add(new SelectOption("5",SystemEnv.getHtmlLabelName(15384, user.getLanguage()),true));
		option.add(new SelectOption("8",SystemEnv.getHtmlLabelName(81716, user.getLanguage())));
		option.add(new SelectOption("6",SystemEnv.getHtmlLabelName(385642, Util.getIntValue(user.getLanguage()))));
		wrg.setOptions(option);
		
		domkeylist.add(dateselect);
		wrg.setDomkey(domkeylist);
		
		selectLinks.put("conditionType", "DATEPICKER");  
		selectLinks.put("viewAttr", 3);
		selectLinks.put("format", "YYYY");
		domkeylist = new  ArrayList<String>();
		domkeylist.add(domkey);
		selectLinks.put("domkey", domkeylist);
		map.put("6", selectLinks);
		wrg.setSelectLinkageDatas(map);
	  
	return wrg;
  }

	/**
	 * 日期控件：不限、本年、上一年、指定年份
	 * @param label
	 * @param user
	 * @param domkey
	 * @param dateselect
	 * @return
	 */
	public WeaRadioGroup getAdvanceDate4(String label, User user, String domkey, String dateselect) {

		List<Object> option = new ArrayList<Object>();
		Map<String, Object> selectLinks = new HashMap<String, Object>();
		List<String> domkeylist = new ArrayList<String>();
		Map<String, Object> map = new HashMap<String, Object>();


		WeaRadioGroup wrg = new WeaRadioGroup();
		wrg.setLabel(label);

		option.add(new SelectOption("",SystemEnv.getHtmlLabelName(763, user.getLanguage()), true));//不限
		option.add(new SelectOption("5", SystemEnv.getHtmlLabelName(15384, user.getLanguage())));
		option.add(new SelectOption("8", SystemEnv.getHtmlLabelName(81716, user.getLanguage())));
		option.add(new SelectOption("6", SystemEnv.getHtmlLabelName(385642, Util.getIntValue(user.getLanguage()))));
		wrg.setOptions(option);

		domkeylist.add(dateselect);
		wrg.setDomkey(domkeylist);

		selectLinks.put("conditionType", "DATEPICKER");
		selectLinks.put("viewAttr", 3);
		selectLinks.put("format", "YYYY");
		domkeylist = new ArrayList<String>();
		domkeylist.add(domkey);
		selectLinks.put("domkey", domkeylist);
		map.put("6", selectLinks);
		wrg.setSelectLinkageDatas(map);

		return wrg;
	}
  
  
  /*
   * 
   * 工作状态
   * 
   */
  public WeaRadioGroup  getAdvanceStatus(String label,User user){
  
		List<Object> option = new ArrayList<Object>();
		List<String>  domkey =  new  ArrayList<String>();
		WeaRadioGroup   wrg = new WeaRadioGroup();
		wrg.setLabel(label);
		
		option.add(new SelectOption("9",SystemEnv.getHtmlLabelName(332, user.getLanguage())));  
		option.add(new SelectOption("8",SystemEnv.getHtmlLabelName(1831, user.getLanguage()),true));
		option.add(new SelectOption("0",SystemEnv.getHtmlLabelName(15710, user.getLanguage())));
		option.add(new SelectOption("1",SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
		option.add(new SelectOption("2",SystemEnv.getHtmlLabelName(480, user.getLanguage())));
		option.add(new SelectOption("3",SystemEnv.getHtmlLabelName(15844, user.getLanguage())));
		option.add(new SelectOption("4",SystemEnv.getHtmlLabelName(6094, user.getLanguage())));
		option.add(new SelectOption("5",SystemEnv.getHtmlLabelName(6091, user.getLanguage())));
		option.add(new SelectOption("6",SystemEnv.getHtmlLabelName(6092, user.getLanguage())));
		option.add(new SelectOption("7",SystemEnv.getHtmlLabelName(2245, user.getLanguage())));
		wrg.setOptions(option);
		domkey.add("workstatus");
		wrg.setDomkey(domkey);
		return wrg;
  }
  
  
  /*
   * 总部  分部  部门 
   * 
   */
  public WeaRadioGroup  getAdvanceOrg(String label,User user){
		Map<String, Object> selectLinkageDatas = new HashMap<String, Object>();
		Map<String,Object> groupitem = null;
		List<Object> option = new ArrayList<Object>();
		List<String>  domkey =  new  ArrayList<String>();
		WeaRadioGroup   wrg = new WeaRadioGroup();
		wrg.setLabel(label);
		List<Map<String, Object>> replaceDatas = new ArrayList<Map<String, Object>>();
		Map<String, Object>  datas = new HashMap<String, Object>();
		if(user.getUID()!=1){
			
			int subid = user.getUserSubCompany1();
			String subname = "";
			try {
				SubCompanyComInfo dComInfo = new SubCompanyComInfo();
				subname = dComInfo.getSubcompanyname(subid+"");
			} catch (Exception e) {
				new  BaseBean().writeLog(e);
			}
			datas.put("id",subid+"");
			datas.put("name",subname);
			replaceDatas.add(datas);
		}
		
		//名称、类型、排序
		String[] fields = new String[]{"subcompanyid,106,3,164","departmentid,106,3,4"};
		HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
		SearchConditionItem searchConditionItem = null;
		HrmFieldBean hrmFieldBean = null;
		groupitem = new HashMap<String,Object>();
		groupitem.put("title", SystemEnv.getHtmlLabelName(2112, user.getLanguage()));
		groupitem.put("defaultshow", true);
		
		for(int i=0;i<fields.length;i++){
			String[] fieldinfo = fields[i].split(",");
			hrmFieldBean = new HrmFieldBean();
			hrmFieldBean.setFieldname(fieldinfo[0]);
			hrmFieldBean.setFieldlabel(fieldinfo[1]);
			hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
			hrmFieldBean.setType(fieldinfo[3]);
			hrmFieldBean.setIsFormField(true);
			searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
			searchConditionItem.setLabelcol(5);
			searchConditionItem.setFieldcol(10);
			XssUtil xssUtil = new XssUtil();
			searchConditionItem.getBrowserConditionParam().getDataParams().put("show_virtual_org", xssUtil.put("-1"));
			searchConditionItem.getBrowserConditionParam().getCompleteParams().put("show_virtual_org", xssUtil.put("-1"));
			searchConditionItem.getBrowserConditionParam().setIsSingle(false);
			searchConditionItem.getBrowserConditionParam().setViewAttr(3);
			searchConditionItem.setViewAttr(3);
			if(fieldinfo[0].equals("subcompanyid")){
				searchConditionItem.getBrowserConditionParam().setReplaceDatas(replaceDatas);
			}
			selectLinkageDatas.put(""+(i+1),searchConditionItem);
		}
		wrg.setSelectLinkageDatas(selectLinkageDatas);
		
		if(user.getUID()!=1){
			option.add(new SelectOption("0",SystemEnv.getHtmlLabelName(140, user.getLanguage())));
			option.add(new SelectOption("1",SystemEnv.getHtmlLabelName(141, user.getLanguage()),true));
			option.add(new SelectOption("2",SystemEnv.getHtmlLabelName(124, user.getLanguage())));
		}else{
			option.add(new SelectOption("0",SystemEnv.getHtmlLabelName(140, user.getLanguage()),true));
			option.add(new SelectOption("1",SystemEnv.getHtmlLabelName(141, user.getLanguage())));
			option.add(new SelectOption("2",SystemEnv.getHtmlLabelName(124, user.getLanguage())));
		}
		wrg.setOptions(option);
		domkey.add("company");
		wrg.setDomkey(domkey);
		return wrg;
	}
	
  
  
  /*
   * 总部  分部  部门 
   * 
   */
  public WeaRadioGroup  getAdvanceOrg(String label,User user,String[] fields){
		Map<String, Object> selectLinkageDatas = new HashMap<String, Object>();
		Map<String,Object> groupitem = null;
		List<Object> option = new ArrayList<Object>();
		List<String>  domkey =  new  ArrayList<String>();
		WeaRadioGroup   wrg = new WeaRadioGroup();
		wrg.setLabel(label);
		List<Map<String, Object>> replaceDatas = new ArrayList<Map<String, Object>>();
		Map<String, Object>  datas = new HashMap<String, Object>();
		if(user.getUID()!=1){
			
			int subid = user.getUserSubCompany1();
			String subname = "";
			try {
				SubCompanyComInfo dComInfo = new SubCompanyComInfo();
				subname = dComInfo.getSubcompanyname(subid+"");
			} catch (Exception e) {
				new  BaseBean().writeLog(e);
			}
			datas.put("id",subid+"");
			datas.put("name",subname);
			replaceDatas.add(datas);
		}

		
		//名称、类型、排序
	//	String[] fields = new String[]{"subcompanyid,106,3,164","departmentid,106,3,4"};
		HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
		SearchConditionItem searchConditionItem = null;
		HrmFieldBean hrmFieldBean = null;
		groupitem = new HashMap<String,Object>();
		groupitem.put("title", SystemEnv.getHtmlLabelName(2112, user.getLanguage()));
		groupitem.put("defaultshow", true);
		
		for(int i=0;i<fields.length;i++){
			String[] fieldinfo = fields[i].split(",");
			hrmFieldBean = new HrmFieldBean();
			hrmFieldBean.setFieldname(fieldinfo[0]);
			hrmFieldBean.setFieldlabel(fieldinfo[1]);
			hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
			hrmFieldBean.setType(fieldinfo[3]);
			hrmFieldBean.setIsFormField(true);
			searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
			searchConditionItem.setLabelcol(5);
			searchConditionItem.setFieldcol(10);
			XssUtil xssUtil = new XssUtil();
			searchConditionItem.getBrowserConditionParam().getDataParams().put("show_virtual_org", xssUtil.put("-1"));
			searchConditionItem.getBrowserConditionParam().getCompleteParams().put("show_virtual_org", xssUtil.put("-1"));
			searchConditionItem.getBrowserConditionParam().setIsSingle(false);
			searchConditionItem.getBrowserConditionParam().setViewAttr(3);
			if(fieldinfo[0].equals("subcompanyid")||fieldinfo[0].equals("subcompanyIdOut")||fieldinfo[0].equals("subcompanyIdIn")){
				searchConditionItem.getBrowserConditionParam().setReplaceDatas(replaceDatas);
			}
			selectLinkageDatas.put(""+(i+1),searchConditionItem);
		}
		wrg.setSelectLinkageDatas(selectLinkageDatas);
		
		if(user.getUID()!=1){
			option.add(new SelectOption("0",SystemEnv.getHtmlLabelName(140, user.getLanguage())));
			option.add(new SelectOption("1",SystemEnv.getHtmlLabelName(141, user.getLanguage()),true));
			option.add(new SelectOption("2",SystemEnv.getHtmlLabelName(124, user.getLanguage())));
		}else{
			option.add(new SelectOption("0",SystemEnv.getHtmlLabelName(140, user.getLanguage()),true));
			option.add(new SelectOption("1",SystemEnv.getHtmlLabelName(141, user.getLanguage())));
			option.add(new SelectOption("2",SystemEnv.getHtmlLabelName(124, user.getLanguage())));
		}
		wrg.setOptions(option);
		domkey.add("company");
		wrg.setDomkey(domkey);
		return wrg;
	}
  
 
	/**
	 *  工作状态下拉框
	 * */
	public static List<SearchConditionOption> getWorkStatusSelect(int language, String defaultValue){
		if(Util.null2String(defaultValue).length()==0){
			defaultValue = "8";
		}
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
		options.add(new SearchConditionOption("9", SystemEnv.getHtmlLabelName(332, language),"9".equals(defaultValue)));
		options.add(new SearchConditionOption("8", SystemEnv.getHtmlLabelName(1831, language),"8".equals(defaultValue)));
		options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, language),"0".equals(defaultValue)));
		options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, language),"1".equals(defaultValue)));
		options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, language),"2".equals(defaultValue)));
		options.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(15844, language),"3".equals(defaultValue)));
		options.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(6094, language),"4".equals(defaultValue)));
		options.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(6091, language),"5".equals(defaultValue)));
		options.add(new SearchConditionOption("6", SystemEnv.getHtmlLabelName(6092, language),"6".equals(defaultValue)));
		options.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(2245, language),"7".equals(defaultValue)));
		return options;
	}
  
	
	
	
  public WeaRadioGroup  getAdvanceCondition(String fieldname, String label, String[] options ,String[] linkageDatas, User user){
		Map<String, Object> selectLinkageDatas = new HashMap<String, Object>();
		List<Object> option = new ArrayList<Object>();
		List<String> domkey = new  ArrayList<String>();
		WeaRadioGroup wrg = new WeaRadioGroup();
		domkey.add(fieldname);
		wrg.setDomkey(domkey);
		wrg.setLabel(SystemEnv.getHtmlLabelNames(label, user.getLanguage()));
		
		HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
		SearchConditionItem searchConditionItem = null;
		HrmFieldBean hrmFieldBean = null;
		for(int i=0;linkageDatas!=null && i<linkageDatas.length;i++){
			String[] fieldinfo = Util.splitString(linkageDatas[i], ",");
			hrmFieldBean = new HrmFieldBean();
			hrmFieldBean.setFieldname(fieldinfo[1]);
			hrmFieldBean.setFieldlabel(fieldinfo[2]);
			hrmFieldBean.setFieldhtmltype(fieldinfo[3]);
			hrmFieldBean.setType(fieldinfo[4]);
			hrmFieldBean.setIsFormField(true);
			if(fieldinfo.length>5){
				hrmFieldBean.setFieldvalue(fieldinfo[5]);
			}
			searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
			searchConditionItem.setLabelcol(5);
			searchConditionItem.setFieldcol(10);
			searchConditionItem.setViewAttr(3);
			if(hrmFieldBean.getFieldhtmltype().equals("3")){
				searchConditionItem.getBrowserConditionParam().setViewAttr(3);
				new BaseBean().writeLog("==zj==(fieldinfo--length)" + fieldinfo.length);
				//==zj 考勤报表默认显示此账号数据
				if (fieldinfo.length > 6){
					KqUtil kqUtil = new KqUtil();
					BrowserBean browserConditionParam = searchConditionItem.getBrowserConditionParam();
					List<Map<String, Object>> replaceDatas = browserConditionParam.getReplaceDatas();
					browserConditionParam.setReplaceDatas( kqUtil.getChildKq(user.getUID(),replaceDatas));
				}
			}
			selectLinkageDatas.put(fieldinfo[0],searchConditionItem);
		}
		wrg.setSelectLinkageDatas(selectLinkageDatas);
		
		for(int i=0;i<options.length;i++){
			String[] fieldinfo = Util.splitString(options[i], ",");
			option.add(new SelectOption(fieldinfo[0],SystemEnv.getHtmlLabelNames(fieldinfo[1], user.getLanguage()),fieldinfo[2].equals("true")));
			wrg.setOptions(option);
		}
		
		return wrg;
	}
  
}
