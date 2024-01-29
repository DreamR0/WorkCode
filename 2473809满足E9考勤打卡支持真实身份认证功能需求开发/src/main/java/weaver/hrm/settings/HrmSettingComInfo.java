package weaver.hrm.settings;

import weaver.cache.*;
import weaver.conn.RecordSet;
import weaver.general.Util;

public class HrmSettingComInfo extends CacheBase {
  protected static String TABLE_NAME = "hrmsettings";
  /**
   * sql中的where信息，不要以where开始
   */
  protected static String TABLE_WHERE = null;
  /**
   * sql中的order by信息，不要以order by开始
   */
  protected static String TABLE_ORDER = "uuid";

  @PKColumn(type = CacheColumnType.NUMBER)
  protected static String PK_NAME = "uuid";

  /*******************密码策略*******************/
  @CacheColumn(name = "minPasslen")
  private static int minPasslen;//密码最小长度

  @CacheColumn(name = "passwordReuse")
  private static int passwordReuse;//是否开启历史密码校检

  @CacheColumn(name = "passwordReuseNum")
  private static int passwordReuseNum;//历史密码校检次数

  @CacheColumn(name = "passwordComplexity")
  private static int passwordComplexity;// 密码复杂度

  @CacheColumn(name = "valid")
  private static int valid;//密码变更提醒

  @CacheColumn(name = "remindperiod")
  private static int remindperiod;//提醒周期

  @CacheColumn(name = "remindperTime")
  private static int remindperTime; // 提醒时间

  @CacheColumn(name = "passwordChangeReminder")
  private static int passwordChangeReminder;//是否启用密码强制修改

  @CacheColumn(name = "changePasswordDays")
  private static int changePasswordDays;//同一密码使用多少天后强制修改

  @CacheColumn(name = "daysToRemind")
  private static int daysToRemind; //强制修改前多少天提醒用户

  @CacheColumn(name = "loginMustUpPswd")
  private static int loginMustUpPswd;//首次登陆必须修改密码

  @CacheColumn(name = "initialPwdValidity")
  private static int initialPwdValidity;//初始密码有效期

  @CacheColumn(name = "weakPasswordDisable")
  private static int weakPasswordDisable;//禁止保存弱密码

  @CacheColumn(name = "defaultPasswordEnable")
  private static int defaultPasswordEnable;//启用初始密码

  @CacheColumn(name = "defaultPassword")
  private static int defaultPassword;//初始密码
  /*******************密码策略*******************/

  /*******************账号锁定*******************/
  @CacheColumn(name = "openPasswordLock")
  private static int openPasswordLock;//是否开启密码输入错误自动锁定

  @CacheColumn(name = "sumPasswordLock")
  private static int sumPasswordLock;//输入密码错误累计多少次锁定账号

  @CacheColumn(name = "needPasswordLockMin")
  private static int needPasswordLockMin;//是否需要自动解锁

  @CacheColumn(name = "passwordLockMin")
  private static int passwordLockMin;//多少分钟后自动解锁

  @CacheColumn(name = "unRegPwdLock")
  private static int unRegPwdLock;//长时间未登录自动锁定开关

  @CacheColumn(name = "unRegPwdLockDays")
  private static int unRegPwdLockDays;//多少天未登录自动锁定
  /*******************账号锁定*******************/

  /*******************登录设置*******************/
  @CacheColumn(name = "logintype")
  private static int logintype;//登录方式

  @CacheColumn(name = "relogin")
  private static int relogin;//允许同一用户重复登录

  @CacheColumn(name = "needforgotpassword")
  private static int needforgotpassword;//启用忘记密码功能

  @CacheColumn(name = "forgotpasswordmode")
  private static int forgotpasswordmode;//密码找回方式

  @CacheColumn(name = "forgotpasswordExpiration")
  private static int forgotpasswordExpiration;//忘记密码短信和邮件验证码失效时间

  @CacheColumn(name = "forgotpasswordrelieve")
  private static int forgotpasswordrelieve;//找回密码时同步解除账号锁定

  @CacheColumn(name = "forbidLogin")
  private static int forbidLogin;//禁止在网段外登录

  @CacheColumn(name = "needvalidate")
  private static int needvalidate;//是否启用验证码

  @CacheColumn(name = "validatetype")
  private static int validatetype;//验证码类型

