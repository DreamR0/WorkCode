/**
 * RemindSettings Created on 2004-12-23 13:28:43
 * <p>
 * Copyright(c) 2001-2004 Weaver Software Corp.All rights reserved.
 */
package weaver.hrm.settings;

import weaver.general.Util;

/**
 * Description: RemindSettings
 * Company: 泛微软件
 *
 * @author xiaofeng.zhang
 * @version 1.0 2004-12-23
 */

/**
 * 保存人力资源其他信息的缓存类
 */
public class RemindSettings {
    /*******************密码策略*******************/
    private int minPasslen;//密码最小长度
    private String passwordComplexity;// 密码复杂度
    private String valid;//密码变更提醒
    private String remindperiod;//提醒周期
    private String remindperTime;// 提醒时间
    private String PasswordChangeReminder;//是否启用密码强制修改
    private String ChangePasswordDays;//同一密码使用多少天后强制修改
    private String DaysToRemind; //强制修改前多少天提醒用户
    private String loginMustUpPswd;//首次登陆必须修改密码
    private String initialPwdValidity;//初始密码有效期
    private String passwordReuse;//已用密码禁止重复使用
    private String passwordReuseNum;//历史密码校验次数
    private String weakPasswordDisable;//禁止保存弱密码
    private String defaultPasswordEnable;//启用初始密码
    private String defaultPassword;//初始密码
    /*******************密码策略*******************/

    /*******************账号锁定*******************/
    private String openPasswordLock;//是否开启密码输入错误自动锁定
    private String sumPasswordLock;//输入密码错误累计多少次锁定账号
    private String NeedPasswordLockMin;//是否需要自动解锁
    private String PasswordLockMin;//多少分钟后自动解锁
    private String unRegPwdLock;//长时间未登录自动锁定开关
    private String unRegPwdLockDays;//多少天未登录自动锁定
    /*******************账号锁定*******************/


    /*******************登录设置*******************/
    private String logintype;//登录方式
    private String relogin;//允许同一用户重复登录
    private String needforgotpassword = "";//启用忘记密码功能
    private String forgotpasswordmode = "";//密码找回方式
    private String forgotpasswordExpiration = "";//忘记密码短信和邮件验证码失效时间
    private String forgotpasswordrelieve = "";//找回密码时同步解除账号锁定
    private String forbidLogin;//禁止在网段外登录
    private int needvalidate;//是否启用验证码
    private int validatetype;//验证码类型
    private int validatenum;//验证码位数
    private int numvalidatewrong;//--已废弃
    /*******************登录设置*******************/

    /*******************其他设置*******************/
    private String checkkey;//外部用户接口检验码
    private String checkUnJob;//非在职人员信息查看控制，启用后，只有有“离职人员查看”权限的用户才能检索非在职人员
    private String checkIsEdit;//个人信息编辑权限是否哦开放到个人
    private String checkSysValidate;//系统信息批量设置验证码控制 启用后，系统信息批量设置保存的时候需要输入验证码--已废弃
    private String canEditPhoto = "";//是否允许个人编辑照片
    private String canEditUserIcon = "";//是否允许个人头像
    /*******************其他设置*******************/


    /*******************动态密码保护*******************/
    private int needdynapass;//动态密码是否允许作为登录辅助校验
    private int needdynapassdefault;//默认启用方式
    private int dynapasslen;//动态密码长度
    private String dypadcon;//动态密码内容
    private String validitySec;//动态密码有效期（单位：秒）
    private String needpassword;//需要登录密码
    private String secondNeedDynapass = "";//动态密码允许作为二次身份校验
    private String secondDynapassValidityMin = "";//动态密码免密时间(分钟)
    /*******************动态密码保护*******************/

    /*******************动态令牌*******************/
    private String needusbDt;//动态令牌是否允许作为登录辅助校验
    private String needusbdefaultDt;//默认启用方式
    private String secondNeedusbDt = "";//动态令牌允许作为二次身份校验
    private String secondValidityDtMin = "";//动态令牌免密时间(分钟)
    /*******************动态令牌*******************/

    /*海泰key--已废弃*/
    private String needusbHt;//海泰key是否允许作为登录辅助校验
    private String needusbdefaultHt;//默认启用方式
    /*海泰key--已废弃*/

    /*******************二次验证密码*******************/
    private String secondPassword = "";//密码允许作为二次身份校验
    private String secondPasswordMin = "";//密码免密时间(分钟)
    /*******************二次验证密码*******************/

