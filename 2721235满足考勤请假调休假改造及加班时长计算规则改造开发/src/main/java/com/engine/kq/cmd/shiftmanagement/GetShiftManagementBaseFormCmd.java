package com.engine.kq.cmd.shiftmanagement;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

/**
 * 获取班次管理基本信息表单
 * @author pzy
 *
 */
public class GetShiftManagementBaseFormCmd extends AbstractCommonCommand<Map<String, Object>>{

  public GetShiftManagementBaseFormCmd(Map<String, Object> params, User user) {
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
    Map<String, Object> retmap = new HashMap<String, Object>();
    List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
    Map<String, Object> groupitem = null;
    List<Object> itemlist = null;
    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    String sql = "";
    try {
      if(!HrmUserVarify.checkUserRight("KQClass:Management",user)) {
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
        return retmap;
      }
      CheckSubCompanyRight newCheck=new CheckSubCompanyRight();
      ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
      boolean hrmdetachable = manageDetachComInfo.isUseHrmManageDetach();//是否开启了人力资源模块的管理分权
      String hrmdftsubcomid = manageDetachComInfo.getHrmdftsubcomid();//分权默认分部

      SubCompanyComInfo sc = new SubCompanyComInfo();
      String _id = Util.null2String(params.get("id"));
      String subcompanyid = Util.null2String(params.get("subcompanyid"));
      boolean isEdit = false;

      List<Object> workSectionList = new ArrayList<>();
      Map<String,Object> groupSectionMaps = new HashMap<>();//分组用的map
      Map<String,Object> sectionMaps = new HashMap<>();
      Map<String,Object> sectionMap = new HashMap<>();

      //现在休息时段还是只能设置一个的，对于以后可能出现的多个休息时段预留这个map
      List<Object> restSectionsList = new ArrayList<>();
      Map<String,Object> restSectionsMap = new HashMap<>();

      String[] fields = new String[]{"serial,125818,1,1","subcompanyid,141,3,169","shiftonoffworkcount,388563,5,3","punchsettings,388564,4,1",
          "isresttimeopen,388565,4,2","restbeigin,388566,3,19","restend,388567,3,19","halfcalrule,513090,5,1","cardRemind,507833,4,2","cardRemOfSignIn,507835,5,1",
          "minsBeforeSignIn,510106,1,2","cardRemOfSignOut,507837,5,1","minsAfterSignOut,510108,1,2","remindMode,501471,5,3","remindOnPC,513233,4,2",
          "isoffdutyfreecheck,388568,4,2","isNight,-1218003,4,2","halfcalpoint,513145,3,19","halfcalpoint2cross,513090,5,1"};

      Map<String,String> shiftValMap = new HashMap<>();
      if(_id.length() > 0){
        String getShiftInfo  = "select * from kq_ShiftManagement where (isdelete is null or  isdelete <> '1') and  id = ?";
        rs.executeQuery(getShiftInfo, _id);
        if(rs.next()){
          for(int i = 0 ; i < fields.length ; i++){
            String[] tmpField = Util.null2String(fields[i]).split(",");
            String fieldname = tmpField[0];
            String fieldvalue = rs.getString(fieldname);
            if("shiftonoffworkcount".equalsIgnoreCase(fieldname) ||
                "punchsettings".equalsIgnoreCase(fieldname) ||
                "cardRemOfSignIn".equalsIgnoreCase(fieldname) ||
                "cardRemOfSignOut".equalsIgnoreCase(fieldname) ||
                "remindMode".equalsIgnoreCase(fieldname)){
              fieldvalue = Util.null2s(fieldvalue, "1");
            }else if("isresttimeopen".equalsIgnoreCase(fieldname) ||
                "halfcalrule".equalsIgnoreCase(fieldname) ||
                "cardRemind".equalsIgnoreCase(fieldname) ||
                "minsAfterSignOut".equalsIgnoreCase(fieldname) ||
                "remindOnPC".equalsIgnoreCase(fieldname) ||
                "isoffdutyfreecheck".equalsIgnoreCase(fieldname) ||
                "halfcalpoint".equalsIgnoreCase(fieldname) ||
                "halfcalpoint2cross".equalsIgnoreCase(fieldname)){
              fieldvalue = Util.null2s(fieldvalue, "0");
            }else if("minsBeforeSignIn".equalsIgnoreCase(fieldname)){
              fieldvalue = Util.null2s(fieldvalue, "10");
            }else if("subcompanyid".equalsIgnoreCase(fieldname)){
              subcompanyid = fieldvalue;
            }
            shiftValMap.put(fieldname, fieldvalue);
          }
        }

        String getWorkSections = "select * from kq_ShiftOnOffWorkSections where (isdelete is null or  isdelete <> '1') and serialid = ?  order by record ";
        rs.executeQuery(getWorkSections, _id);
        while(rs.next()){
          String record = rs.getString("record");
          if(record.length() == 0) {
            continue;
          }

          String onoffworktype = Util.null2String(rs.getString("onoffworktype"));
          String across = Util.null2String(rs.getString("across"));
          String times = Util.null2String(rs.getString("times"));
          String mins = Util.null2String(rs.getString("mins"));
          String mins_next = Util.null2String(rs.getString("mins_next"));
          String clockinnot = Util.null2String(rs.getString("clockinnot"));
          sectionMap = new HashMap<>();
          sectionMaps = new HashMap<>();
          sectionMap.put("across", across);
          sectionMap.put("times", times);
          sectionMap.put("mins", mins);
          sectionMap.put("mins_next", mins_next);
          sectionMap.put("clockinnot", clockinnot);
          sectionMaps.put(onoffworktype, sectionMap);
          if(groupSectionMaps.get(record) != null){
            List<Object> tmpSections = (List<Object>) groupSectionMaps.get(record);
            ((Map<String,Object>)tmpSections.get(tmpSections.size()-1)).putAll(sectionMaps);
          }else{
            sectionMaps.put("record", record);
            workSectionList.add(sectionMaps);
            groupSectionMaps.put(record, workSectionList);
          }
        }

        groupSectionMaps = new HashMap<>();
        sectionMaps = new HashMap<>();
        String getRestSections = "select * from kq_ShiftRestTimeSections where (isdelete is null or  isdelete <> '1') and serialid = ? order by orderId ";
        rs.executeQuery(getRestSections, _id);
        while(rs.next()){
          String resttype = Util.null2String(rs.getString("resttype"));
          String across = Util.null2String(rs.getString("across"));
          String times = Util.null2String(rs.getString("time"));
		  String record = Util.null2String(rs.getString("record1"));
          String orderId = Util.null2String(rs.getString("orderId"));
          restSectionsMap = new HashMap<>();
		  sectionMaps = new HashMap<>();
          restSectionsMap.put("resttype", resttype);
          restSectionsMap.put("time", times);
          restSectionsMap.put("across", across);
          restSectionsMap.put("record", record);
          restSectionsMap.put("orderId", orderId);
          sectionMaps.put(resttype, restSectionsMap);
          if(groupSectionMaps.get(record) != null){
            List<Object> tmpSections = (List<Object>) groupSectionMaps.get(record);
            ((Map<String,Object>)tmpSections.get(tmpSections.size()-1)).putAll(sectionMaps);
          }else{
            sectionMaps.put("record", record);
            restSectionsList.add(sectionMaps);
            groupSectionMaps.put(record, restSectionsList);
          }
        }
        isEdit = true;
      }

      //班次名称 所属机构（开启分权有所属机构） 一天内上下班次数  打卡时段是否开启 排除休息时间是否开启 休息开始时间 休息结束时间 允许下班不打卡
      HrmFieldBean hrmFieldBean = null;

      HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
      SearchConditionItem searchConditionItem = null;
      List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();

      groupitem = new HashMap<String, Object>();

      itemlist = new ArrayList<Object>();
      for (int j = 0; j < fields.length; j++) {
        options = new ArrayList<SearchConditionOption>();
        String[] tmpField = Util.null2String(fields[j]).split(",");
        String fieldname = tmpField[0];
        String beanVal = Util.null2String(shiftValMap.get(fieldname));
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname(fieldname);
        hrmFieldBean.setFieldlabel(tmpField[1]);
        hrmFieldBean.setFieldhtmltype(tmpField[2]);
        hrmFieldBean.setType(tmpField[3]);
        hrmFieldBean.setIsFormField(true);

        if("serial".equalsIgnoreCase(tmpField[0]) || "restbeigin".equalsIgnoreCase(tmpField[0]) || "restend".equalsIgnoreCase(tmpField[0])){
          hrmFieldBean.setViewAttr(3);
          hrmFieldBean.setRules("required|string");
        }
        if("shiftonoffworkcount".equalsIgnoreCase(tmpField[0]) || "restbeigin".equalsIgnoreCase(tmpField[0]) || "restend".equalsIgnoreCase(tmpField[0])){
          hrmFieldBean.setRules("required|string");
          if(!isEdit){
            if("restbeigin".equalsIgnoreCase(tmpField[0])){
              hrmFieldBean.setFieldvalue("12:00");
            }
            if("restend".equalsIgnoreCase(tmpField[0])){
              hrmFieldBean.setFieldvalue("13:00");
            }
          }
        }
//        if("color".equalsIgnoreCase(tmpField[0])){
//          hrmFieldBean.setTip(SystemEnv.getHtmlLabelName(389509, user.getLanguage()));
//        }
        if(!isEdit){
          if("isresttimeopen".equalsIgnoreCase(tmpField[0])){
            hrmFieldBean.setFieldvalue("0");
          }
          if("isoffdutyfreecheck".equalsIgnoreCase(tmpField[0])){
            hrmFieldBean.setFieldvalue("0");
          }
          if("punchsettings".equalsIgnoreCase(tmpField[0])){
            hrmFieldBean.setFieldvalue("1");
          }
//          if("color".equalsIgnoreCase(tmpField[0])){
//            hrmFieldBean.setFieldvalue("#000");
//          }
          if("cardRemOfSignIn".equals(hrmFieldBean.getFieldname())){
            beanVal = "1";
          }
          if("minsBeforeSignIn".equals(hrmFieldBean.getFieldname())){
            beanVal = "10";
          }
          if("cardRemOfSignOut".equals(hrmFieldBean.getFieldname())){
            beanVal = "1";
          }
          if("minsAfterSignOut".equals(hrmFieldBean.getFieldname())){
            beanVal = "0";
          }
          if("remindMode".equals(hrmFieldBean.getFieldname())){
            beanVal = "1";
          }
          if("halfcalrule".equals(hrmFieldBean.getFieldname())){
            hrmFieldBean.setFieldvalue("0");
          }
          if("halfcalpoint2cross".equals(hrmFieldBean.getFieldname())){
            hrmFieldBean.setFieldvalue("0");
          }
        }

        if("shiftonoffworkcount".equals(tmpField[0])){
          SearchConditionOption SearchConditionOption_1 = new SearchConditionOption("1",SystemEnv.getHtmlLabelName(388569, user.getLanguage()));
          SearchConditionOption SearchConditionOption_2 =	new SearchConditionOption("2",SystemEnv.getHtmlLabelName(388570, user.getLanguage()));
          SearchConditionOption SearchConditionOption_3 =	new SearchConditionOption("3",SystemEnv.getHtmlLabelName(388571, user.getLanguage()));
          if(isEdit){
            if("1".equalsIgnoreCase(beanVal)){
              SearchConditionOption_1.setSelected(true);
            }else if("2".equalsIgnoreCase(beanVal)){
              SearchConditionOption_2.setSelected(true);
            }else if("3".equalsIgnoreCase(beanVal)){
              SearchConditionOption_3.setSelected(true);
            }
          }else{
            SearchConditionOption_1.setSelected(true);
          }
          options.add(SearchConditionOption_1);
          options.add(SearchConditionOption_2);
          options.add(SearchConditionOption_3);
          hrmFieldBean.setSelectOption(options);
        }
        if(isEdit){
          hrmFieldBean.setFieldvalue(beanVal);
        }
        if("subcompanyid".equals(tmpField[0])){
          if(hrmdetachable){
            hrmFieldBean.setViewAttr(3);
            hrmFieldBean.setRules("required|string");
            String defaultSubcompanyid = "";
            int[] subcomids = newCheck.getSubComByUserRightId(user.getUID(),"KQClass:Management",0);
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
            //表示左侧分部树选择了
            if(Util.getIntValue(Util.null2String(subcompanyid)) > 0){
              hrmFieldBean.setFieldvalue(subcompanyid);
            }else{
              hrmFieldBean.setFieldvalue(defaultSubcompanyid);
            }

          }else{
            //不开启分权的话，不显示分部
            continue;
          }
        }
        if("punchsettings".equalsIgnoreCase(tmpField[0])){
          hrmFieldBean.setFieldvalue("1");
        }
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        if("shiftonoffworkcount".equals(tmpField[0])){
//          searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(388574, user.getLanguage()));
        }
        if("isoffdutyfreecheck".equals(tmpField[0])){
          searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(388573, user.getLanguage()));
        }
        if(hrmdetachable && "subcompanyid".equals(tmpField[0])){
          searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "KQClass:Management");
          searchConditionItem.getBrowserConditionParam().getCompleteParams().put("rightStr", "KQClass:Management");
        }
        if("cardRemOfSignIn".equals(hrmFieldBean.getFieldname())){
          List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
          optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(19782, user.getLanguage()), "0".equals(beanVal)));
          optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(510106, user.getLanguage()), "1".equals(beanVal)));
          searchConditionItem.setOptions(optionsList);
          searchConditionItem.setValue(beanVal);
        }
        if("minsBeforeSignIn".equals(hrmFieldBean.getFieldname())||"minsAfterSignOut".equals(hrmFieldBean.getFieldname())){
          searchConditionItem.setValue(beanVal);
          searchConditionItem.setMin("0");
          searchConditionItem.setViewAttr(3);
          searchConditionItem.setRules("required|integer");
        }
        if("cardRemOfSignOut".equals(hrmFieldBean.getFieldname())){
          List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
          optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(19782, user.getLanguage()), "0".equals(beanVal)));
          optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(510108, user.getLanguage()), "1".equals(beanVal)));
          searchConditionItem.setOptions(optionsList);
          searchConditionItem.setValue(beanVal);
        }
        if("remindMode".equals(hrmFieldBean.getFieldname())){
          List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
          optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(383607, user.getLanguage()), "1".equals(beanVal)));
          optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(18845, user.getLanguage()), "2".equals(beanVal)));
          optionsList.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(17586, user.getLanguage()), "3".equals(beanVal)));
          searchConditionItem.setOptions(optionsList);
          searchConditionItem.setValue(beanVal);
        }
        if("halfcalrule".equals(hrmFieldBean.getFieldname())){
          List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
          optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(513091, user.getLanguage()), "0".equals(beanVal)));
          optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(513092, user.getLanguage()), "1".equals(beanVal)));
          searchConditionItem.setOptions(optionsList);
        }
        if("halfcalpoint2cross".equals(hrmFieldBean.getFieldname())){
          List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
          optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(509159, user.getLanguage()), "0".equals(beanVal)));
          optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(388785, user.getLanguage()), "1".equals(beanVal)));
          searchConditionItem.setOptions(optionsList);
        }
        searchConditionItem.setColSpan(1);

        itemlist.add(searchConditionItem);
      }
      groupitem.put("items", itemlist);
      grouplist.add(groupitem);
      int operatelevel = -1;
      if(hrmdetachable){
        if(subcompanyid.length()>0 && !subcompanyid.equalsIgnoreCase("0")){
          CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
          operatelevel=checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"KQClass:Management",Util.getIntValue(subcompanyid,-1));
        }
      }else{
        operatelevel = 2;
      }
      if(!isEdit){
        operatelevel = 2;
      }

      if(user.getUID() == 1){
        operatelevel = 2;
      }
      if(operatelevel > 0){
        retmap.put("canAdd", true);
      }else{
        retmap.put("canAdd", false);
      }
      retmap.put("status", "1");
      retmap.put("condition", grouplist);
      retmap.put("workSections", workSectionList);
      retmap.put("restTimeSections", restSectionsList);
      if(shift_24()){
        retmap.put("shift_24", "1");
      }else{
        retmap.put("shift_24", "0");
      }
      retmap.put("id", _id);
    } catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      writeLog(e);
    }
    return retmap;
  }

  /**
   * 班次是否放开24小时制
   * @return
   */
  public boolean shift_24() {
    boolean shift_24 = false;
    RecordSet rs = new RecordSet();
    String settingSql = "select * from KQ_SETTINGS where main_key='shift_24'";
    rs.executeQuery(settingSql);
    if(rs.next()){
      String main_val = rs.getString("main_val");
      if("1".equalsIgnoreCase(main_val)){
        shift_24 = true;
      }
    }
    return shift_24;
  }
}
