package com.engine.kq.cmd.hrmAttProcSet;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.wfset.attendance.domain.HrmAttProcFields;
import com.engine.kq.wfset.attendance.domain.HrmAttProcRelation;
import com.engine.kq.wfset.attendance.domain.HrmAttProcSet;
import com.engine.kq.wfset.attendance.domain.WorkflowBillfield;
import com.engine.kq.wfset.attendance.manager.HrmAttProcFieldsManager;
import com.engine.kq.wfset.attendance.manager.HrmAttProcRelationManager;
import com.engine.kq.wfset.attendance.manager.HrmAttProcSetManager;
import com.engine.kq.wfset.attendance.manager.WorkflowBillfieldManager;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

/**
 * 获取考勤流程设置 字段对应信息
 * @author pzy
 *
 */
public class GetStateProcSetFlowWfFieldsCmd extends AbstractCommonCommand<Map<String, Object>>{

	public GetStateProcSetFlowWfFieldsCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}


	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		 
		List<Object> lsGroup = new ArrayList<Object>();
		Map<String,Object> groupitem = null;
		List<Object> itemlist = null;
		try {
			
			RecordSet rs = new RecordSet();
			HrmAttProcSetManager attProcSetManager = new HrmAttProcSetManager();
			HrmAttProcFieldsManager attProcFieldsManager = new HrmAttProcFieldsManager();
			HrmAttProcRelationManager attProcRelationManager = new HrmAttProcRelationManager();
			WorkflowBillfieldManager workflowBillfieldManager = new WorkflowBillfieldManager();
			
			String id = StringUtil.vString(params.get("id"));
			int subcompanyid = StringUtil.parseToInt(StringUtil.vString(params.get("subcompanyid")), 0);
			subcompanyid = subcompanyid < 0 ? 0 : subcompanyid;

			HrmAttProcSet bean = attProcSetManager.get(id);
			int billId = bean.getField002();
			//考勤流程类型
      int field006 = bean.getField006();
			List list = attProcFieldsManager.find(attProcSetManager.getMapParam("field001:"+bean.getBillId()+";languageid:"+user.getLanguage()));
			List fieldList = workflowBillfieldManager.find(attProcSetManager.getMapParam("billid:"+billId+";languageid:"+user.getLanguage()+";viewtype:0"));
      List detailList = workflowBillfieldManager.find(attProcSetManager.getMapParam("billid:"+billId+";languageid:"+user.getLanguage()+";viewtype:1"));

      List relationList = attProcRelationManager.find(attProcSetManager.getMapParam("field001:"+id));

			HrmAttProcFields fBean = null;
			StringBuffer checkFields = new StringBuffer();
			for(int i=0; i<(list == null ? 0 : list.size()); i++){
				fBean = (HrmAttProcFields)list.get(i);
				if(fBean.getField010() == 1){
					checkFields.append(checkFields.length() == 0 ? "" : ",")
					.append("select"+fBean.getId());
				}
			}
			if(bean.isHAF() && relationList.size() == 0) {
				attProcRelationManager.initRelation(bean.getId(), list, fieldList);
				relationList = attProcRelationManager.find(attProcSetManager.getMapParam("field001:"+id));
			}

      int usedetail = bean.getUsedetail();
      //排班的现在只有明细
      if(field006 == 5){
        usedetail = 1;
      }
      Map<String,Object> mapFields = new HashMap<String, Object>();

      //补卡和销假流程是主表和明细表都需要的
      if(field006 == 7 || field006 == 6 || field006 == 8){
        //生成主表字段的集合
        getFieldsInfo(mapFields,list, fieldList, relationList, bean,"0");
        if(mapFields != null && !mapFields.isEmpty()){
          getGroupitems(lsGroup,groupitem,itemlist,mapFields, bean.getTablename());
        }
        mapFields = new HashMap<String, Object>();
        //生成明细字段的集合
        getFieldsInfo(mapFields,list, detailList, relationList, bean,"1");
        if(mapFields != null && !mapFields.isEmpty()){
          getGroupitems(lsGroup,groupitem,itemlist,mapFields, bean.getDetailtablename());
        }

      }else{
        String labelFormName = "";
        if(usedetail == 1){
          labelFormName = bean.getDetailtablename();
          //生成明细字段的集合
          getFieldsInfo(mapFields,list, detailList, relationList, bean,"1");
        }else{
          labelFormName = bean.getTablename();
          //生成主表字段的集合
          getFieldsInfo(mapFields,list, fieldList, relationList, bean,"0");
        }
        if(mapFields != null && !mapFields.isEmpty()){
            //qc2574368
            new BaseBean().writeLog("==zj==(考勤类型)" + JSON.toJSONString(field006));
          getGroupitems(lsGroup,groupitem,itemlist,mapFields, labelFormName);
        }
      }

      HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
      HrmFieldBean hrmFieldBean = null;
      String[] msg = new String[]{
          "1,10005333,8,1"};
      itemlist = new ArrayList<Object>();
      for(int i= 0 ; i < msg.length ; i++){
        String msg_str = msg[i];
        String [] fieldConfig = msg_str.split(",");
        String fieldname = fieldConfig[0];
        String fieldlabel = "";
        String fieldhtmltype = fieldConfig[2];
        String type = fieldConfig[3];
        String fieldvalue = Util.toScreen(SystemEnv.getHtmlLabelName(Util.getIntValue(fieldConfig[1]), user.getLanguage()), user.getLanguage());
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname(fieldname);
        hrmFieldBean.setFieldlabel(fieldlabel);
        hrmFieldBean.setFieldhtmltype(fieldhtmltype);
        hrmFieldBean.setType(type);
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(1);
        hrmFieldBean.setFieldvalue(fieldvalue);
        itemlist.add(hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user));
      }
      groupitem = new HashMap<String, Object>();
      groupitem.put("title", SystemEnv.getHtmlLabelName(85,user.getLanguage()));
      groupitem.put("defaultshow", true);
      groupitem.put("items", itemlist);
      if(field006 == KqSplitFlowTypeEnum.CARD.getFlowtype() || field006 == KqSplitFlowTypeEnum.SHIFT.getFlowtype()){
      }else{
        lsGroup.add(groupitem);
      }

      retmap.put("condition", lsGroup);
			retmap.put("status", "1");
		} catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
			
		return retmap;
	}

  /**
   * 根据主表、明细表生成相应的返回字段数据
   * @param lsGroup
   * @param groupitem
   * @param itemlist
   * @param mapFields
   * @param labelFormName
   */
	private void getGroupitems(List<Object> lsGroup,Map<String,Object> groupitem,List<Object> itemlist,Map<String,Object> mapFields,String labelFormName){

    for(Map.Entry<String, Object> me : mapFields.entrySet()){
      String key = me.getKey();//得到group的标签
      List<List<Object>> listFields = (List<List<Object>>) me.getValue();
      HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
      SearchConditionItem searchConditionItem = null;
      HrmFieldBean hrmFieldBean = null;
      itemlist = new ArrayList<Object>();
      groupitem = new HashMap<String,Object>();
      groupitem.put("title", SystemEnv.getHtmlLabelNames(key,user.getLanguage())+labelFormName);
      groupitem.put("defaultshow", true);
      for(int i=0;i<listFields.size();i++){
        List<Object> fieldinfo = listFields.get(i);
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname(Util.null2String(fieldinfo.get(0)));
        hrmFieldBean.setFieldlabel(Util.null2String(fieldinfo.get(1)));
        hrmFieldBean.setFieldhtmltype(Util.null2String(fieldinfo.get(2)));
        hrmFieldBean.setType(Util.null2String(fieldinfo.get(3)));
        hrmFieldBean.setIsFormField(true);
        List<SearchConditionOption> options = (List<SearchConditionOption>) fieldinfo.get(4);
        if(options != null && options.size() > 0){
          hrmFieldBean.setSelectOption(options);
        }
        boolean isMust = (Boolean) fieldinfo.get(5);
        if(isMust){
          hrmFieldBean.setViewAttr(3);
        }
        hrmFieldBean.setTip(Util.null2String(fieldinfo.get(6)));

        boolean isDisabled = (Boolean) fieldinfo.get(7);
        if(isDisabled){
          hrmFieldBean.setViewAttr(1);
        }

        //qc2574368
          new BaseBean().writeLog("==zj==(hrmFieldBean)" + JSON.toJSONString(hrmFieldBean));

        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        if(isMust){
          searchConditionItem.setRules("required|string");
        }
        searchConditionItem.setHelpfulTip(Util.null2String(fieldinfo.get(6)));
        searchConditionItem.setLabelcol(6);
        searchConditionItem.setFieldcol(18);
        itemlist.add(searchConditionItem);
      }
      groupitem.put("items", itemlist);
      lsGroup.add(groupitem);
    }
  }


	/**
	 * 生成字段对应的集合数据
	 * @param mapFields
	 * @param list
	 * @param fieldList
	 * @param relationList
	 * @param bean
   * @param usedetail 1表示明细表
	 */
	private void getFieldsInfo(Map<String,Object> mapFields,List list,List fieldList,List relationList,HrmAttProcSet bean,String usedetail){
		List<List<Object>> listFields = new ArrayList<List<Object>>();
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
		List<Object> lFields = new ArrayList<Object>();
		HrmAttProcFields fBean = null;
		HrmAttProcRelation rBean = null;
		WorkflowBillfield wfBean = null;

		String groupLable = "18020";
		if("1".equalsIgnoreCase(usedetail)){
		  groupLable = "18550";
    }

    String bean_detailname = bean.getDetailtablename();
		int billId = bean.getField002();
		boolean isDisabled = false;
		for(int i=0; i<(list == null ? 0 : list.size()); i++){
			fBean = (HrmAttProcFields)list.get(i);
			String selectFieldName = "";
      if("1".equalsIgnoreCase(usedetail)){
        //如果是明细表，不以detail_开头的都要去除
        if(!fBean.getField002().startsWith("detail_")) {
          continue;
        }
      }else {
        //如果是主表，以detail_开头的都要去除
        if(fBean.getField002().startsWith("detail_")) {
          continue;
        }
      }

			for(int x=0; x<(relationList==null?0:relationList.size()); x++){
				rBean = (HrmAttProcRelation)relationList.get(x);
				if(String.valueOf(rBean.getField002()).equals(String.valueOf(fBean.getId()))){
					selectFieldName = rBean.getField004();
					break;
				}
			}

      if(mapFields.get(groupLable) != null){
        listFields = (List<List<Object>>) mapFields.get(groupLable);
      }else {
        listFields = new ArrayList<List<Object>>();
        mapFields.put(groupLable,listFields);
      }

			boolean isSameType = false, isSameHtmltype = false, isSelectField = false;
			SearchConditionOption emptySearch = new SearchConditionOption("","");
			options.add(emptySearch);
			for(int j=0; j<(fieldList == null ? 0 : fieldList.size()); j++){
				wfBean = (WorkflowBillfield)fieldList.get(j);
        String detailtable = wfBean.getDetailtable();
        if("1".equalsIgnoreCase(usedetail)){
          if(bean_detailname.length() > 0 && detailtable.length() > 0 && !detailtable.equalsIgnoreCase(bean_detailname)) {
            continue;
          }
        }
				isSameType = String.valueOf(wfBean.getType()).equals(String.valueOf(fBean.getField006()));
				isSameHtmltype = wfBean.getFieldhtmltype().equals(String.valueOf(fBean.getField005()));
				isSelectField = selectFieldName.length() > 0 && selectFieldName.equals(wfBean.getFieldname());
				SearchConditionOption SearchConditionOption1 = new SearchConditionOption(wfBean.getFieldname()+"___"+wfBean.getId(),wfBean.getLabelName(),(selectFieldName.length() > 0 && selectFieldName.equals(wfBean.getFieldname())?true:false));
				if(!isSelectField && ((fBean.getField005() == 5 && !isSameHtmltype)|| (fBean.getField005() != 5 && (!isSameHtmltype || !isSameType)))) {
//					continue;
					SearchConditionOption1.setVisible(false);
				}
				options.add(SearchConditionOption1);

			}

			lFields = new ArrayList<Object>();
			lFields.add("select"+fBean.getId());
			lFields.add(""+fBean.getField003());
			lFields.add("5");
			lFields.add("1");
			lFields.add(options);
			lFields.add(fBean.getField010() == 1);
			lFields.add(SystemEnv.getHtmlLabelNames(fBean.getField005Title(),user.getLanguage()));
			lFields.add(isDisabled);
			listFields.add(lFields);
			options = new ArrayList<SearchConditionOption>();
		}
	}
}