    /*******************CA扫码登录*******************/
    private String mobileScanCA;//CA扫码登录是否允许作为登录辅助校验
    private String CADefault = "";//CA默认启用方式
    private String secondCA = "";//CA允许作为二次身份校验
    private String secondCAValidityMin = "";//CA免密时间(分钟)

    private String needcadefault;
    private String addressCA = "";//CA云服务地址
    private String needca;
    private String cetificatePath;
    /*******************CA扫码登录*******************/


    /*******************契约锁*******************/
    private String needCL = "";//契约锁是否允许作为登录辅助校验
    private String needCLdefault = "";//契约锁默认启用方式
    private String secondCL = "";//契约锁允许作为二次身份校验
    private String secondCLValidityMin = "";//契约锁免密时间(分钟)

    private String addressCL = "";//契约锁云服务地址
    private String appKey = ""; //契约锁企业AppKey
    private String appSecret = ""; //契约锁AppKey秘钥
    private String clAuthtype = "";//契约锁认证方式
    private String clAuthTypeDefault = "";//默认契约锁认证方式
    private String hideSuccessStatusPage = "";//契约锁认证通过后隐藏成功页
    /*******************契约锁*******************/

    /*******************人脸识别*******************/
    private String secondFace = "";
    private String secondFaceValidityMin = "";
    /*******************人脸识别*******************/

    /*******************PC端强制扫码登录*******************/
    String qrCode = "";//允许作为登录辅助校验
    String qrCodeDefault = "";//默认启用方式
    /*******************PC端强制扫码登录*******************/

    /*******************二次认证允许个人设置*******************/
    String allowUserSetting = "";//二次认证允许个人设置
    /*******************二次认证允许个人设置*******************/

    /*******************员工关怀提醒--生日提醒-管理员提醒*******************/
    private String birthvalidadmin = "";//是否开启提醒
    private String birthremindperiod;//提前提醒天数
    private String birthremindperTime;

    private String epremindperTime; // 员工生日消息提醒时间
    private String brithalarmadminscope = "";//--已废弃
    /*******************员工关怀提醒--生日提醒-管理员提醒*******************/

    /*******************员工关怀提醒--生日提醒-员工提醒*******************/
    private String birthvalid;//是否开启提醒
    private String birthremindmode;//提醒方式：1-消息中心、2-弹出窗口
    private String birthDispatchImg;//推送图片
    private String birthDispatchImgId;//推送图片
    private String birthDispatchShowField;//消息中心显示字段
    private String birthshowfield = "";//弹出窗口方式的显示字段
    private String birthshowfieldWF;//消息中心方式的显示字段
    private String congratulation;//弹出窗口方式的生日祝词
    private String congratulation1;//消息中心方式的生日祝词
    private String brithalarmscope = "";//提醒范围

    private String birthshowfieldcolor;//弹出窗口方式的显示字段颜色
    private String birthshowcontentcolor;//弹出窗口方式的祝词字体颜色
    private String birthdialogstyle = "";//弹出窗口方式的弹窗样式(值为图片存储在系统内的ID)
    /*******************员工关怀提醒--生日提醒-员工提醒*******************/

    /*******************入职一周年提醒设置*******************/
    private String entryValid = "";//是否开启提醒
    private String entryCongrats = "";//周年寄语
    private String entryCongratsColor = "";//周年寄语字体颜色
    private String entryFont = "";//周年寄语字体
    private String entryFontSize = "";//周年寄语字体大小
    private String entryDialogStyle = "";//弹窗提醒图片
    /*******************入职一周年提醒设置*******************/

    /*******************合同到期提醒*******************/
    private String contractvalid = "";//是否启用合同到期提醒
    private String contractremindperiod = "";//提前几天提醒
    private String contractremindperTime; // 合同提醒时间
    private String statusWithContract;//是否开启合同到期后自动将人员置为“无效”状态
    /*******************合同到期提醒*******************/

    /*******************入职提醒*******************/
    private String entervalid;//是否开启入职提醒
    /*******************入职提醒*******************/

    /*移动电话隐私设置--已废弃*/
    private String mobileShowSet;//是否允许个人设置
    private String mobileShowType;//可设置项
    private String mobileShowTypeDefault;//默认启用方式
    /*移动电话隐私设置--已废弃*/

    /*usb--已废弃*/
    private String needusb;//是否启用辅助校验
    private String usbType;//辅助校验方式：2-海泰key、3-动态令牌、4-动态密码
    private String needusbdefault;//默认启用方式
    private String needusbnetwork;//辅助校验是否需要网段策略
    /*usb--已废弃*/