  @CacheColumn(name = "validatenum")
  private static int validatenum;//验证码位数

  @CacheColumn(name = "numvalidatewrong")
  private static int numvalidatewrong;//--已废弃

  @CacheColumn(name = "timeOutSwitch")
  private static int timeOutSwitch;//启用超时登出弹窗功能
  /*******************登录设置*******************/

  /*******************其他设置*******************/
  @CacheColumn(name = "checkkey")
  private static int checkkey;//外部用户接口检验码

  @CacheColumn(name = "checkUnJob")
  private static int checkUnJob;//非在职人员信息查看控制，启用后，只有有“离职人员查看”权限的用户才能检索非在职人员

  @CacheColumn(name = "checkIsEdit")
  private static int checkIsEdit;//个人信息编辑权限是否哦开放到个人

  @CacheColumn(name = "checkSysValidate")
  private static int checkSysValidate;//系统信息批量设置验证码控制 启用后，系统信息批量设置保存的时候需要输入验证码--已废弃

  @CacheColumn(name = "canEditPhoto")
  private static int canEditPhoto;//是否允许个人编辑照片

  @CacheColumn(name = "canEditUserIcon")
  private static int canEditUserIcon;//是否允许个人头像
  /*******************其他设置*******************/

  /*******************动态密码保护*******************/
  @CacheColumn(name = "needdynapass")
  private static int needdynapass;//动态密码是否允许作为登录辅助校验

  @CacheColumn(name = "needdynapassdefault")
  private static int needdynapassdefault;//默认启用方式

  @CacheColumn(name = "dynapasslen")
  private static int dynapasslen;//动态密码长度

  @CacheColumn(name = "dypadcon")
  private static int dypadcon;//动态密码内容

  @CacheColumn(name = "validitySec")
  private static int validitySec;//动态密码有效期（单位：秒）

  @CacheColumn(name = "needpassword")
  private static int needpassword;//动态密码是否需要密码

  @CacheColumn(name = "secondNeedDynapass")
  private static int secondNeedDynapass;//动态密码允许作为二次身份校验

  @CacheColumn(name = "secondDynapassValidityMin")
  private static int secondDynapassValidityMin;//动态密码免密时间(分钟)
  /*******************动态密码保护*******************/

  /*******************动态令牌*******************/
  @CacheColumn(name = "needusbDt")
  private static int needusbDt;//动态令牌是否允许作为登录辅助校验

  @CacheColumn(name = "needusbdefaultDt")
  private static int needusbdefaultDt;//默认启用方式

  @CacheColumn(name = "secondNeedusbDt")
  private static int secondNeedusbDt;//动态令牌允许作为二次身份校验

  @CacheColumn(name = "secondValidityDtMin")
  private static int secondValidityDtMin;//动态令牌免密时间(分钟)
  /*******************动态令牌*******************/

  /*海泰key--已废弃*/
  @CacheColumn(name = "needusbHt")
  private static int needusbHt;//海泰key是否允许作为登录辅助校验

  @CacheColumn(name = "needusbdefaultHt")
  private static int needusbdefaultHt;//默认启用方式
  /*海泰key--已废弃*/

  /*******************二次验证密码*******************/
  @CacheColumn(name = "secondPassword")
  private static int secondPassword;//密码允许作为二次身份校验

  @CacheColumn(name = "secondPasswordMin")
  private static int secondPasswordMin;//密码免密时间(分钟)
  /*******************二次验证密码*******************/

  /*******************CA扫码登录*******************/
  @CacheColumn(name = "mobileScanCA")
  private static int mobileScanCA;//CA扫码登录是否允许作为登录辅助校验

  @CacheColumn(name = "CADefault")
  private static int CADefault;//CA默认启用方式

  @CacheColumn(name = "secondCA")
  private static int secondCA;//CA允许作为二次身份校验

  @CacheColumn(name = "secondCAValidityMin")
  private static int secondCAValidityMin;//CA免密时间(分钟)

  @CacheColumn(name = "needCAdefault")
  private static int needCAdefault;

  @CacheColumn(name = "addressCA")
  private static int addressCA;//CA云服务地址

  @CacheColumn(name = "needCA")
  private static int needCA;

  @CacheColumn(name = "cetificatePath")
  private static int cetificatePath;
  /*******************CA扫码登录*******************/

