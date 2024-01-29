package com.engine.hrm.cmd.securitysetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;

import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import weaver.hrm.loginstrategy.LoginStrategyComInfo;
import weaver.hrm.loginstrategy.style.LoginStrategy;

/**
 * 安全设置高级设置表单
 *
 * @author lvyi
 */

public class GetSecuritySettingAdvancedForm extends AbstractCommonCommand<Map<String, Object>> {

  public GetSecuritySettingAdvancedForm(Map<String, Object> params, User user) {
    this.user = user;
    this.params = params;
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String, Object> retmap = new HashMap<String, Object>();
    List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
    Map<String, Object> groupitem = null;
    List<Object> itemlist = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      if (!HrmUserVarify.checkUserRight("OtherSettings:Edit", user)&&!EncryptConfigBiz.hasRight(user)) {
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
        return retmap;
      }

      //动态密码保护：默认启用方式、动态密码长度、动态密码内容、有效期（秒）
      //海泰Key：启用、默认启用方式
      //动态令牌：启用、默认启用方式
      sql = "select * from HrmSettings ";
      rs.execute(sql);
      rs.next();

      LoginStrategyComInfo loginStrategyComInfo = new LoginStrategyComInfo();

      int viewattr = 2;
      Map<String, String[]> fieldGroups = new LinkedHashMap<String, String[]>();
      String fromEncrypt = Util.null2String(params.get("fromEncrypt"));
      LoginStrategy segmentStrategy = LoginStrategy.SEGMENT_STRATEGY;
      if (fromEncrypt.equals("1")) {
        fieldGroups.put("32491", new String[]{"secondNeedDynapass,388489,4,2", "dynapasslen,20280,1,2,integer", "dypadcon,21993,5,1",
          "validitySec,382773,1,2,integer", "secondDynapassValidityMin,388490,1,2,integer", "secondDynapassDefault,525435,4,1"});
        fieldGroups.put("32896", new String[]{"secondNeedusbDt,388489,4,2", "secondValidityDtMin,388490,1,2,integer", "secondUsbDtDefault,525435,4,1"});
        fieldGroups.put("388412", new String[]{"secondPasswordMin,388490,1,2,integer", "secondPasswordDefault,525435,4,1"});
        //契约锁 人脸识别暂不支持 qc2473809
        /*fieldGroups.put("381991", new String[]{"secondCA,388489,4,2", "secondCAValidityMin,388490,1,2,integer", "secondCADefault,525435,4,1"});*/
        fieldGroups.put("-2023817010", new String[]{"needCL,-2023817021,4,2","clAuthtype,-2023817008,5,2","clAuthTypeDefault,-2023817007,5,1","needCLdefault,-2023817022,5,1","secondCL,-2023817009,4,2","secondCLValidityMin,-2023817006,1,2,integer", "secondCLDefault,-2023817005,4,1","addressCL,-2023817004,1,1","appKey,-2023817003,1,1", "appSecret,-2023817002,1,1","hideSuccessStatusPage,-2023817001,4,2"});
        //fieldGroups.put("507979", new String[]{"secondFace,388489,4,2", "secondFaceValidityMin,388490,1,2,integer", "secondFaceDefault,525435,4,1"});
      } else {
        fieldGroups.put("32491", new String[]{"needdynapass,388492,4,2", "needdynapassdefault,32507,5,1", "dynapasslen,20280,1,2,integer", "dypadcon,21993,5,1",
          "validitySec,382773,1,2,integer", "needpassword,507832,4,2", "secondNeedDynapass,388489,4,2", "secondDynapassValidityMin,388490,1,2,integer", "secondDynapassDefault,525435,4,1"});
        fieldGroups.put("32896", new String[]{"needusbDt,388492,4,2", "needusbdefaultDt,32507,5,1", "secondNeedusbDt,388489,4,2", "secondValidityDtMin,388490,1,2,integer", "secondUsbDtDefault,525435,4,1"});
        //fieldGroups.put("388412", new String[]{ "secondPassword,388489,4,2","secondPasswordMin,388490,1,2,integer"});
        fieldGroups.put("388412", new String[]{"secondPasswordMin,388490,1,2,integer", "secondPasswordDefault,525435,4,1"});
        //qc2473809
        fieldGroups.put("385000", new String[]{"mobileScanCA,388492,4,2", "CADefault,32507,5,1"});//"secondCA,388489,4,2", "secondCAValidityMin,388490,1,2,integer", "secondCADefault,525435,4,1"});//"addressCA,388491,1,1", 先隐藏
        //qc2473809
        fieldGroups.put("-2023817010", new String[]{"needCL,-2023817021,4,2","clAuthtype,-2023817008,5,2","clAuthTypeDefault,-2023817007,5,1","needCLdefault,-2023817022,5,1","secondCL,-2023817009,4,2","secondCLValidityMin,-2023817006,1,2,integer", "secondCLDefault,-2023817005,4,1","addressCL,-2023817004,1,1","appKey,-2023817003,1,1", "appSecret,-2023817002,1,1","hideSuccessStatusPage,-2023817001,4,2"});
        //契约锁 人脸识别暂不支持
        //fieldGroups.put("388414", new String[]{ "secondCL,388489,4,2","addressCL,388491,1,1"});//,"secondCLValidityMin,388490,1,2,integer"
        //fieldGroups.put("507979", new String[]{ "secondFace,388489,4,2","secondFaceValidityMin,388490,1,2,integer","secondFaceDefault,525435,4,1"});

        // 网段策略
        fieldGroups.put(Integer.toString(segmentStrategy.getLabel()),
          new String[]{segmentStrategy.toString() + ",388492,4,2", segmentStrategy.getDefaultFlagName() + ",32507,5,1"});

				fieldGroups.put("526336", new String[]{ "qrCode,388492,4,2","qrCodeDefault,32507,5,1"});
      }

      Map<String, List<SearchConditionOption>> htOption = new HashMap<String, List<SearchConditionOption>>();
      List<SearchConditionOption> options = null;
      String needdynapassdefault = Util.getIntValue(rs.getString("needdynapassdefault")) < 0 ? "0" : rs.getString("needdynapassdefault");
      String validitySec = Util.getIntValue(rs.getString("validitySec")) <= 0 ? "120" : rs.getString("validitySec");
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), needdynapassdefault.equals("0")));
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18096, user.getLanguage()), needdynapassdefault.equals("1")));
      options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21384, user.getLanguage()), needdynapassdefault.equals("2")));
      htOption.put("needdynapassdefault", options);

      String dypadcon = Util.getIntValue(rs.getString("dypadcon")) < 0 ? "0" : rs.getString("dypadcon");
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(607, user.getLanguage()), dypadcon.equals("0")));
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18729, user.getLanguage()), dypadcon.equals("1")));
      options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21994, user.getLanguage()), dypadcon.equals("2")));
      htOption.put("dypadcon", options);

      String needusbdefaultHt = Util.getIntValue(rs.getString("needusbdefaultHt")) < 0 ? "0" : rs.getString("needusbdefaultHt");
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), needusbdefaultHt.equals("0")));
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18096, user.getLanguage()), needusbdefaultHt.equals("1")));
      options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21384, user.getLanguage()), needusbdefaultHt.equals("2")));
      htOption.put("needusbdefaultHt", options);

      String needusbdefaultDt = Util.getIntValue(rs.getString("needusbdefaultDt")) < 0 ? "0" : rs.getString("needusbdefaultDt");
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), needusbdefaultDt.equals("0")));
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18096, user.getLanguage()), needusbdefaultDt.equals("1")));
      options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21384, user.getLanguage()), needusbdefaultDt.equals("2")));
      htOption.put("needusbdefaultDt", options);

      String CADefault = Util.getIntValue(rs.getString("CADefault")) < 0 ? "0" : rs.getString("CADefault");
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), CADefault.equals("0")));
      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18096, user.getLanguage()), CADefault.equals("1")));
      htOption.put("CADefault", options);

      //qc2473809
      String needCLdefault = Util.getIntValue(rs.getString("needCLdefault")) < 0 ? "0" : rs.getString("needCLdefault");
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(-2023817016, user.getLanguage()), needCLdefault.equals("0")));
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(-2023817015, user.getLanguage()), needCLdefault.equals("1")));
      options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(-2023817014, user.getLanguage()), needusbdefaultDt.equals("2")));
      htOption.put("needCLdefault", options);

      options = new ArrayList<SearchConditionOption>();

      //
      String segmentStrategyDefaultValue = Util.null2String(loginStrategyComInfo.getDefaultflag(segmentStrategy.toString()));
      if (StringUtils.isBlank(segmentStrategyDefaultValue)) segmentStrategyDefaultValue = "0";

      options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), segmentStrategyDefaultValue.equals("0")));
      options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18096, user.getLanguage()), segmentStrategyDefaultValue.equals("1")));
      htOption.put(segmentStrategy.getDefaultFlagName(), options);


      //qc2473809
			String qrCodeDefault = Util.getIntValue(rs.getString("qrCodeDefault"))<0?"0":rs.getString("qrCodeDefault");
			options = new ArrayList<SearchConditionOption>();
			options.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(-2023817016, user.getLanguage()),qrCodeDefault.equals("0")));
			options.add(new SearchConditionOption("0",SystemEnv.getHtmlLabelName(-2023817015, user.getLanguage()),qrCodeDefault.equals("1")));
			htOption.put("qrCodeDefault", options);


      String clAuthtype = Util.null2String(rs.getString("clAuthtype"));
      if (clAuthtype.length() == 0) clAuthtype = "FACE";
      options = new ArrayList<SearchConditionOption>();
      SearchConditionOption option = new SearchConditionOption("FACE", SystemEnv.getHtmlLabelName(-2023817013, user.getLanguage()), clAuthtype.indexOf("FACE")>-1);
      option.setDisabled(true);
      options.add(option);
      options.add(new SearchConditionOption("IVS", SystemEnv.getHtmlLabelName(-2023817012, user.getLanguage()), clAuthtype.indexOf("IVS")>-1));
      options.add(new SearchConditionOption("BANK", SystemEnv.getHtmlLabelName(-2023817011, user.getLanguage()), clAuthtype.indexOf("BANK")>-1));
      htOption.put("clAuthtype", options);

      String clAuthTypeDefault = Util.null2String(rs.getString("clAuthTypeDefault"));
      if (clAuthTypeDefault.length() == 0) clAuthTypeDefault = "FACE";
      options = new ArrayList<SearchConditionOption>();
      options.add(new SearchConditionOption("FACE", SystemEnv.getHtmlLabelName(-2023817013, user.getLanguage()), clAuthTypeDefault.indexOf("FACE")>-1));
      options.add(new SearchConditionOption("IVS", SystemEnv.getHtmlLabelName(-2023817012, user.getLanguage()), clAuthTypeDefault.indexOf("IVS") > -1));
      options.add(new SearchConditionOption("BANK", SystemEnv.getHtmlLabelName(-2023817011, user.getLanguage()), clAuthTypeDefault.indexOf("BANK") > -1));
      htOption.put("clAuthTypeDefault", options);
      //

      HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
      SearchConditionItem searchConditionItem = null;
      HrmFieldBean hrmFieldBean = null;
      Iterator<Entry<String, String[]>> iter = fieldGroups.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, String[]> entry = iter.next();
        String grouplabel = entry.getKey();
        String[] fields = entry.getValue();
        groupitem = new HashMap<String, Object>();
        groupitem.put("title", SystemEnv.getHtmlLabelNames(grouplabel, user.getLanguage()));
        groupitem.put("defaultshow", true);

        itemlist = new ArrayList<Object>();
        for (int j = 0; j < fields.length; j++) {
          int tmpViewattr = viewattr;
          String[] fieldinfo = fields[j].split(",");
          hrmFieldBean = new HrmFieldBean();
          hrmFieldBean.setFieldname(fieldinfo[0]);
          hrmFieldBean.setFieldlabel(fieldinfo[1]);
          hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
          hrmFieldBean.setType(fieldinfo[3]);
          hrmFieldBean.setIsFormField(true);
          if (hrmFieldBean.getFieldname().equals("needdynapassdefault")) {
            hrmFieldBean.setFieldvalue(needdynapassdefault);
          } else if (hrmFieldBean.getFieldname().equals("dypadcon")) {
            hrmFieldBean.setFieldvalue(dypadcon);
          } else if (hrmFieldBean.getFieldname().equals("needusbdefaultHt")) {
            hrmFieldBean.setFieldvalue(needusbdefaultHt);
          } else if (hrmFieldBean.getFieldname().equals("needusbdefaultDt")) {
            hrmFieldBean.setFieldvalue(needusbdefaultDt);
          } else if (hrmFieldBean.getFieldname().equals("validitySec")) {
            hrmFieldBean.setFieldvalue(validitySec);
          } else if (segmentStrategy.toString().equals(hrmFieldBean.getFieldname())) {
            hrmFieldBean.setFieldvalue(loginStrategyComInfo.getFlag(segmentStrategy.toString()));
          } else if (segmentStrategy.getDefaultFlagName().equals(hrmFieldBean.getFieldname())) {
            hrmFieldBean.setFieldvalue(segmentStrategyDefaultValue);
          } else {
            String fieldvalue = Util.null2String(rs.getString(fieldinfo[0]));
            if (Util.getIntValue(fieldvalue) <= 0) {
              if (hrmFieldBean.getFieldname().equals("dynapasslen")) {
                fieldvalue = "6";
              }
            }
            hrmFieldBean.setFieldvalue(fieldvalue);
          }

          if (fieldinfo.length == 5) {
            hrmFieldBean.setRules("required|" + fieldinfo[4]);
          }
          //qc2473809
          if (hrmFieldBean.getFieldname().equals("addressCA") || hrmFieldBean.getFieldname().equals("addressCL")
                  || hrmFieldBean.getFieldname().equals("appKey")|| hrmFieldBean.getFieldname().equals("appSecret")) {
            hrmFieldBean.setMultilang(false);
          }
          searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
          if (searchConditionItem != null) {
            searchConditionItem.setLabelcol(8);
            searchConditionItem.setFieldcol(16);
          }

          if (hrmFieldBean.getFieldlabel().equals("388490")) {
            searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(388510, user.getLanguage()));
          }

          if (hrmFieldBean.getFieldhtmltype().equals("5")) {
            searchConditionItem.setOptions(htOption.get(hrmFieldBean.getFieldname()));
          } else if (hrmFieldBean.getFieldhtmltype().equals("1") && hrmFieldBean.getType().equals("2")) {
            Map<String, Object> otherParams = new HashMap<String, Object>();
            otherParams.put("min", 0);
            searchConditionItem.setOtherParams(otherParams);
          }
          //qc2473809
          if(!this.canDisable(hrmFieldBean.getFieldname()) && Util.null2String(hrmFieldBean.getFieldvalue()).equals("1")){
            tmpViewattr = 1;
          }
          searchConditionItem.setViewAttr(tmpViewattr);
          itemlist.add(searchConditionItem);
        }
        groupitem.put("items", itemlist);
        grouplist.add(groupitem);
      }
      retmap.put("status", "1");
      retmap.put("formField", grouplist);
    } catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
      writeLog(e);
    }
    return retmap;
  }
//qc2473809
  private boolean canDisable(String fieldName){
    boolean canDisable = true;
//    String sql = "";
//    RecordSet rs = new RecordSet();
//    try{
//      int authType = 0;
//      if(fieldName.equals("secondNeedDynapass")){
//        authType = SecondAuthType.DynamicPassword.getId();
//      }else if(fieldName.equals("secondNeedusbDt")){
//        authType = SecondAuthType.DynamicToken.getId();
//      }else if(fieldName.equals("secondCA")){
//        authType = SecondAuthType.CAAuth.getId();
//      }else if(fieldName.equals("secondCL")){
//        authType = SecondAuthType.QYS.getId();
//      }else{
//        return canDisable;
//      }
//      sql = "select count(1) as num from enc_secondauth_config_info where isenable=1 and authtype="+authType;
//      rs.executeQuery(sql);
//      if(rs.next()){
//        if(rs.getInt("num")>0){
//          canDisable = false;
//        }
//      }
//    }catch (Exception e){
//      writeLog(e);
//    }
    return canDisable;
  }

  @Override
  public BizLogContext getLogContext() {
    // TODO Auto-generated method stub
    return null;
  }


}