    /*其他*/
    private String firmcode;//验证码
    private String usercode;//用户代码
    private String needDactylogram;//是否需要使用维尔指纹验证设备登录认证功能
    private String canModifyDactylogram;//是否允许客户端修改指纹
    private String defaultResult;//默认结果
    private String defaultTree;//默认树结构


    //是否开启分级保护
    private String isOpenClassification;
    private String isCrc;
    private String secondDynapassDefault;
    private String secondUsbDtDefault;
    private String secondPasswordDefault;
    private String secondCADefault;
    private String secondCLDefault;
    private String secondFaceDefault;
    private String newResourceMode;

    public String getNewResourceMode() {
        return newResourceMode;
    }

    public void setNewResourceMode(String newResourceMode) {
        this.newResourceMode = newResourceMode;
    }

    public String getMobileScanCA() {
        return mobileScanCA;
    }

    public void setMobileScanCA(String mobileScanCA) {
        this.mobileScanCA = mobileScanCA;
    }


    public String getCetificatePath() {
        return cetificatePath;
    }

    public void setCetificatePath(String cetificatePath) {
        this.cetificatePath = cetificatePath;
    }


    public String getValiditySec() {
        return validitySec;
    }

    public void setValiditySec(String validitySec) {
        this.validitySec = validitySec;
    }
    public String getNeedpassword() {
        return needpassword;
    }

    public void setNeedpassword(String needpassword) {
        this.needpassword = needpassword;
    }

    public String getCheckkey() {
        return checkkey;
    }

    public void setCheckkey(String checkkey) {
        this.checkkey = checkkey;
    }

    public String getNeedusbdefaultDt() {
        return needusbdefaultDt;
    }

    public void setNeedusbdefaultDt(String needusbdefaultDt) {
        this.needusbdefaultDt = needusbdefaultDt;
    }

    public String getNeedusbdefaultHt() {
        return needusbdefaultHt;
    }

    public void setNeedusbdefaultHt(String needusbdefaultHt) {
        this.needusbdefaultHt = needusbdefaultHt;
    }

    public String getNeedusbDt() {
        return needusbDt;
    }

    public void setNeedusbDt(String needusbDt) {
        this.needusbDt = needusbDt;
    }

    public String getNeedusbHt() {
        return needusbHt;
    }

    public void setNeedusbHt(String needusbHt) {
        this.needusbHt = needusbHt;
    }

    public String getForbidLogin() {
        return forbidLogin;
    }

    public void setForbidLogin(String forbidLogin) {
        this.forbidLogin = forbidLogin;
    }

    public String getLoginMustUpPswd() {
        return loginMustUpPswd;
    }

    public void setLoginMustUpPswd(String loginMustUpPswd) {
        this.loginMustUpPswd = loginMustUpPswd;
    }

    public String getDaysToRemind() {
        return DaysToRemind;
    }

    public void setDaysToRemind(String daysToRemind) {
        this.DaysToRemind = daysToRemind;
    }

    public String getPasswordChangeReminder() {
        return PasswordChangeReminder;
    }

    public void setPasswordChangeReminder(String passwordChangeReminder) {
        this.PasswordChangeReminder = passwordChangeReminder;
    }

    public String getChangePasswordDays() {
        return ChangePasswordDays;
    }

    public void setChangePasswordDays(String changePasswordDays) {
        this.ChangePasswordDays = changePasswordDays;
    }

    public String getOpenPasswordLock() {
        return openPasswordLock;
    }

    public void setOpenPasswordLock(String openPasswordLock) {
        this.openPasswordLock = openPasswordLock;
    }

    public String getSumPasswordLock() {
        return sumPasswordLock;
    }

    public void setSumPasswordLock(String sumPasswordLock) {
        this.sumPasswordLock = sumPasswordLock;
    }

    public String getPasswordComplexity() {
        return passwordComplexity;
    }

    public void setPasswordComplexity(String passwordComplexity) {
        this.passwordComplexity = passwordComplexity;
    }



    public String getLogintype() {
        return logintype;
    }
    public void setLogintype(String logintype) {
        this.logintype = logintype;
    }
    /**
     * 获取是否可以重复登陆
     *
     * @return
     */
    public String getRelogin() {
        return relogin;
    }

    /**
     * 设置是否可以重复登陆
     *
     * @param relogin 是否可以重复登陆
     */
    public void setRelogin(String relogin) {
        this.relogin = relogin;
    }