  /*******************契约锁*******************/
  @CacheColumn(name = "needCL")
  private static int needCL;//契约锁是否允许作为登录辅助校验
  
  @CacheColumn(name = "needCLdefault")
  private static int needCLdefault;//契约锁默认启用方式

  @CacheColumn(name = "secondCL")
  private static int secondCL;//契约锁允许作为二次身份校验

  @CacheColumn(name = "secondCLValidityMin")
  private static int secondCLValidityMin;//契约锁免密时间(分钟)

  @CacheColumn(name = "addressCL")
  private static int addressCL;//契约锁云服务地址
  @CacheColumn(name = "appKey")
  private static int appKey; //契约锁企业AppKey
  @CacheColumn(name = "appSecret")
  private static int appSecret;//契约锁AppKey秘钥
  @CacheColumn(name = "clAuthtype")
  private static int clAuthtype;//契约锁认证方式
  @CacheColumn(name = "clAuthTypeDefault")
  private static int clAuthTypeDefault;//契约锁认证方式
  @CacheColumn(name = "hideSuccessStatusPage")
  private static int hideSuccessStatusPage;//契约锁认证通过后隐藏成功页
  /*******************契约锁*******************/

  /*******************人脸识别*******************/
  @CacheColumn(name = "secondFace")
  private static int secondFace;//人脸识别允许作为二次身份校验

  @CacheColumn(name = "secondFaceValidityMin")
  private static int secondFaceValidityMin;//人脸识别免密时间(分钟)
  /*******************人脸识别*******************/

  /*******************PC端强制扫码登录*******************/
  @CacheColumn(name = "qrCode")
  private static int  qrCode;//允许作为登录辅助校验
  @CacheColumn(name = "qrCodeDefault")
  private static int  qrCodeDefault;//默认启用方式
  /*******************PC端强制扫码登录*******************/

  /*******************二次认证允许个人设置*******************/
  @CacheColumn(name = "allowUserSetting")
  private static int allowUserSetting;//二次认证允许个人设置
  /*******************二次认证允许个人设置*******************/

  /*******************员工关怀提醒--生日提醒-管理员提醒*******************/
  @CacheColumn(name = "birthvalidadmin")
  private static int birthvalidadmin;//是否开启提醒

  @CacheColumn(name = "birthremindperiod")
  private static int birthremindperiod;//提前提醒天数

  @CacheColumn(name = "birthremindperTime")
  private static int birthremindperTime;

  @CacheColumn(name = "epremindperTime")
  private static int epremindperTime;

  @CacheColumn(name = "brithalarmadminscope")
  private static int brithalarmadminscope;//--已废弃
  /*******************员工关怀提醒--生日提醒-管理员提醒*******************/

  /*******************员工关怀提醒--生日提醒-员工提醒*******************/
  @CacheColumn(name = "birthvalid")
  private static int birthvalid;//是否开启提醒

  @CacheColumn(name = "birthremindmode")
  private static int birthremindmode;//提醒方式：1-消息中心、2-弹出窗口

  @CacheColumn(name = "birthDispatchImg")
  private static int birthDispatchImg;//推送图片

  @CacheColumn(name = "birthDispatchImgId")
  private static int birthDispatchImgId;//推送图片

  @CacheColumn(name = "birthDispatchShowField")
  private static int birthDispatchShowField;//消息中心显示字段

  @CacheColumn(name = "birthshowfield")
  private static int birthshowfield;//弹出窗口方式的显示字段

  @CacheColumn(name = "birthshowfieldWF")
  private static int birthshowfieldWF;//消息中心方式的显示字段

  @CacheColumn(name = "congratulation")
  private static int congratulation;//弹出窗口方式的生日祝词

  @CacheColumn(name = "congratulation1")
  private static int congratulation1;//消息中心方式的生日祝词

  @CacheColumn(name = "brithalarmscope")
  private static int brithalarmscope;//提醒范围

  @CacheColumn(name = "birthshowfieldcolor")
  private static int birthshowfieldcolor;//弹出窗口方式的显示字段颜色

  @CacheColumn(name = "birthshowcontentcolor")
  private static int birthshowcontentcolor;//弹出窗口方式的祝词字体颜色

