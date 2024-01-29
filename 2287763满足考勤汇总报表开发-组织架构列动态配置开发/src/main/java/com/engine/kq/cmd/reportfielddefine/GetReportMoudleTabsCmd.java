package com.engine.kq.cmd.reportfielddefine;

import com.api.customization.qc2287763.Util.KqReportUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.biz.KQReportFieldComInfo;
import com.engine.kq.biz.KQReportFieldGroupComInfo;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetReportMoudleTabsCmd extends AbstractCommonCommand<Map<String, Object>> {

  public GetReportMoudleTabsCmd(Map<String, Object> params, User user) {
    this.user = user;
    this.params = params;
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    HashMap retmap = new HashMap();
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      List<Map<String, Object>> tabs = new ArrayList();
      Map<String, Object> tab = null;
      //默认条件
      tab = new HashMap();
      tab.put("key", "0");
      tab.put("title", SystemEnv.getHtmlLabelName(149, this.user.getLanguage()));
      tab.put("moudledata", getDisplaySetting());
      tabs.add(tab);

      sql = "select * from kq_report_moudle where resourceid=? order by showorder ";
      rs.executeQuery(sql,user.getUID());
      while (rs.next()) {
        tab = new HashMap();
        tab.put("key", rs.getString("id"));
        tab.put("title", rs.getString("moudlename"));
        tab.put("moudledata", rs.getString("moudledata"));
        tab.put("editable", true);
        tabs.add(tab);
      }

      retmap.put("status", "1");
      retmap.put("tabs", tabs);
    } catch (Exception e) {
      this.writeLog(e);
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(382661, this.user.getLanguage()));
    }
    return retmap;
  }


  public Map<String, Object> getDisplaySetting() {
    Map retmap = new HashMap();
    Map<String,Object> displaySetting = new HashMap<>();
    List<Object> groups = new ArrayList<>();
    Map<String,Object> group = null;
    List<Object> options = null;
    Map<String,Object> option = null;
    String value = "";
    KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
    List<Map<String, Object>> leaveRules = kqLeaveRulesBiz.getAllLeaveRules();
    KQReportFieldGroupComInfo kqReportFieldGroupComInfo = new KQReportFieldGroupComInfo();
    KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
    while(kqReportFieldGroupComInfo.next()){
      if(!kqReportFieldGroupComInfo.getIsshow().equals("1"))continue;
      group = new HashMap<>();
      options = new ArrayList<>();
      value = "";

      kqReportFieldComInfo.setTofirstRow();
      while (kqReportFieldComInfo.next()){
        if(kqReportFieldComInfo.getGroupid().equals(kqReportFieldGroupComInfo.getId())){
          if(!kqReportFieldComInfo.getIsenable().equals("1"))continue;
          if(!kqReportFieldComInfo.getIsDefinedColumn().equals("1"))continue;
          if(!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("month"))continue;
          if("overtime_nonleave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname()) || "overtime_4leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())){
            continue;
          }
          if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0){
            continue;
          }
          option = new HashMap<>();
          option.put("key",kqReportFieldComInfo.getFieldname());
          option.put("label",kqReportFieldComInfo.getFieldlabel());
          option.put("name",kqReportFieldComInfo.getFieldname());
          option.put("showname",SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(),user.getLanguage()));
          options.add(option);
          if(kqReportFieldComInfo.getDefaultShow().equals("1")) {
            if(value.length()>0)value+=",";
            value += kqReportFieldComInfo.getFieldname();
          }
        }
      }
      group.put("groupname", Util.formatMultiLang(kqReportFieldGroupComInfo.getGroupname(),""+user.getLanguage()));
      //==zj 添加建模表自定义字段
      if ("人员信息~".equals(Util.formatMultiLang(kqReportFieldGroupComInfo.getGroupname(),""+user.getLanguage()))){
        KqReportUtil kqReportUtil = new KqReportUtil();
        kqReportUtil.getTabs(options);
      }
      group.put("options",options);
      group.put("value",value);
      if(options.size()==0)continue;
      groups.add(group);
    }

    //加入未分组数据
    group = new HashMap<>();
    options = new ArrayList<>();
    value = "";
    kqReportFieldComInfo.setTofirstRow();
    while (kqReportFieldComInfo.next()){
      if(Util.null2String(kqReportFieldComInfo.getGroupid()).equals("")){
        if(!kqReportFieldComInfo.getIsenable().equals("1"))continue;
        if(!kqReportFieldComInfo.getIsDefinedColumn().equals("1"))continue;
        if(!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("month"))continue;
        if("overtime_nonleave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname()) || "overtime_4leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())){
          continue;
        }
        if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0){
          continue;
        }
        option = new HashMap<>();
        option.put("key",kqReportFieldComInfo.getFieldname());
        option.put("label",kqReportFieldComInfo.getFieldlabel());
        option.put("name",kqReportFieldComInfo.getFieldname());
        option.put("showname",SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(),user.getLanguage()));
        options.add(option);
        if(kqReportFieldComInfo.getDefaultShow().equals("1")) {
          if(value.length()>0)value+=",";
          value += kqReportFieldComInfo.getFieldname();
        }
      }
    }
    group.put("groupname", SystemEnv.getHtmlLabelName(81307,user.getLanguage()));
    group.put("options",options);
    group.put("value",value);
    if(options.size()>0) {
      groups.add(group);
    }

    displaySetting.put("groups",groups);
    retmap.put("displaySetting",displaySetting);
    return retmap;
  }
  @Override
  public BizLogContext getLogContext() {
    return null;
  }

}