    /**
     * 获取生日提醒模式
     *
     * @return
     */
    public String getBirthremindmode() {
        return birthremindmode;
    }

    /**
     * 设置生日提醒模式
     *
     * @param birthremindmode 生日提醒模式
     */
    public void setBirthremindmode(String birthremindmode) {
        this.birthremindmode = birthremindmode;
    }

    /**
     * 获取提醒提前天数
     *
     * @return
     */
    public String getRemindperiod() {
        return remindperiod;
    }

    /**
     * 设置提醒提前天数
     *
     * @param remindperiod 提醒提前天数
     */
    public void setRemindperiod(String remindperiod) {
        this.remindperiod = remindperiod;
    }

    public String getBirthdialogstyle() {
        return birthdialogstyle;
    }

    public void setBirthdialogstyle(String birthdialogstyle) {
        this.birthdialogstyle = birthdialogstyle;
    }

    public String getBirthshowfield() {
        return birthshowfield;
    }

    public void setBirthshowfield(String birthshowfield) {
        this.birthshowfield = birthshowfield;
    }

    public String getBrithalarmscope() {
        return brithalarmscope;
    }

    public void setBrithalarmscope(String brithalarmscope) {
        this.brithalarmscope = brithalarmscope;
    }

    public String getBirthvalidadmin() {
        return birthvalidadmin;
    }

    public void setBirthvalidadmin(String birthvalidadmin) {
        this.birthvalidadmin = birthvalidadmin;
    }

    public String getBrithalarmadminscope() {
        return brithalarmadminscope;
    }

    public void setBrithalarmadminscope(String brithalarmadminscope) {
        this.brithalarmadminscope = brithalarmadminscope;
    }

    public String getContractvalid() {
        return contractvalid;
    }

    public void setContractvalid(String contractvalid) {
        this.contractvalid = contractvalid;
    }

    public String getContractremindperiod() {
        return contractremindperiod;
    }

    public void setContractremindperiod(String contractremindperiod) {
        this.contractremindperiod = contractremindperiod;
    }

    public String getEntervalid() {
        return entervalid;
    }

    public void setEntervalid(String entervalid) {
        this.entervalid = entervalid;
    }

    /**
     * 获取是否提醒
     *
     * @return
     */
    public String getValid() {
        return valid;
    }

    /**
     * 设置是否提醒
     *
     * @param valid 是否提醒
     */
    public void setValid(String valid) {
        this.valid = valid;
    }

    /**
     * 获取生日提醒提前天数
     *
     * @return
     */
    public String getBirthremindperiod() {
        return birthremindperiod;
    }

    /**
     * 设置生日提醒提前天数
     *
     * @param birthremindperiod 生日提醒提前天数
     */
    public void setBirthremindperiod(String birthremindperiod) {
        this.birthremindperiod = birthremindperiod;
    }

    /**
     * 获取祝福语
     *
     * @return
     */
    public String getCongratulation() {
        return congratulation;
    }

    /**
     * 设置祝福语
     *
     * @param congratulation 祝福语
     */
    public void setCongratulation(String congratulation) {
        this.congratulation = congratulation;
    }

    /**
     * 获取祝福语
     *
     * @return
     */
    public String getCongratulation1() {
        return congratulation1;
    }

    /**
     * 设置祝福语
     *
     * @param congratulation 祝福语
     */
    public void setCongratulation1(String congratulation1) {
        this.congratulation1 = congratulation1;
    }

    /**
     * 获取是否启用生日提醒
     *
     * @return
     */
    public String getBirthvalid() {
        return birthvalid;
    }

    /**
     * 设置是否启用生日提醒
     *
     * @param birthvalid 是否启用生日提醒
     */
    public void setBirthvalid(String birthvalid) {
        this.birthvalid = birthvalid;
    }

    /**
     * 获取是否启用usb
     *
     * @return
     */
    public String getNeedusb() {
        return needusb;
    }

    /**
     * usb类型,现包括2种
     *
     * @param usbType usb类型
     */
    public void setUsbType(String usbType) {
        this.usbType = usbType;
    }

    /**
     * 获取是否启用usb
     *
     * @return
     */
    public String getUsbType() {
        return usbType;
    }

    /**
     * 设置是否启用usb
     *
     * @param needusb 是否启用usb
     */
    public void setNeedusb(String needusb) {
        this.needusb = needusb;
    }

    /**
     * 获取usb公司码
     *
     * @return
     */
    public String getFirmcode() {
        return firmcode;
    }