  @CacheColumn(name = "birthdialogstyle")
  private static int birthdialogstyle;//弹出窗口方式的弹窗样式(值为图片存储在系统内的ID)
  /*******************员工关怀提醒--生日提醒-员工提醒*******************/

  /*******************入职一周年提醒设置*******************/
  @CacheColumn(name = "entryValid")
  private static int entryValid;//是否开启提醒

  @CacheColumn(name = "entryCongrats")
  private static int entryCongrats;//周年寄语

  @CacheColumn(name = "entryCongratsColor")
  private static int entryCongratsColor;//周年寄语字体颜色

  @CacheColumn(name = "entryFont")
  private static int entryFont;//周年寄语字体

  @CacheColumn(name = "entryFontSize")
  private static int entryFontSize;//周年寄语字体大小

  @CacheColumn(name = "entryDialogStyle")
  private static int entryDialogStyle;//弹窗提醒图片
  /*******************入职一周年提醒设置*******************/

  /*******************合同到期提醒*******************/
  @CacheColumn(name = "contractvalid")
  private static int contractvalid;//是否启用合同到期提醒

  @CacheColumn(name = "contractremindperiod")
  private static int contractremindperiod;//提前几天提醒

  @CacheColumn(name = "contractremindperTime")
  private static int contractremindperTime; // 提醒时间

  @CacheColumn(name = "statusWithContract")
  private static int statusWithContract;//是否开启合同到期后自动将人员置为“无效”状态
  /*******************合同到期提醒*******************/

  /*******************入职提醒*******************/
  @CacheColumn(name = "entervalid")
  private static int entervalid;//是否开启入职提醒
  /*******************入职提醒*******************/

  /*移动电话隐私设置--已废弃*/
  @CacheColumn(name = "mobileShowSet")
  private static int mobileShowSet;//是否允许个人设置

  @CacheColumn(name = "mobileShowType")
  private static int mobileShowType;//可设置项

  @CacheColumn(name = "mobileShowTypeDefault")
  private static int mobileShowTypeDefault;//默认启用方式
  /*移动电话隐私设置--已废弃*/

  /*usb--已废弃*/
  @CacheColumn(name = "needusb")
  private static int needusb;//是否启用辅助校验

  @CacheColumn(name = "usbType")
  private static int usbType;//辅助校验方式：2-海泰key、3-动态令牌、4-动态密码

  @CacheColumn(name = "needusbdefault")
  private static int needusbdefault;//默认启用方式

  @CacheColumn(name = "needusbnetwork")
  private static int needusbnetwork;//辅助校验是否需要网段策略
  /*usb--已废弃*/

  /*其他*/
  @CacheColumn(name = "firmcode")
  private static int firmcode;//验证码

  @CacheColumn(name = "usercode")
  private static int usercode;//用户代码

  @CacheColumn(name = "needDactylogram")
  private static int needDactylogram;//是否需要使用维尔指纹验证设备登录认证功能

  @CacheColumn(name = "canModifyDactylogram")
  private static int canModifyDactylogram;//是否允许客户端修改指纹

  @CacheColumn(name = "defaultResult")
  private static int defaultResult;//默认结果

  @CacheColumn(name = "defaultTree")
  private static int defaultTree;//默认树结构

  //是否开启分级保护
  @CacheColumn(name = "isOpenClassification")
  private static int isOpenClassification;

  //是否开启分级保护
  @CacheColumn(name = "isCrc")
  private static int isCrc;

  // 新建人员默认模式
  @CacheColumn(name = "newResourceMode")
  private static int newResourceMode;

  @CacheColumn(name = "secondDynapassDefault")
  private static int secondDynapassDefault;
  @CacheColumn(name = "secondUsbDtDefault")
  private static int secondUsbDtDefault;
  @CacheColumn(name = "secondPasswordDefault")
  private static int secondPasswordDefault;
  @CacheColumn(name = "secondCADefault")
  private static int secondCADefault;
  @CacheColumn(name = "secondCLDefault")
  private static int secondCLDefault;
  @CacheColumn(name = "secondFaceDefault")
  private static int secondFaceDefault;

  public CacheItem initCache(String key){

    RecordSet rs = new RecordSet() ;
    rs.executeQuery("select * from hrmsettings") ;
    CacheItem cacheItem = null;
    if(rs.next()){
      cacheItem = createCacheItem() ;
      parseResultSetToCacheItem(rs,cacheItem) ;
    }
    return cacheItem ;
  }

