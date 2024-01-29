package com.engine.encrypt.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.SearchConditionOption;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.EncryptSecondAuthEntity;
import com.engine.common.entity.EncryptSpecialAuthEntity;
import com.engine.common.enums.EncryptMould;
import com.engine.common.enums.SecondAuthType;
import com.engine.doc.util.DocEncryptUtil;
import com.engine.encrypt.biz.EncryptFieldConfigComInfo;
import com.engine.encrypt.biz.WfEncryptBiz;
import com.engine.integration.util.DataShowUtil;
import com.engine.meeting.util.MeetingEncryptUtil;
import com.engine.mobilemode.biz.json.MJSONObject;
import com.engine.workflow.biz.SecondAuthBiz;
import com.weaver.formmodel.mobile.ui.manager.MobileAppHomepageManager;
import com.weaver.formmodel.mobile.ui.model.AppHomepage;
import weaver.conn.RecordSet;
import weaver.filter.XssUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.definedfield.HrmFieldComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.HrmSettingsComInfo;
import weaver.hrm.settings.RemindSettings;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class EncryptUtil extends BaseBean {

  public static SecondAuthType getDefaultSecondAuthType() {
    HrmSettingsComInfo hrmSettingsComInfo = new HrmSettingsComInfo();
    String secondDynapassDefault = Util.null2String(hrmSettingsComInfo.getSecondDynapassDefault());
    String secondUsbDtDefault = Util.null2String(hrmSettingsComInfo.getSecondUsbDtDefault());
    String secondPasswordDefault = Util.null2String(hrmSettingsComInfo.getSecondPasswordDefault());
    String secondCADefault = Util.null2String(hrmSettingsComInfo.getSecondCADefault());
    String secondCLDefault = Util.null2String(hrmSettingsComInfo.getSecondCLDefault());
    String secondFaceDefault = Util.null2String(hrmSettingsComInfo.getSecondFaceDefault());

    if (secondDynapassDefault.equals("1")) {
      return SecondAuthType.DynamicPassword;
    } else if (secondUsbDtDefault.equals("1")) {
      return SecondAuthType.DynamicToken;
    } else if (secondPasswordDefault.equals("1")) {
      return SecondAuthType.SecondAuthPassword;
    } else if (secondCADefault.equals("1")) {
      return SecondAuthType.CAAuth;
    } else if (secondCLDefault.equals("1")) {
      return SecondAuthType.QYS;
    } else if (secondFaceDefault.equals("1")) {
      return SecondAuthType.FaceAuth;
    }
    return SecondAuthType.SecondAuthPassword;
  }

  public static long getFreeseCretTime(int authType) {
    long freeseCretTime = 0;
    HrmSettingsComInfo hrmSettingsComInfo = new HrmSettingsComInfo();
    if (authType == SecondAuthType.DynamicPassword.getId()) {
      freeseCretTime = Util.getIntValue(hrmSettingsComInfo.getSecondDynapassValidityMin()) * 60 * 1000;
    } else if (authType == SecondAuthType.DynamicToken.getId()) {
      freeseCretTime = Util.getIntValue(hrmSettingsComInfo.getSecondValidityDtMin()) * 60 * 1000;
    } else if (authType == SecondAuthType.SecondAuthPassword.getId()) {
      freeseCretTime = Util.getIntValue(hrmSettingsComInfo.getSecondPasswordMin()) * 60 * 1000;
    } else if (authType == SecondAuthType.CAAuth.getId()) {
      freeseCretTime = Util.getIntValue(hrmSettingsComInfo.getSecondCAValidityMin()) * 60 * 1000;
    } else if (authType == SecondAuthType.QYS.getId()) {
      freeseCretTime = Util.getIntValue(hrmSettingsComInfo.getSecondCLValidityMin()) * 60 * 1000;
    } else if (authType == SecondAuthType.FaceAuth.getId()) {
      freeseCretTime = Util.getIntValue(hrmSettingsComInfo.getSecondFaceValidityMin()) * 60 * 1000;
    }
    if (freeseCretTime < 60 * 5 * 1000) {
      freeseCretTime = 60 * 5 * 1000;
    }
    return freeseCretTime;
  }

  public Object doInvoke(String arg0, String arg1, List<Object> arg2) {
    arg0 = Util.null2String(arg0);
    arg1 = Util.null2String(arg1);
    int size = arg2 == null ? 0 : arg2.size();
    if (arg0.length() == 0 || arg1.length() == 0) {
      return "";
    }
    Object result = null;
    Class<?> procClass = null;
    Method procMethod = null;
    Constructor ct = null;
    try {
      procClass = Class.forName(arg0);
      ct = procClass.getConstructor(new Class[0]);
      Class[] paramClass = new Class[size];
      Object[] paramValue = new Object[size];
      Object obj = null;
      for (int i = 0; i < size; i++) {
        obj = arg2.get(i);
        if (obj instanceof String) {
          paramClass[i] = String.class;
          paramValue[i] = (String) obj;
        } else if (obj instanceof Boolean) {
          paramClass[i] = Boolean.class;
          paramValue[i] = (Boolean) obj;
        } else if (obj instanceof Double) {
          paramClass[i] = Double.class;
          paramValue[i] = (Double) obj;
        } else if (obj instanceof Integer) {
          paramClass[i] = Integer.class;
          paramValue[i] = (Integer) obj;
        } else if (obj instanceof HttpServletRequest) {
          paramClass[i] = HttpServletRequest.class;
          paramValue[i] = (HttpServletRequest) obj;
        } else if (obj instanceof HttpServletResponse) {
          paramClass[i] = HttpServletResponse.class;
          paramValue[i] = (HttpServletResponse) obj;
        } else if (obj instanceof User) {
          paramClass[i] = User.class;
          paramValue[i] = (User) obj;
        } else if (obj instanceof Map) {
          paramClass[i] = Map.class;
          paramValue[i] = (Map) obj;
        } else if (obj == null) {
          paramClass[i] = HttpServletRequest.class;
          paramValue[i] = (HttpServletRequest) obj;
        }
      }
      procMethod = procClass.getMethod(arg1, paramClass);
      result = procMethod.invoke(ct.newInstance(new Object[0]), paramValue);
    } catch (Exception e) {
      writeLog(e);
    }
    return result;
  }

  public static List<String> getExcludeField() {
    List<String> lsExcludeField = new ArrayList<>();
    lsExcludeField.add("lastname");
    lsExcludeField.add("loginid");
    lsExcludeField.add("workcode");
    return lsExcludeField;
  }

  public static List<String> getIsEncryptHiddenField() {
    List<String> lsIsEncryptDisableField = new ArrayList<>();
    lsIsEncryptDisableField.add("workyear");
    lsIsEncryptDisableField.add("companyworkyear");
    return lsIsEncryptDisableField;
  }

  public static boolean isSecondAuthExclude(String mouldCode, String itemCode) {
    boolean isSecondAuthExclude = false;
    if (mouldCode.equals("WORKFLOW") && itemCode.equals("NODE")) {
      //isSecondAuthExclude = true;
    }
    return isSecondAuthExclude;
  }

  public String getLabelName(String labelId, String LanId) {
    return SystemEnv.getHtmlLabelNames(labelId, LanId);
  }

  public Map<String, Object> getSecondAuthConfig(Map<String, Object> params, HttpServletRequest request, User user) {
    new BaseBean().writeLog("=====zj====2023-09-21-params更新:" + JSON.toJSONString(params));
    Map<String, Object> retmap = new HashMap<String, Object>();
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      String mouldCode = Util.null2String(params.get("mouldCode"));
      String itemCode = Util.null2String(params.get("itemCode"));

      if(mouldCode.equals("CONSTRACT")&&itemCode.equals("ONLINE_EDIT")){
        //合同外发编辑
        EncryptSecondAuthEntity encryptSecondAuthEntity = EncryptConfigBiz.getSecondAuthEncryptConfig(mouldCode, itemCode);
        if(Util.null2String(encryptSecondAuthEntity.getIsEnable()).equals("1")){
          int secondAuthType = Util.getIntValue(encryptSecondAuthEntity.getAuthType());
          //获取相关token
          String secondAuthToken = EncryptConfigBiz.getSecondAuthToken(mouldCode,itemCode,"",secondAuthType,user);
          if(EncryptUtil.getFreeMins(secondAuthType,user)==0){
            secondAuthToken = "";
          }
          //二次认证token
          retmap.put("secondAuthToken", secondAuthToken);
          retmap.put("isNeedSecondAuth", true);
          retmap.put("secondAuthType", secondAuthType);
        }
        retmap.put("mouldCode", mouldCode);
        retmap.put("itemCode", itemCode);
        return retmap;
      }

      String userId = "" + user.getUID();
      String verifier = "";
      boolean isNeedSecondAuth = false;
      boolean isNeedDoubleAuth = false;
      String secondAuthToken = "";
      String doubleAuthToken = "";
      int secondAuthType = EncryptUtil.getDefaultSecondAuthType().getId();
      int doubleAuthType = EncryptUtil.getDefaultSecondAuthType().getId();

      String configId = "";
      String value = Util.null2String(params.get("value"));
      if (value.startsWith("desensitization__")) {
        value = new XssUtil().get(value.replace("desensitization__", ""));
        String[] values = Util.splitString(value, "__");
        configId = values[0];
        if (configId.startsWith(EncryptMould.INTEGRATION.getCode() + "+")) {
          String tmpConfigId = configId.replace(EncryptMould.INTEGRATION.getCode() + "+", "");
          isNeedSecondAuth = Util.null2String(new DataShowUtil().getBrowserFiledInfo(tmpConfigId).getSecondauth()).equals("1");
        } else if (mouldCode.equalsIgnoreCase(EncryptMould.COWORK.getCode())) {//协作二级认证单独控制
          isNeedSecondAuth = Util.null2String(params.get("secondAuth")).equals("1");
        } else if (mouldCode.equalsIgnoreCase(EncryptMould.BLOG.getCode())) {
          isNeedSecondAuth = Util.null2String(params.get("secondAuth")).equals("1");
        } else {
          EncryptFieldConfigComInfo encryptConfigComInfo = new EncryptFieldConfigComInfo();
          isNeedSecondAuth = Util.null2String(encryptConfigComInfo.getSecondauth(configId)).equals("1");
        }
      } else {
        // 获取数据
        EncryptSecondAuthEntity encryptSecondAuthEntity = EncryptConfigBiz.getSecondAuthEncryptConfig(mouldCode, itemCode);
        new BaseBean().writeLog("==zj==(encryptSecondAuthEntity新)" + JSON.toJSONString(encryptSecondAuthEntity));
        /*EncryptSecondAuthEntity encryptSecondAuthEntity = new EncryptSecondAuthEntity();*/
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery("select * from enc_secondauth_config_info where mouldcode = ? and itemcode = ?", mouldCode, itemCode);
        new BaseBean().writeLog("=====zj====更新:" + JSON.toJSONString(mouldCode) + " |  " + itemCode);
        if (recordSet.next()) {
          encryptSecondAuthEntity.setId(recordSet.getString("id"));
          encryptSecondAuthEntity.setMouldCode(recordSet.getString("mouldcode"));
          encryptSecondAuthEntity.setItemCode(recordSet.getString("itemcode"));
          encryptSecondAuthEntity.setIsEnable(recordSet.getString("isenable"));
          encryptSecondAuthEntity.setDoubleAuth(recordSet.getString("doubleauth"));
          encryptSecondAuthEntity.setVerifier(recordSet.getString("verifier"));
          encryptSecondAuthEntity.setAuthType(recordSet.getString("authtype"));
          encryptSecondAuthEntity.setShowOrder(recordSet.getString("showorder"));
          encryptSecondAuthEntity.setFilePath(recordSet.getString("filepath"));
          encryptSecondAuthEntity.setAuthScope(recordSet.getString("authscope"));
        }
        new BaseBean().writeLog("=====zj====encryptSecondAuthEntity更新:" + JSON.toJSONString(encryptSecondAuthEntity));
        EncryptSpecialAuthEntity encryptSpecialAuthEntity = EncryptConfigBiz.getSpecialAuthEncryptConfig(userId, itemCode);
        if(mouldCode.equalsIgnoreCase(EncryptMould.WORKFLOW.getCode()) && "NODE_SUBMIT".equalsIgnoreCase(itemCode)){
          encryptSecondAuthEntity = EncryptConfigBiz.getSecondAuthEncryptConfig(mouldCode, "NODE");
          int wfid = Util.getIntValue(Util.null2String(params.get("workflowid")), 0);
          int nodeid = Util.getIntValue(Util.null2String(params.get("nodeid")), 0);
          Map<String, Object> authConfig = SecondAuthBiz.getSecondAuthConfig(wfid, nodeid, user);
          int secondAuthTypeInt = Util.getIntValue(Util.null2String(authConfig.get("secondAuthType")));
          int isEnableAuth = Util.getIntValue(Util.null2String(authConfig.get("isEnableAuth")));
          if (encryptSecondAuthEntity != null) {
            if (isEnableAuth == 1 && secondAuthTypeInt == 60) {
              encryptSecondAuthEntity.setIsEnable("1");
              encryptSecondAuthEntity.setAuthType(SecondAuthType.QYS.getId() + "");
            } else {
              encryptSecondAuthEntity.setIsEnable("0");
            }
            encryptSecondAuthEntity.setDoubleAuth("0");
            encryptSecondAuthEntity.setVerifier("");
          }
        }else if (encryptSecondAuthEntity != null && mouldCode.equalsIgnoreCase(EncryptMould.WORKFLOW.getCode())) {//流程布局可以单独关闭
          WfEncryptBiz wfEncryptBiz = new WfEncryptBiz();
          Map<String, Object> result = wfEncryptBiz.loadSecondAuthCfg(params);
          encryptSecondAuthEntity.setIsEnable(Util.null2String(result.get("isEnableSecondAuth")));
          encryptSecondAuthEntity.setDoubleAuth(Util.null2String(result.get("isEnableDoubleAuth")));
          encryptSecondAuthEntity.setVerifier(Util.null2String(result.get("authverifier")));
        } else if (encryptSecondAuthEntity != null && encryptSecondAuthEntity.getIsEnable().equals("1") && mouldCode.equalsIgnoreCase(EncryptMould.FORMMODE.getCode())) {//建模布局,查询可以单独关闭
          String layoutid = Util.null2String(params.get("layoutid"));//布局id
          String customid = Util.null2String(params.get("customid"));//查询id
          String id = "0";
          if (!"".equals(layoutid)) {
            id = layoutid;
            sql = "select secondauth,doubleauth,authverifier from modehtmllayout where id = ?";
          } else if (!"".equals(customid)) {
            id = customid;
            sql = "select secondauth,doubleauth,authverifier from mode_customsearch where id = ?";
          }
          rs.executeQuery(sql, id);
          if (rs.next()) {
            encryptSecondAuthEntity.setIsEnable(Util.null2String(rs.getString("secondauth")));
            encryptSecondAuthEntity.setDoubleAuth(Util.null2String(rs.getString("doubleauth")));
            encryptSecondAuthEntity.setVerifier(Util.null2String(rs.getString("authverifier")));//请补充
          } else {
            encryptSecondAuthEntity.setIsEnable("0");
            encryptSecondAuthEntity.setDoubleAuth("0");
            encryptSecondAuthEntity.setVerifier("");
          }
        } else if (encryptSecondAuthEntity != null && "1".equals(encryptSecondAuthEntity.getIsEnable()) && mouldCode.equalsIgnoreCase(EncryptMould.MOBILEMODE.getCode())) {//移动建模页面可以单独关闭
          int pageId = Util.getIntValue(Util.null2String(params.get("pageid")));
          MobileAppHomepageManager mobileAppHomepageManager = MobileAppHomepageManager.getInstance();
          AppHomepage appHomepage = mobileAppHomepageManager.getAppHomepage(pageId);
          String pageAttr = Util.null2String(appHomepage.getPageAttr());
          if (!"".equals(pageAttr)) {
            MJSONObject pageAttrObj = MJSONObject.fromObject(pageAttr);
            encryptSecondAuthEntity.setIsEnable(Util.null2String(pageAttrObj.get("secondaryValidation")));
            encryptSecondAuthEntity.setDoubleAuth(Util.null2String(pageAttrObj.get("doubleAuth")));
            encryptSecondAuthEntity.setVerifier(Util.null2String(pageAttrObj.get("authVerifier")));
          } else {
            encryptSecondAuthEntity.setIsEnable("0");
            encryptSecondAuthEntity.setDoubleAuth("0");
            encryptSecondAuthEntity.setVerifier("");
          }
        }else if(mouldCode.equalsIgnoreCase(EncryptMould.DOCUMENT.getCode())){
          int docid = Util.getIntValue(params.get("docid")+"");//布局id
          if(docid>0){
            Map<String,String> encryptInfo = DocEncryptUtil.EncryptInfo(docid);
            encryptSecondAuthEntity.setIsEnable(Util.null2String(encryptInfo.get(DocEncryptUtil.ISENABLESECONDAUTH)));
            encryptSecondAuthEntity.setDoubleAuth(Util.null2String(encryptInfo.get(DocEncryptUtil.ISENABLEDOUBLEAUTH)));
            encryptSecondAuthEntity.setVerifier(Util.null2String(encryptInfo.get(DocEncryptUtil.AUTHVERIFIER)));
          }
        }else if(mouldCode.equalsIgnoreCase(EncryptMould.MEETING.getCode())){
          String meetingid = Util.null2String(params.get("meetingid"));
          if(meetingid != null && meetingid != ""){
            Map<String,String> encryptInfo = MeetingEncryptUtil.EncryptInfo(meetingid);
            encryptSecondAuthEntity.setIsEnable(Util.null2String(encryptInfo.get("isEnableSecondAuth")));
            encryptSecondAuthEntity.setDoubleAuth(Util.null2String(encryptInfo.get("isEnableDoubleAuth")));
            encryptSecondAuthEntity.setVerifier(Util.null2String(encryptInfo.get("authverifier")));
          }
        }else if(mouldCode.equalsIgnoreCase(EncryptMould.WORKPLAN.getCode())){
          String workplanid = Util.null2String(params.get("workplanid"));
          if(workplanid != null && workplanid != ""){
            Map<String,String> encryptInfo = MeetingEncryptUtil.workplanEncryptInfo(workplanid);
            encryptSecondAuthEntity.setIsEnable(Util.null2String(encryptInfo.get("isEnableSecondAuth")));
            encryptSecondAuthEntity.setDoubleAuth(Util.null2String(encryptInfo.get("isEnableDoubleAuth")));
            encryptSecondAuthEntity.setVerifier(Util.null2String(encryptInfo.get("authverifier")));
          }
        }

        if (encryptSecondAuthEntity != null && encryptSecondAuthEntity.getIsEnable().equals("1")) {
          //二次身份认证
          isNeedSecondAuth = true;
          secondAuthType = Util.getIntValue(encryptSecondAuthEntity.getAuthType());
          doubleAuthType = Util.getIntValue(encryptSecondAuthEntity.getAuthType());
          if (!EncryptUtil.isSecondAuthExclude(mouldCode, itemCode)) {
            //其他无需二次身份认证的情况
            if (Util.null2String(encryptSecondAuthEntity.getDoubleAuth()).equals("1")) {
              isNeedDoubleAuth = true;
              verifier = Util.null2String(encryptSecondAuthEntity.getVerifier());
            }
          }
        }
        if (encryptSpecialAuthEntity != null) {
          //如果开启了特殊身份认证，不需要进行二次身份认证
          isNeedDoubleAuth = true;
          doubleAuthType = Util.getIntValue(encryptSpecialAuthEntity.getAuthType());
          verifier = Util.null2String(encryptSpecialAuthEntity.getVerifier());
        }

        if (isNeedDoubleAuth && ("" + user.getUID()).equals(verifier)){
          //如果双重身份认证是本人，不需要再次认证
          isNeedDoubleAuth = false;
          verifier = "";
        }
      }
      if (secondAuthType < 0) {
        secondAuthType = EncryptUtil.getDefaultSecondAuthType().getId();
      }
      if (doubleAuthType < 0) {
        doubleAuthType = EncryptUtil.getDefaultSecondAuthType().getId();
      }

      //获取相关token
      if(isNeedSecondAuth){
        secondAuthToken = EncryptConfigBiz.getSecondAuthToken(mouldCode,itemCode,configId,secondAuthType,user);
        if(EncryptUtil.getFreeMins(secondAuthType,user)==0){
          secondAuthToken = "";
        }
      }
      if(isNeedDoubleAuth){
        User tmpUser = new User(Util.getIntValue(verifier));
        doubleAuthToken = EncryptConfigBiz.getSecondAuthToken(mouldCode,itemCode,configId,doubleAuthType,tmpUser);
      }
      new BaseBean().writeLog("======zj======before userSecondAuthStatus");
      //检查个人设置
      new BaseBean().writeLog("==zj==(2023-09-21-isNeedSecondAuth)" + JSON.toJSONString(isNeedSecondAuth));
      new BaseBean().writeLog("==zj==(2023-09-21-userSecondAuthStatus)" + !userSecondAuthStatus(user));
      if(!userSecondAuthStatus(user)){
        isNeedSecondAuth = false;
        secondAuthToken = "";
      }
      new BaseBean().writeLog("======zj======after userSecondAuthStatus:" + userSecondAuthStatus(user));

      retmap.put("mouldCode", mouldCode);
      retmap.put("itemCode", itemCode);
      retmap.put("configId", configId);
      //是否需要二次认证（自己）
      retmap.put("isNeedSecondAuth", isNeedSecondAuth);
      //是否需要双重认证（他人），特殊人员人认证开启的情况下，以特殊人员认证优先
      retmap.put("isNeedDoubleAuth", isNeedDoubleAuth);
      //二次认证方式（自己）
      retmap.put("secondAuthType", secondAuthType);
      //双重认证方式（他人）
      retmap.put("doubleAuthType", doubleAuthType);
      //二次认证token
      retmap.put("secondAuthToken", secondAuthToken);
      //双重认证方式token
      retmap.put("doubleAuthToken", doubleAuthToken);
      //认证人
      retmap.put("verifier", verifier);
      retmap.put("status", "1");
      new BaseBean().writeLog("======zj======retmap:" + JSON.toJSONString(retmap));
    } catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
      writeLog(e);
    }
    writeLog("getSecondAuthConfig retmap:"+retmap);
    return retmap;
  }

  public String getMouldName(String id, String params) {
    List parameterList = Util.TokenizerString(params, "+");
    String mouldcode = Util.null2String(parameterList.get(0));
    int languageId = Util.getIntValue(Util.null2String(parameterList.get(1)), 7);
    String mouldName = SystemEnv.getHtmlLabelName(EncryptMould.getLabelIdByMouldCode(mouldcode), languageId);
    return mouldName;
  }

  public String getEncryptBaseSettingName(String id, String params) {
    int languageId = Util.getIntValue(Util.null2String(params), 7);
    String encryptBaseSettingName = SystemEnv.getHtmlLabelName(82751, languageId);
    return encryptBaseSettingName;
  }

  public String getEncryptFieldName(String id, String params) {
    String showName = "";
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      HrmFieldComInfo hrmFieldComInfo = new HrmFieldComInfo();
      List parameterList = Util.TokenizerString(params, "+");
      String scopeId = Util.null2String(parameterList.get(0));
      String fieldname = Util.null2String(parameterList.get(1));
      showName = fieldname;
      int languageId = Util.getIntValue(Util.null2String(parameterList.get(2)), 7);
      int fieldLabel = -1;
      if (scopeId.equals("-1")) {
        if (hrmFieldComInfo.getBaseFieldMap(fieldname) != null) {
          fieldLabel = Util.getIntValue(Util.null2String(hrmFieldComInfo.getBaseFieldMap(fieldname).get("fieldlabel")));
        }
      } else if (scopeId.equals("1")) {
        if (hrmFieldComInfo.getBaseFieldMap(fieldname) != null) {
          fieldLabel = Util.getIntValue(Util.null2String(hrmFieldComInfo.getPersonalFieldMap(fieldname).get("fieldlabel")));
        }
      } else if (scopeId.equals("3")) {
        if (hrmFieldComInfo.getBaseFieldMap(fieldname) != null) {
          fieldLabel = Util.getIntValue(Util.null2String(hrmFieldComInfo.getWorkFieldMap(fieldname).get("fieldlabel")));
        }
      } else if ("SUBCOMPANY".equalsIgnoreCase(scopeId)) {//分部信息
        sql = "select fieldlabel from hrm_formfield a, hrm_fieldgroup b where a.groupid = b.id AND b.grouptype=4 and fieldname=?";
        rs.executeQuery(sql, fieldname);
        if (rs.next()) {
          fieldLabel = rs.getInt("fieldlabel");
        }
      } else if ("DEPARTMENT".equalsIgnoreCase(scopeId)) {//部门信息
        sql = "select fieldlabel from hrm_formfield a, hrm_fieldgroup b where a.groupid = b.id AND b.grouptype=5 and fieldname=?";
        rs.executeQuery(sql, fieldname);
        if (rs.next()) {
          fieldLabel = rs.getInt("fieldlabel");
        }
      } else if ("SALARY".equalsIgnoreCase(scopeId)) {//工资福利
        if (fieldname.equals("accountname")) {
          fieldLabel = 83353;
        } else if (fieldname.equals("accountid1")) {
          fieldLabel = 16016;
        } else if (fieldname.equals("accumfundaccount")) {
          fieldLabel = 16085;
        }
      }
      showName = SystemEnv.getHtmlLabelName(fieldLabel, languageId);
    } catch (Exception e) {
      writeLog(e);
    }
    return showName;
  }

  public String getEncryptItemName(String id, String params) {
    String showName = "";
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      List parameterList = Util.TokenizerString(params, "+");
      String mouldcode = Util.null2String(parameterList.get(0));
      String itemcode = Util.null2String(parameterList.get(1));
      int languageId = Util.getIntValue(Util.null2String(parameterList.get(2)), 7);
      sql = "select * from enc_item_config_info  where mouldcode=? and itemcode=?";
      rs.executeQuery(sql, mouldcode, itemcode);
      if (rs.next()) {
        showName = SystemEnv.getHtmlLabelName(rs.getInt("itemlabel"), languageId);
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return showName;
  }

  public String getSpecialAuthItemName(String id, String params) {
    String showName = "";
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      List parameterList = Util.TokenizerString(params, "+");
      String itemid = Util.null2String(parameterList.get(0));
      int languageId = Util.getIntValue(Util.null2String(parameterList.get(1)), 7);
      sql = "select itemlabel from enc_item_config_info where id=?";
      rs.executeQuery(sql, itemid);
      if (rs.next()) {
        showName = SystemEnv.getHtmlLabelName(rs.getInt("itemlabel"), languageId);
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return showName;
  }

  public String getFieldViewScopeName(String id, String language) {
    String result = "";
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      String resourceid = "";
      String resourcetype = "";
      String alllevel = "";
      int languageid = weaver.general.ThreadVarLanguage.getLang();
      String jobtitlelevel = "";
      String subdepid = "";

      sql = "select typevalue as resourceid,type as resourcetype,alllevel,jobtitlelevel from enc_field_view_scope where id = ? ";
      rs.executeQuery(sql, id);
      if (rs.next()) {
        resourceid = Util.null2String(rs.getString("resourceid"));
        resourcetype = Util.null2String(rs.getString("resourcetype"));
        alllevel = Util.null2String(rs.getString("alllevel"));
        jobtitlelevel = Util.null2String(rs.getString("jobtitlelevel"));
      } else {
        return "";
      }
      SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      if ("1".equals(resourcetype) || "7".equals(resourcetype) || "8".equals(resourcetype)) {
        ResourceComInfo resourceComInfo = new ResourceComInfo();
        result = resourceComInfo.getResourcename(resourceid);
      } else if ("2".equals(resourcetype)) {
        if (alllevel.equals("1")) {
          result = subCompanyComInfo.getSubCompanyname(resourceid) + "(" + SystemEnv.getHtmlLabelName(125963, languageid) + ")";
        } else {
          result = subCompanyComInfo.getSubCompanyname(resourceid);
        }
      } else if ("3".equals(resourcetype)) {
        if (alllevel.equals("1")) {
          result = departmentComInfo.getDepartmentname(resourceid) + "(" + SystemEnv.getHtmlLabelName(125963, languageid) + ")";
        } else {
          result = departmentComInfo.getDepartmentname(resourceid);
        }
      } else if ("5".equals(resourcetype)) {
        JobTitlesComInfo rComInfo = new JobTitlesComInfo();
        String st = "";
        if (jobtitlelevel.equals("1")) st = "/" + SystemEnv.getHtmlLabelName(140, languageid);
        if (jobtitlelevel.equals("2"))
          st = "/" + SystemEnv.getHtmlLabelName(19437, languageid) + "(" + subCompanyComInfo.getSubcompanynames(subdepid) + ")";
        if (jobtitlelevel.equals("3"))
          st = "/" + SystemEnv.getHtmlLabelName(19438, languageid) + "(" + departmentComInfo.getDepartmentNames(subdepid) + ")";
        result = rComInfo.getJobTitlesname(resourceid) + st;
      } else if ("6".equals(resourcetype)) {
        result = SystemEnv.getHtmlLabelName(1340, languageid);
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return result;
  }

  public static int getFreeMins(int secondAuthType, User user){
    String sql = "";
    RecordSet rs = new RecordSet();
    int freeMins = 0;
    try{
      ChgPasswdReminder reminder = new ChgPasswdReminder();
      RemindSettings settings = reminder.getRemindSettings();
      if (secondAuthType == com.engine.workflow.constant.SecondAuthType.DynamicPassword.getId()) {      //动态密码
        freeMins = Util.getIntValue(Util.null2String(settings.getSecondDynapassValidityMin()), 0);      //动态密码的免密时间
      } else if (secondAuthType == com.engine.workflow.constant.SecondAuthType.DynamicToken.getId()) {      //动态令牌
        freeMins = Util.getIntValue(Util.null2String(settings.getSecondValidityDtMin()), 0);      //动态令牌的免密时间
      } else if (secondAuthType == com.engine.workflow.constant.SecondAuthType.SecondAuthPassword.getId()) {    //二次密码
        freeMins = Util.getIntValue(Util.null2String(settings.getSecondPasswordMin()), 0);      //二次密码的免密时间
      } else if (secondAuthType == com.engine.workflow.constant.SecondAuthType.CAAuth.getId()) {    //CA认证
        freeMins = Util.getIntValue(Util.null2String(settings.getSecondCAValidityMin()), 0);      //CA的免密时间
      } else if (secondAuthType == com.engine.workflow.constant.SecondAuthType.QYS.getId()) {    //契约锁
        freeMins = Util.getIntValue(Util.null2String(settings.getSecondCLValidityMin()), 0);      //契约锁的免密时间
      }
      if(Util.null2String(settings.getAllowUserSetting()).equals("1")){
        sql = "select * from user_secondauth_config where userid = "+user.getUID();
        rs.executeQuery(sql);
        if(rs.next()){
          freeMins = rs.getInt("freeMins");
        }
      }
    }catch (Exception e){
      new BaseBean().writeLog(e);
    }

    return freeMins;
  }

  public static boolean userSecondAuthStatus(User user) {
    boolean userSecondAuthStatus = false;
    String sql = "";
    RecordSet rs = new RecordSet();
    //检查个人设置
    ChgPasswdReminder reminder = new ChgPasswdReminder();
    RemindSettings settings = reminder.getRemindSettings();
    if (Util.null2String(settings.getAllowUserSetting()).equals("1")) {
      sql = "select * from user_secondauth_config where userid = " + user.getUID();
      rs.executeQuery(sql);
      if (rs.next()) {
        if (rs.getInt("status") == 1) {
          //设置了，状态为 1 表示启用
          userSecondAuthStatus = true;
        }
      }else{
        //未设置情况，默认为启用
        userSecondAuthStatus = true;
      }
    }else{
      //不允许设置情况，默认为启用
      userSecondAuthStatus = true;
    }
    return userSecondAuthStatus;
  }

  public static List<SearchConditionOption> getEncModeDefine(String mode ,User user){
    List<SearchConditionOption> options = new ArrayList<>();
    SearchConditionOption option = null;
    String sql = "";
    RecordSet rs = new RecordSet();
    sql = "select * from enc_mode_define where status=1 order by id asc";
    rs.executeQuery(sql);
    while (rs.next()) {
      option = new SearchConditionOption();
      option.setKey(rs.getString("definedInterfaceClass"));
      option.setShowname(SystemEnv.getHtmlLabelName(rs.getInt("definedInterfaceLabel"), user.getLanguage()));
      option.setSelected(mode!=null&&mode.equals(rs.getString("definedInterfaceClass")));
      options.add(option);
    }
    return options;
  }
}