    /**
     * 设置usb公司码
     *
     * @param firmcode usb公司码
     */
    public void setFirmcode(String firmcode) {
        this.firmcode = firmcode;
    }

    /**
     * 获取usb用户码
     *
     * @return
     */
    public String getUsercode() {
        return usercode;
    }

    /**
     * 设置usb用户码
     *
     * @param usercode usb用户码
     */
    public void setUsercode(String usercode) {
        this.usercode = usercode;
    }


    /**
     * 返回是否启用验证码，0: 否,1: 是
     *
     * @author sean.yang
     */
    public int getNeedvalidate() {
        return needvalidate;
    }

    /**
     * 设置是否启用验证码，0: 否,1: 是
     *
     * @author sean.yang
     */
    public void setNeedvalidate(int needvalidate) {
        this.needvalidate = needvalidate;
    }

    /**
     * 返回验证码类型，0：数字；1：字母；2：汉字
     *
     * @author sean.yang
     */
    public int getValidatetype() {
        return validatetype;
    }

    /**
     * 设置验证码类型，0：数字；1：字母；2：汉字
     *
     * @author sean.yang
     */
    public void setValidatetype(int validatetype) {
        this.validatetype = validatetype;
    }

    /**
     * 设置验证码位数
     *
     * @author sean.yang
     */
    public int getValidatenum() {
        return validatenum;
    }

    /**
     * 返回验证码位数
     *
     * @author sean.yang
     */
    public void setValidatenum(int validatenum) {
        this.validatenum = validatenum;
    }

    /**
     * 设置验证码允许输错次数
     *
     * @author sean.yang
     */
    public int getNumvalidatewrong() {
        return numvalidatewrong;
    }

    /**
     * 返回验证码允许输错次数
     *
     * @author sean.yang
     */
    public void setNumvalidatewrong(int numvalidatewrong) {
        this.numvalidatewrong = numvalidatewrong;
    }


    public int getMinPasslen() {
        return minPasslen;
    }

    /**
     * 最小密码位数
     *
     * @param minPasslen
     */
    public void setMinPasslen(int minPasslen) {
        this.minPasslen = minPasslen;
    }

    /**
     * 动态密码
     *
     * @return
     */
    public int getNeeddynapass() {
        return needdynapass;
    }

    public void setNeeddynapass(int needdynapass) {
        this.needdynapass = needdynapass;
    }

    public int getDynapasslen() {
        return dynapasslen;
    }

    public String getDypadcon() {
        return dypadcon;
    }

    /*
     * 动态密码内容
     * */
    public void setDypadcon(String dypadcon) {
        this.dypadcon = dypadcon;
    }

    public void setDynapasslen(int dynapasslen) {
        this.dynapasslen = dynapasslen;
    }

    public void setNeedDactylogram(String needDactylogram) {
        this.needDactylogram = needDactylogram;
    }

    public String getNeedDactylogram() {
        return needDactylogram;
    }

    public void setCanModifyDactylogram(String canModifyDactylogram) {
        this.canModifyDactylogram = canModifyDactylogram;
    }

    public String getCanModifyDactylogram() {
        return canModifyDactylogram;
    }

    /*
     * 是否启用USB网段策略
     * */
    public void setNeedusbnetwork(String needusbnetwork) {
        this.needusbnetwork = needusbnetwork;
    }

    public String getNeedusbnetwork() {
        return needusbnetwork;
    }

    public String getNeedusbdefault() {
        return needusbdefault;
    }

    public void setNeedusbdefault(String needusbdefault) {
        this.needusbdefault = needusbdefault;
    }

    public int getNeeddynapassdefault() {
        return needdynapassdefault;
    }

    public void setNeeddynapassdefault(int needdynapassdefault) {
        this.needdynapassdefault = needdynapassdefault;
    }

    public String getMobileShowSet() {
        return mobileShowSet;
    }

    public void setMobileShowSet(String mobileShowSet) {
        this.mobileShowSet = mobileShowSet;
    }

    public String getMobileShowType() {
        return mobileShowType;
    }

    public void setMobileShowType(String mobileShowType) {
        this.mobileShowType = mobileShowType;
    }

    public String getMobileShowTypeDefault() {
        return mobileShowTypeDefault;
    }

    public void setMobileShowTypeDefault(String mobileShowTypeDefault) {
        this.mobileShowTypeDefault = mobileShowTypeDefault;
    }

    public String getDefaultResult() {
        return defaultResult;
    }

    public void setDefaultResult(String defaultResult) {
        this.defaultResult = defaultResult;
    }