  public CacheMap initCache() throws Exception {
    CacheMap localData = super.createCacheMap();
    RecordSet rs = new RecordSet() ;
    rs.executeQuery("select * from hrmsettings") ;
    if(rs.next()){
      String id = Util.null2String(rs.getString(PK_NAME));
      CacheItem row = createCacheItem();
      parseResultSetToCacheItem(rs, row);
      modifyCacheItem(id, row);
      localData.put(id, row);
    }

    return localData;
  }

  public String getNewResourceMode() {
    return (String) getRowValue(newResourceMode);
  }

  public String getId() {
    return (String) getRowValue(PK_INDEX);
  }

  public String getBirthremindmode() {
    return (String) getRowValue(birthremindmode);
  }

  public String getBirthremindperiod() {
    return (String) getRowValue(birthremindperiod);
  }

  public String getBirthvalid() {
    return (String) getRowValue(birthvalid);
  }

  public String getCongratulation() {
    return (String) getRowValue(congratulation);
  }

  public String getCongratulation1() {
    return (String) getRowValue(congratulation1);
  }

  public String getBirthdialogstyle() {
    return (String) getRowValue(birthdialogstyle);
  }

  public String getBirthshowfield() {
    return (String) getRowValue(birthshowfield);
  }

  public String getBrithalarmscope() {
    return (String) getRowValue(brithalarmscope);
  }

  public String getBirthvalidadmin() {
    return (String) getRowValue(birthvalidadmin);
  }

  public String getBrithalarmadminscope() {
    return (String) getRowValue(brithalarmadminscope);
  }

  public String getContractvalid() {
    return (String) getRowValue(contractvalid);
  }

  public String getContractremindperiod() {
    return (String) getRowValue(contractremindperiod);
  }

  public String getEntervalid() {
    return (String) getRowValue(entervalid);
  }

  public String getDynapasslen() {
    return (String) getRowValue(dynapasslen);
  }

  public String getDypadcon() {
    return (String) getRowValue(dypadcon);
  }

  public String getFirmcode() {
    return (String) getRowValue(firmcode);
  }

  public String getMinPasslen() {
    return (String) getRowValue(minPasslen);
  }

  public String getNeeddynapass() {
    return (String) getRowValue(needdynapass);
  }

  public String getNeedusb() {
    return (String) getRowValue(needusb);
  }

  public String getUsbType() {
    return (String) getRowValue(usbType);
  }

  public String getNeedvalidate() {
    return (String) getRowValue(needvalidate);
  }

  public String getLogintype() {
    return (String) getRowValue(logintype);
  }

  public String getRelogin() {
    return (String) getRowValue(relogin);
  }

  public String getRemindperiod() {
    return (String) getRowValue(remindperiod);
  }

  public String getUsercode() {
    return (String) getRowValue(usercode);
  }

  public String getValid() {
    return (String) getRowValue(valid);
  }

  public String getValidatenum() {
    return (String) getRowValue(validatenum);
  }

  public String getTimeOutSwitch() {
    return (String) getRowValue(timeOutSwitch);
  }

  public String getValidatetype() {
    return (String) getRowValue(validatetype);
  }

  public String getNumvalidatewrong() {
    return (String) getRowValue(numvalidatewrong);
  }

  public String getNeedDactylogram() {
    return (String) getRowValue(needDactylogram);
  }

  public String getCanModifyDactylogram() {
    return (String) getRowValue(canModifyDactylogram);
  }

  public String getDaysToRemind() {
    return (String) getRowValue(daysToRemind);
  }

  public String getPasswordChangeReminder() {
    return (String) getRowValue(passwordChangeReminder);
  }

  public String getChangePasswordDays() {
    return (String) getRowValue(changePasswordDays);
  }

  public String getOpenPasswordLock() {
    return (String) getRowValue(openPasswordLock);
  }

  public String getSumPasswordLock() {
    return (String) getRowValue(sumPasswordLock);
  }

  public String getPasswordComplexity() {
    return (String) getRowValue(passwordComplexity);
  }

  public String getNeedusbnetwork() {
    return (String) getRowValue(needusbnetwork);
  }