    public String getDefaultTree() {
        return defaultTree;
    }

    public void setDefaultTree(String defaultTree) {
        this.defaultTree = defaultTree;
    }

    public String getBirthshowfieldcolor() {
        return birthshowfieldcolor;
    }

    public void setBirthshowfieldcolor(String birthshowfieldcolor) {
        this.birthshowfieldcolor = birthshowfieldcolor;
    }

    public String getBirthshowcontentcolor() {
        return birthshowcontentcolor;
    }

    public void setBirthshowcontentcolor(String birthshowcontentcolor) {
        this.birthshowcontentcolor = birthshowcontentcolor;
    }

    public String getBirthshowfieldWF() {
        return birthshowfieldWF;
    }

    public void setBirthshowfieldWF(String birthshowfieldWF) {
        this.birthshowfieldWF = birthshowfieldWF;
    }

    public String getCheckUnJob() {
        return checkUnJob;
    }

    public void setCheckUnJob(String checkUnJob) {
        this.checkUnJob = checkUnJob;
    }

    public String getCheckIsEdit() {
        return checkIsEdit;
    }

    public void setCheckIsEdit(String checkIsEdit) {
        this.checkIsEdit = checkIsEdit;
    }

    public String getStatusWithContract() {
        return statusWithContract;
    }

    public void setStatusWithContract(String statusWithContract) {
        this.statusWithContract = statusWithContract;
    }

    public String getCheckSysValidate() {
        return checkSysValidate;
    }

    public void setCheckSysValidate(String checkSysValidate) {
        this.checkSysValidate = checkSysValidate;
    }

    public String getNeedPasswordLockMin() {
        return NeedPasswordLockMin;
    }

    public void setNeedPasswordLockMin(String needPasswordLockMin) {
        NeedPasswordLockMin = needPasswordLockMin;
    }

    public String getPasswordLockMin() {
        return PasswordLockMin;
    }

    public void setPasswordLockMin(String passwordLockMin) {
        PasswordLockMin = passwordLockMin;
    }


    public String getNeedca() {
        return needca;
    }

    public void setNeedca(String needca) {
        this.needca = needca;
    }

    public String getNeedcadefault() {
        return needcadefault;
    }

    public void setNeedcadefault(String needcadefault) {
        this.needcadefault = needcadefault;
    }

    public String getEntryDialogStyle() {
        return entryDialogStyle;
    }

    public void setEntryDialogStyle(String entryDialogStyle) {
        this.entryDialogStyle = entryDialogStyle;
    }

    public String getEntryValid() {
        return entryValid;
    }

    public void setEntryValid(String entryValid) {
        this.entryValid = entryValid;
    }

    public String getEntryCongrats() {
        return entryCongrats;
    }

    public void setEntryCongrats(String entryCongrats) {
        this.entryCongrats = entryCongrats;
    }

    public String getEntryCongratsColor() {
        return entryCongratsColor;
    }

    public void setEntryCongratsColor(String entryCongratsColor) {
        this.entryCongratsColor = entryCongratsColor;
    }

    public String getEntryFont() {
        return entryFont;
    }

    public void setEntryFont(String entryFont) {
        this.entryFont = entryFont;
    }

    public String getEntryFontSize() {
        return entryFontSize;
    }

    public void setEntryFontSize(String entryFontSize) {
        this.entryFontSize = entryFontSize;
    }

    public String getNeedforgotpassword() {
        return needforgotpassword;
    }

    public void setNeedforgotpassword(String needforgotpassword) {
        this.needforgotpassword = needforgotpassword;
    }

    public String getForgotpasswordmode() {
        return forgotpasswordmode;
    }

    public void setForgotpasswordmode(String forgotpasswordmode) {
        this.forgotpasswordmode = forgotpasswordmode;
    }

  public String getForgotpasswordExpiration() {
    return forgotpasswordExpiration;
  }

  public void setForgotpasswordExpiration(String forgotpasswordExpiration) {
    this.forgotpasswordExpiration = forgotpasswordExpiration;
  }

    public String getForgotpasswordrelieve() {
        return forgotpasswordrelieve;
    }

    public void setForgotpasswordrelieve(String forgotpasswordrelieve) {
        this.forgotpasswordrelieve = forgotpasswordrelieve;
    }

    public String getCanEditPhoto() {
		return canEditPhoto;
	}

	public void setCanEditPhoto(String canEditPhoto) {
		this.canEditPhoto = canEditPhoto;
	}

	public String getCanEditUserIcon() {
		return canEditUserIcon;
	}

	public void setCanEditUserIcon(String canEditUserIcon) {
		this.canEditUserIcon = canEditUserIcon;
	}

    public String getSecondNeedDynapass() {
        return secondNeedDynapass;
    }

    public void setSecondNeedDynapass(String secondNeedDynapass) {
        this.secondNeedDynapass = secondNeedDynapass;
    }

    public String getSecondDynapassValidityMin() {
        return secondDynapassValidityMin;
    }

    public void setSecondDynapassValidityMin(String secondDynapassValidityMin) {
        this.secondDynapassValidityMin = secondDynapassValidityMin;
    }

    public String getSecondNeedusbDt() {
        return secondNeedusbDt;
    }

    public void setSecondNeedusbDt(String secondNeedusbDt) {
        this.secondNeedusbDt = secondNeedusbDt;
    }

    public String getSecondValidityDtMin() {
        return secondValidityDtMin;
    }

    public void setSecondValidityDtMin(String secondValidityDtMin) {
        this.secondValidityDtMin = secondValidityDtMin;
    }

    public String getSecondPassword() {
        return secondPassword;
    }

    public void setSecondPassword(String secondPassword) {
        this.secondPassword = secondPassword;
    }

    public String getSecondPasswordMin() {
        return secondPasswordMin;
    }

    public void setSecondPasswordMin(String secondPasswordMin) {
        this.secondPasswordMin = secondPasswordMin;
    }

    public String getAddressCA() {
        return addressCA;
    }

    public void setAddressCA(String addressCA) {
        this.addressCA = addressCA;
    }

    public String getCADefault() {
        return CADefault;
    }

    public void setCADefault(String CADefault) {
        this.CADefault = CADefault;
    }

    public String getSecondCA() {
        return secondCA;
    }

    public void setSecondCA(String secondCA) {
        this.secondCA = secondCA;
    }

    public String getSecondCAValidityMin() {
        return secondCAValidityMin;
    }

    public void setSecondCAValidityMin(String secondCAValidityMin) {
        this.secondCAValidityMin = secondCAValidityMin;
    }

    public String getAddressCL() {
        return addressCL;
    }

    public void setAddressCL(String addressCL) {
        this.addressCL = addressCL;
    }

    public String getSecondCL() {
        return secondCL;
    }

    public void setSecondCL(String secondCL) {
        this.secondCL = secondCL;
    }

    public String getSecondCLValidityMin() {
        return secondCLValidityMin;
    }

    public void setSecondCLValidityMin(String secondCLValidityMin) {
        this.secondCLValidityMin = secondCLValidityMin;
    }

    public String getSecondFace() {
        return secondFace;
    }
    public void setSecondFace(String secondFace) {
        this.secondFace = secondFace;
    }

    public String getSecondFaceValidityMin() {
        return secondFaceValidityMin;
    }
    public void setSecondFaceValidityMin(String secondFaceValidityMin) {
        this.secondFaceValidityMin = secondFaceValidityMin;
    }

    public String getQRCode() {
        return qrCode;
    }

    public void setQRCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQRCodeDefault() {
        return qrCodeDefault;
    }

    public void setQRCodeDefault(String qrCodeDefault) {
        this.qrCodeDefault = qrCodeDefault;
    }
    public String getAllowUserSetting() {
        return allowUserSetting;
    }

    public void setAllowUserSetting(String allowUserSetting) {
        this.allowUserSetting = allowUserSetting;
    }

    public String getInitialPwdValidity() {
        return initialPwdValidity;
    }

    public void setInitialPwdValidity(String initialPwdValidity) {
        this.initialPwdValidity = initialPwdValidity;
    }

    public String getUnRegPwdLock() {
        return unRegPwdLock;
    }

    public void setUnRegPwdLock(String unRegPwdLock) {
        this.unRegPwdLock = unRegPwdLock;
    }

    public String getUnRegPwdLockDays() {
        return unRegPwdLockDays;
    }

    public void setUnRegPwdLockDays(String unRegPwdLockDays) {
        this.unRegPwdLockDays = unRegPwdLockDays;
    }
    public String getPasswordReuse() {
        return passwordReuse;
    }

    public void setPasswordReuse(String passwordReuse) {
        this.passwordReuse = passwordReuse;
    }
    public String getPasswordReuseNum() {
        return passwordReuseNum;
    }

    public void setPasswordReuseNum(String passwordReuseNum) {
        this.passwordReuseNum = passwordReuseNum;
    }