  public String getNeeddynapassdefault() {
    return (String) getRowValue(needdynapassdefault);
  }

  public String getNeedusbdefault() {
    return (String) getRowValue(needusbdefault);
  }

  public String getMobileShowSet() {
    return (String) getRowValue(mobileShowSet);
  }

  public String getMobileShowType() {
    return (String) getRowValue(mobileShowType);
  }

  public String getMobileShowTypeDefault() {
    return (String) getRowValue(mobileShowTypeDefault);
  }

  public String getForbidLogin() {
    return (String) getRowValue(forbidLogin);
  }

  public String getLoginMustUpPswd() {
    return (String) getRowValue(loginMustUpPswd);
  }

  public String getNeedusbdefaultDt() {
    return (String) getRowValue(needusbdefaultDt);
  }

  public String getNeedusbdefaultHt() {
    return (String) getRowValue(needusbdefaultHt);
  }

  public String getNeedusbDt() {
    return (String) getRowValue(needusbDt);
  }

  public String getNeedusbHt() {
    return (String) getRowValue(needusbHt);
  }

  public String getValiditySec() {
    return (String) getRowValue(validitySec);
  }

  public String getNeedpassword() {
    return (String) getRowValue(needpassword);
  }

  public String getCheckkey() {
    return (String) getRowValue(checkkey);
  }

  public String getCheckIsEdit() {
    return (String) getRowValue(checkIsEdit);
  }

  public String getDefaultResult() {
    return (String) getRowValue(defaultResult);
  }

  public String getDefaultTree() {
    return (String) getRowValue(defaultTree);
  }

  public String getBirthshowfieldcolor() {
    return (String) getRowValue(birthshowfieldcolor);
  }

  public String getBirthshowcontentcolor() {
    return (String) getRowValue(birthshowcontentcolor);
  }

  public String getBirthshowfieldWF() {
    return (String) getRowValue(birthshowfieldWF);
  }

  public String getCheckUnJob() {
    return (String) getRowValue(checkUnJob);
  }

  public String getStatusWithContract() {
    return (String) getRowValue(statusWithContract);
  }

  public String getCheckSysValidate() {
    return (String) getRowValue(checkSysValidate);
  }

  public String getNeedPasswordLockMin() {
    return (String) getRowValue(needPasswordLockMin);
  }

  public String getPasswordLockMin() {
    return (String) getRowValue(passwordLockMin);
  }

  public String getCetificatePath() {
    return (String) getRowValue(cetificatePath);
  }

  public String getNeedCA() {
    return (String) getRowValue(needCA);
  }

  public String getneedCAdefault() {
    return (String) getRowValue(needCAdefault);
  }

  public String getMobileScanCA() {
    return (String) getRowValue(mobileScanCA);
  }

  public String getEntryDialogStyle() {
    return (String) getRowValue(entryDialogStyle);
  }

  public String getEntryValid() {
    return (String) getRowValue(entryValid);
  }

  public String getEntryCongrats() {
    return (String) getRowValue(entryCongrats);
  }

  public String getEntryCongratsColor() {
    return (String) getRowValue(entryCongratsColor);
  }

  public String getEntryFont() {
    return (String) getRowValue(entryFont);
  }

  public String getEntryFontSize() {
    return (String) getRowValue(entryFontSize);
  }

  public String getNeedforgotpassword() {
    return (String) getRowValue(needforgotpassword);
  }

  public String getForgotpasswordmode() {
    return (String) getRowValue(forgotpasswordmode);
  }

  public String getForgotpasswordExpiration() {
    return (String) getRowValue(forgotpasswordExpiration);
  }

  public String getForgotpasswordrelieve() {
    return (String) getRowValue(forgotpasswordrelieve);
  }

  public String getCanEditPhoto() {
    return (String) getRowValue(canEditPhoto);
  }

  public String getCanEditUserIcon() {
    return (String) getRowValue(canEditUserIcon);
  }

  public String getNeedCAdefault() {
    return (String) getRowValue(needCAdefault);
  }

  public String getSecondNeedDynapass() {
    return (String) getRowValue(secondNeedDynapass);
  }

  public String getSecondDynapassValidityMin() {
    return (String) getRowValue(secondDynapassValidityMin);
  }

  public String getSecondNeedusbDt() {
    return (String) getRowValue(secondNeedusbDt);
  }

  public String getSecondValidityDtMin() {
    return (String) getRowValue(secondValidityDtMin);
  }

  public String getSecondPassword() {
    return (String) getRowValue(secondPassword);
  }

  public String getSecondPasswordMin() {
    return (String) getRowValue(secondPasswordMin);
  }

  public String getAddressCA() {
    return (String) getRowValue(addressCA);
  }

  public String getCADefault() {
    return (String) getRowValue(CADefault);
  }

  public String getSecondCA() {
    return (String) getRowValue(secondCA);
  }

  public String getSecondValidityCAMin() {
    return (String) getRowValue(secondCAValidityMin);
  }

  public String getAddressCL() {
    return (String) getRowValue(addressCL);
  }
  public String getNeedCL() {
    return (String) getRowValue(needCL);
  }

  public String getNeedCLdefault() {
    return (String) getRowValue(needCLdefault);
  }

  public String getAppKey() {
    return (String) getRowValue(appKey);
  }

  public String getAppSecret() {
    return (String) getRowValue(appSecret);
  }

  public String getClAuthtype() {
    return (String) getRowValue(clAuthtype);
  }

  public String getClAuthTypeDefault() {
    return (String) getRowValue(clAuthTypeDefault);
  }

  public String getHideSuccessStatusPage() {
    return (String) getRowValue(hideSuccessStatusPage);
  }

  public String getSecondCL() {
    return (String) getRowValue(secondCL);
  }

  public String getSecondCLValidityMin() {
    return (String) getRowValue(secondCLValidityMin);
  }

  public String getSecondFace() {
    return (String) getRowValue(secondFace);
  }

  public String getSecondFaceValidityMin() {
    return (String) getRowValue(secondFaceValidityMin);
  }
  public String getQRCode() {
    return (String) getRowValue(qrCode);
  }

  public String getQRCodeDefault() {
    return (String) getRowValue(qrCodeDefault);
  }

  public String getAllowUserSetting() {
    return (String) getRowValue(allowUserSetting);
  }

  public String getInitialPwdValidity() {
    return (String) getRowValue(initialPwdValidity);
  }

  public String getUnRegPwdLock() {
    return (String) getRowValue(unRegPwdLock);
  }

  public String getUnRegPwdLockDays() {
    return (String) getRowValue(unRegPwdLockDays);
  }

  public String getWeakPasswordDisable() {
    return (String) getRowValue(weakPasswordDisable);
  }

  public String getDefaultPasswordEnable() {
    return (String) getRowValue(defaultPasswordEnable);
  }

  public String getDefaultPassword() {
    return (String) getRowValue(defaultPassword);
  }

  public String getIsOpenClassification() {
    return (String) getRowValue(isOpenClassification);
  }

  public String getIsCrc() {
    return (String) getRowValue(isCrc);
  }

  public String getBirthDispatchImg() {
    return (String) getRowValue(birthDispatchImg);
  }


  public  String getPasswordReuse() {return (String) getRowValue(passwordReuse); }

  public  String getPasswordReuseNum() { return (String) getRowValue(passwordReuseNum); }


  public String getBirthDispatchShowField(){
    return (String) getRowValue(birthDispatchShowField);
  }

  public String getBirthDispatchImgId() {
    return (String) getRowValue(birthDispatchImgId);
  }

  public String getRemindperTime() {
    return (String) getRowValue(remindperTime);
  }

  public String getBirthremindperTime() {
    return (String) getRowValue(birthremindperTime);
  }

  public String getEpremindperTime() {
    return (String) getRowValue(epremindperTime);
  }

  public String getContractremindperTime() {
    return (String) getRowValue(contractremindperTime);
  }

  public String getSecondDynapassDefault() {
    return (String) getRowValue(secondDynapassDefault);
  }
  public String getSecondUsbDtDefault() {
    return (String) getRowValue(secondUsbDtDefault);
  }
  public String getSecondPasswordDefault() {
    return (String) getRowValue(secondPasswordDefault);
  }
  public String getSecondCADefault() {
    return (String) getRowValue(secondCADefault);
  }
  public String getSecondCLDefault() {
    return (String) getRowValue(secondCLDefault);
  }
  public String getSecondFaceDefault() {
    return (String) getRowValue(secondFaceDefault);
  }
}