    public String getWeakPasswordDisable() {
        return weakPasswordDisable;
    }

    public void setWeakPasswordDisable(String weakPasswordDisable) {
        this.weakPasswordDisable = weakPasswordDisable;
    }

    public String getDefaultPasswordEnable() {
        return defaultPasswordEnable;
    }

    public void setDefaultPasswordEnable(String defaultPasswordEnable) {
        this.defaultPasswordEnable = defaultPasswordEnable;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    public String getIsOpenClassification() {
        return isOpenClassification;
    }

    public void setIsOpenClassification(String isOpenClassification) {
        this.isOpenClassification = isOpenClassification;
    }

    public String getIsCrc() {
        return isCrc;
    }

    public void setIsCrc(String isCrc) {
        this.isCrc = isCrc;
    }

    public String getBirthDispatchImg() {
        return birthDispatchImg;
    }

    public void setBirthDispatchImg(String birthDispatchImg) {
        this.birthDispatchImg = birthDispatchImg;
    }

    public String getBirthDispatchShowField() {
        return birthDispatchShowField;
    }

    public void setBirthDispatchShowField(String birthDispatchShowField) {
        this.birthDispatchShowField = birthDispatchShowField;
    }

    public String getBirthDispatchImgId() {
        return birthDispatchImgId;
    }

    public void setBirthDispatchImgId(String birthDispatchImgId) {
        this.birthDispatchImgId = birthDispatchImgId;
    }

    public String getRemindperTime() {
        return remindperTime;
    }

    public void setRemindperTime(String remindperTime) {
        this.remindperTime = remindperTime;
    }

    public String getBirthremindperTime() {
        return birthremindperTime;
    }

    public void setBirthremindperTime(String birthremindperTime) {
        this.birthremindperTime = birthremindperTime;
    }

    public String getEpremindperTime() {
        return epremindperTime;
    }

    public void setEpremindperTime(String epremindperTime) {
        this.epremindperTime = epremindperTime;
    }

    public String getContractremindperTime() {
        return contractremindperTime;
    }

    public void setContractremindperTime(String contractremindperTime) {
        this.contractremindperTime = contractremindperTime;
    }

    public String getSecondDynapassDefault() {
        return secondDynapassDefault;
    }
    public void setSecondDynapassDefault(String secondDynapassDefault) {
        this.secondDynapassDefault = secondDynapassDefault;
    }

    public String getSecondUsbDtDefault() {
        return secondUsbDtDefault;
    }
    public void setSecondUsbDtDefault(String secondUsbDtDefault) {
        this.secondUsbDtDefault = secondUsbDtDefault;
    }

    public String getSecondPasswordDefault() {
        return secondPasswordDefault;
    }
    public void setSecondPasswordDefault(String secondPasswordDefault) {
        this.secondPasswordDefault = secondPasswordDefault;
    }

    public String getSecondCADefault() {
        return secondCADefault;
    }
    public void setSecondCADefault(String secondCADefault) {
        this.secondCADefault = secondCADefault;
    }

    public String getSecondCLDefault() {
        return secondCLDefault;
    }
    public void setSecondCLDefault(String secondCLDefault) {
        this.secondCLDefault = secondCLDefault;
    }

    public String getSecondFaceDefault() {
        return secondFaceDefault;
    }
    public void setSecondFaceDefault(String secondFaceDefault) {
        this.secondFaceDefault = secondFaceDefault;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getClAuthtype() {
        if(Util.null2String(clAuthtype).equals("")){
            this.setClAuthtype("FACE");
        }
        return clAuthtype;
    }

    public void setClAuthtype(String clAuthtype) {
        this.clAuthtype = clAuthtype;
    }

    public String getClAuthTypeDefault() {
        return clAuthTypeDefault;
    }

    public void setClAuthTypeDefault(String clAuthTypeDefault) {
        this.clAuthTypeDefault = clAuthTypeDefault;
    }

    public String getHideSuccessStatusPage() {
        return hideSuccessStatusPage;
    }

    public void setHideSuccessStatusPage(String hideSuccessStatusPage) {
        this.hideSuccessStatusPage = hideSuccessStatusPage;
    }

    public String getNeedCL() {
        return needCL;
    }

    public void setNeedCL(String needCL) {
        this.needCL = needCL;
    }

    public String getNeedCLdefault() {
        return needCLdefault;
    }

    public void setNeedCLdefault(String needCLdefault) {
        this.needCLdefault = needCLdefault;
    }
}
