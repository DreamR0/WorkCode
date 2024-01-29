package weaver.hrm.settings;

import com.alibaba.fastjson.JSON;
import weaver.conn.ConnStatement;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.conn.RecordSet;
import java.util.*;


/**
 * Title:        cobusiness
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      weaver
 *
 * @author xf
 * @version 1.0
 */

public class HrmSettingsComInfo extends BaseBean {
    //private StaticObj staticobj = null;

    /*******************密码策略*******************/
    private String minPasslen;//密码最小长度
    private String passwordComplexity;// 密码复杂度
    private String valid;//密码变更提醒
    private String remindperiod;//提醒周期
    private String remindperTime;//提醒时间
    private String passwordChangeReminder;//是否启用密码强制修改
    private String changePasswordDays;//同一密码使用多少天后强制修改
    private String daysToRemind; //强制修改前多少天提醒用户
    private String loginMustUpPswd;//首次登陆必须修改密码
    private String initialPwdValidity;//初始密码有效期
    private String weakPasswordDisable;//禁止保存弱密码
    private String defaultPasswordEnable;//启用初始密码
    private String defaultPassword;//初始密码
    private String passwordReuse;//历史密码校验次数
    private String passwordReuseNum;//历史密码校验次数
    /*******************密码策略*******************/

    /*******************账号锁定*******************/
    private String openPasswordLock;//是否开启密码输入错误自动锁定
    private String sumPasswordLock;//输入密码错误累计多少次锁定账号
    private String needPasswordLockMin;//是否需要自动解锁
    private String passwordLockMin;//多少分钟后自动解锁
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
    private String needvalidate;//是否启用验证码
    private String validatetype;//验证码类型
    private String validatenum;//验证码位数
    private String numvalidatewrong;//--已废弃
    private String timeOutSwitch;//启用超时登出弹窗功能
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
    private String needdynapass;//动态密码是否允许作为登录辅助校验
    private String needdynapassdefault;//默认启用方式
    private String dynapasslen;//动态密码长度
    private String dypadcon;//动态密码内容
    private String validitySec;//动态密码有效期（单位：秒）
    private String needpassword;//动态密码是否需要密码
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

    private String needCAdefault;
    private String addressCA = "";//CA云服务地址
    private String needCA;
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
    private String birthremindperTime;// 提醒时间
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
    private String contractremindperTime; //提醒时间
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

    private String newResourceMode;

    private String secondDynapassDefault;
    private String secondUsbDtDefault;
    private String secondPasswordDefault;
    private String secondCADefault;
    private String secondCLDefault;
    private String secondFaceDefault;

    public HrmSettingsComInfo() {
        HrmSettingComInfo rs = new HrmSettingComInfo();
        if(rs.next()){
            birthremindmode = Util.null2String(rs.getBirthremindmode());
            birthDispatchImg = Util.null2String(rs.getBirthDispatchImg());
            birthDispatchImgId = Util.null2String(rs.getBirthDispatchImgId());
            birthDispatchShowField = Util.null2String(rs.getBirthDispatchShowField());
            birthremindperiod = Util.null2String(rs.getBirthremindperiod());
            birthremindperTime = Util.null2String(rs.getBirthremindperTime());
            epremindperTime = Util.null2String(rs.getEpremindperTime());
            birthvalid = Util.null2String(rs.getBirthvalid());
            congratulation = Util.null2String(rs.getCongratulation());
            congratulation1 = Util.null2String(rs.getCongratulation1());
            birthdialogstyle = Util.null2String(rs.getBirthdialogstyle());
            birthshowfield = Util.null2String(rs.getBirthshowfield());
            brithalarmscope = Util.null2String(rs.getBrithalarmscope());
            birthvalidadmin = Util.null2String(rs.getBirthvalidadmin());
            brithalarmadminscope = Util.null2String(rs.getBrithalarmadminscope());
            contractvalid = Util.null2String(rs.getContractvalid());
            contractremindperiod = Util.null2String(rs.getContractremindperiod());
            contractremindperTime = Util.null2String(rs.getContractremindperTime());
            entervalid = Util.null2String(rs.getEntervalid());
            dynapasslen = Util.null2String(rs.getDynapasslen());
            dypadcon = Util.null2String(rs.getDypadcon());
            firmcode = Util.null2String(rs.getFirmcode());
            minPasslen = Util.null2String(rs.getMinPasslen());
            passwordReuse = Util.null2String(rs.getPasswordReuse());
            passwordReuseNum = Util.null2String(rs.getPasswordReuseNum());
            needdynapass = Util.null2String(rs.getNeeddynapass());
            needdynapassdefault = Util.null2String(rs.getNeeddynapassdefault());
            needusb = Util.null2String(rs.getNeedusb());
            needusbdefault = Util.null2String(rs.getNeedusbdefault());
            needusbnetwork = Util.null2String(rs.getNeedusbnetwork());
            usbType = Util.null2String(rs.getUsbType());
            needvalidate = Util.null2String(rs.getNeedvalidate());
            logintype = Util.null2String(rs.getLogintype());
            timeOutSwitch = Util.null2String(rs.getTimeOutSwitch());
            relogin = Util.null2String(rs.getRelogin());
            remindperiod = Util.null2String(rs.getRemindperiod());
            usercode = Util.null2String(rs.getUsercode());
            valid = Util.null2String(rs.getValid());
            remindperTime = Util.null2String(rs.getRemindperTime());
            validatenum = Util.null2String(rs.getValidatenum());
            validatetype = Util.null2String(rs.getValidatetype());
            numvalidatewrong = Util.null2String(rs.getNumvalidatewrong());
            daysToRemind = Util.null2String(rs.getDaysToRemind());
            passwordChangeReminder = Util.null2String(rs.getPasswordChangeReminder());
            changePasswordDays = Util.null2String(rs.getChangePasswordDays());
            openPasswordLock = Util.null2String(rs.getOpenPasswordLock());
            sumPasswordLock = Util.null2String(rs.getSumPasswordLock());
            passwordComplexity = Util.null2String(rs.getPasswordComplexity());
            needDactylogram = Util.null2String(rs.getNeedDactylogram());
            canModifyDactylogram = Util.null2String(rs.getCanModifyDactylogram());
            mobileShowSet = Util.null2String(rs.getMobileShowSet());
            mobileShowType = Util.null2String(rs.getMobileShowType());
            mobileShowTypeDefault = Util.null2String(rs.getMobileShowTypeDefault());
            loginMustUpPswd = Util.null2String(rs.getLoginMustUpPswd());
            forbidLogin = Util.null2String(rs.getForbidLogin());
            needusbHt = Util.null2String(rs.getNeedusbHt());
            needusbdefaultHt = Util.null2String(rs.getNeedusbdefaultHt());
            needusbDt = Util.null2String(rs.getNeedusbDt());
            needusbdefaultDt = Util.null2String(rs.getNeedusbdefaultDt());
            validitySec = Util.null2String(rs.getValiditySec());
            needpassword = Util.null2String(rs.getNeedpassword());
            checkkey = Util.null2String(rs.getCheckkey(), "99adbd0f-c843-4f57-9ee2-6b77b1aa600a");
            checkUnJob = Util.null2String(rs.getCheckUnJob());
            checkIsEdit = Util.null2String(rs.getCheckIsEdit());
            statusWithContract = Util.null2String(rs.getStatusWithContract());
            checkSysValidate = Util.null2String(rs.getCheckSysValidate());
            defaultResult = Util.null2String(rs.getDefaultResult());
            defaultTree = Util.null2String(rs.getDefaultTree());
            birthshowfieldcolor = Util.null2String(rs.getBirthshowfieldcolor());
            birthshowcontentcolor = Util.null2String(rs.getBirthshowcontentcolor());
            birthshowfieldWF = Util.null2String(rs.getBirthshowfieldWF());
            needPasswordLockMin = Util.null2String(rs.getNeedPasswordLockMin());
            passwordLockMin = Util.null2String(rs.getPasswordLockMin());

            needCA = Util.null2String(rs.getNeedCA());
            needCAdefault = Util.null2String(rs.getNeedCAdefault());
            cetificatePath = Util.null2String(rs.getCetificatePath());
            mobileScanCA = Util.null2String(rs.getMobileScanCA());
            //入职一周年提醒设置 start
            entryDialogStyle = Util.null2String(rs.getEntryDialogStyle());
            entryValid = Util.null2String(rs.getEntryValid());
            entryCongrats = Util.null2String(rs.getEntryCongrats());
            entryCongratsColor = Util.null2String(rs.getEntryCongratsColor());
            entryFont = Util.null2String(rs.getEntryFont());
            entryFontSize = Util.null2String(rs.getEntryFontSize());
            //入职一周年提醒设置 end

            //忘记密码设置
            needforgotpassword = Util.null2String(rs.getNeedforgotpassword());
            forgotpasswordmode = Util.null2String(rs.getForgotpasswordmode());
            forgotpasswordExpiration = Util.null2String(rs.getForgotpasswordExpiration());
            forgotpasswordrelieve = Util.null2String(rs.getForgotpasswordrelieve());
            canEditPhoto = Util.null2String(rs.getCanEditPhoto());
            canEditUserIcon = Util.null2String(rs.getCanEditUserIcon());

            secondNeedDynapass = Util.null2String(rs.getSecondNeedDynapass());//动态密码允许作为二次身份校验
            secondDynapassValidityMin = Util.null2String(rs.getSecondDynapassValidityMin());//动态密码免密时间(分钟)
            secondNeedusbDt = Util.null2String(rs.getSecondNeedusbDt());//动态令牌允许作为二次身份校验
            secondValidityDtMin = Util.null2String(rs.getSecondValidityDtMin());//动态令牌免密时间(分钟)
            secondPassword = "1";//密码允许作为二次身份校验
            secondPasswordMin = Util.null2String(rs.getSecondPasswordMin());//密码免密时间(分钟)
            addressCA = Util.null2String(rs.getAddressCA());//CA云服务地址
            CADefault = Util.null2String(rs.getCADefault());//CA默认启用方式
            secondCA = Util.null2String(rs.getSecondCA());//CA允许作为二次身份校验
            secondCAValidityMin = Util.null2String(rs.getSecondValidityCAMin());//CA免密时间(分钟)
            needCL = Util.null2String(rs.getNeedCL());//是否启用辅助校验
            needCLdefault = Util.null2String(rs.getNeedCLdefault());//默认方式
            secondCL = Util.null2String(rs.getSecondCL());//契约锁允许作为二次身份校验
            addressCL = Util.null2String(rs.getAddressCL());//契约锁云服务地址
            appKey = Util.null2String(rs.getAppKey());//契约锁提供
            appSecret = Util.null2String(rs.getAppSecret());//契约锁提供
            clAuthtype = Util.null2String(rs.getClAuthtype());//契约锁认证方式
            clAuthTypeDefault = Util.null2String(rs.getClAuthTypeDefault());//默认契约锁认证方式
            hideSuccessStatusPage = Util.null2String(rs.getHideSuccessStatusPage());//契约锁认证通过后隐藏成功页
            secondCLValidityMin = Util.null2String(rs.getSecondCLValidityMin());//契约锁免密时间(分钟)
            secondFace = Util.null2String(rs.getSecondFace());//人脸识别允许作为二次身份校验
            secondFaceValidityMin = Util.null2String(rs.getSecondFaceValidityMin());//人脸识别免密时间(分钟)
            qrCode = Util.null2String(rs.getQRCode());
            qrCodeDefault = Util.null2String(rs.getQRCodeDefault());
            allowUserSetting = Util.null2String(rs.getAllowUserSetting());
            initialPwdValidity = Util.null2String(rs.getInitialPwdValidity());//初始密码有效期
            unRegPwdLock = Util.null2String(rs.getUnRegPwdLock());//长时间未登录自动锁定
            unRegPwdLockDays = Util.null2String(rs.getUnRegPwdLockDays());//多少天未登录自动锁定
            weakPasswordDisable = Util.null2String(rs.getWeakPasswordDisable());//禁止保存弱密码
            defaultPasswordEnable = Util.null2String(rs.getDefaultPasswordEnable());//启用初始密码
            defaultPassword = Util.null2String(rs.getDefaultPassword());//初始密码
            isOpenClassification = Util.null2String(rs.getIsOpenClassification());
            isCrc = Util.null2String(rs.getIsCrc());
            newResourceMode = Util.null2String(rs.getNewResourceMode());
            secondDynapassDefault = Util.null2String(rs.getSecondDynapassDefault());
            secondUsbDtDefault = Util.null2String(rs.getSecondUsbDtDefault());
            secondPasswordDefault = Util.null2String(rs.getSecondPasswordDefault());
            secondCADefault = Util.null2String(rs.getSecondCADefault());
            secondCLDefault = Util.null2String(rs.getSecondCLDefault());
            secondFaceDefault = Util.null2String(rs.getSecondFaceDefault());
            if (dynapasslen .length()==0) {
                dynapasslen = "6";
            }
            if (dypadcon == null || dypadcon.equals("")) {
                dypadcon = "2";
            }
            if (minPasslen .length()==0) {
                minPasslen = "6";
            }
            if (relogin .length()==0) {
                relogin = "1";
            }
            if (forgotpasswordExpiration.length()==0) {
              forgotpasswordExpiration = "60";
            }
        }
    }

    public void saveHrmSettings() {
        try {
            RecordSet rs=new RecordSet();
            Map<String, String> kv = new HashMap<String, String>();
            kv.put("birthremindmode", Util.null2String(getBirthremindmode()));
            kv.put("birthdispatchimg", Util.null2String(getBirthDispatchImg()));
            kv.put("birthdispatchimgid", Util.null2String(getBirthDispatchImgId()));
            kv.put("birthDispatchShowField", Util.null2String(getBirthDispatchShowField()));
            kv.put("birthremindperiod", Util.null2String(getBirthremindperiod()));
            kv.put("birthremindperTime", Util.null2String(getBirthremindperTime()));
            kv.put("birthvalid", Util.null2String(getBirthvalid()));
            kv.put("congratulation", Util.null2String(getCongratulation()));
            kv.put("congratulation1", Util.null2String(getCongratulation1()));
            kv.put("birthdialogstyle", Util.null2String(getBirthdialogstyle()));
            kv.put("birthshowfield", Util.null2String(getBirthshowfield()));
            kv.put("epremindperTime", Util.null2String(getEpremindperTime()));
            if (getBrithalarmscope() == null || getBrithalarmscope().equals("")) {
                setBrithalarmscope("3");//默认所有人
            }
            kv.put("brithalarmscope", Util.null2String(getBrithalarmscope()));
            kv.put("birthvalidadmin", Util.null2String(getBirthvalidadmin()));
            kv.put("brithalarmadminscope", Util.null2String(getBrithalarmadminscope()));
            kv.put("contractvalid", Util.null2String(getContractvalid()));
            kv.put("contractremindperiod", Util.null2String(getContractremindperiod()));
            kv.put("contractremindperTime", Util.null2String(getContractremindperTime()));
            kv.put("entervalid", Util.null2String(getEntervalid()));
            kv.put("dynapasslen", getDynapasslen());
            kv.put("dypadcon", Util.null2String(getDypadcon()));
            kv.put("firmcode", Util.null2String(getFirmcode()));
            kv.put("minPasslen", Util.null2String(getMinPasslen()));
            kv.put("needdynapass", Util.null2String(getNeeddynapass()));
            kv.put("needdynapassdefault", Util.null2String(getNeeddynapassdefault()));
            kv.put("needusb", Util.null2String(getNeedusb()));
            kv.put("usbType", Util.null2String(getUsbType()));
            kv.put("needvalidate", Util.null2String(getNeedvalidate()));
            kv.put("logintype", Util.null2String(getLogintype()));
            kv.put("timeOutSwitch", Util.null2String(getTimeOutSwitch()));
            kv.put("relogin", Util.null2String(getRelogin()));
            kv.put("remindperiod", Util.null2String(getRemindperiod()));
            kv.put("remindpertime", Util.null2String(getRemindperTime()));
            kv.put("usercode", Util.null2String(getUsercode()));
            kv.put("valid", Util.null2String(getValid()));
            kv.put("validatenum", Util.null2String(getValidatenum()));
            kv.put("validatetype", Util.null2String(getValidatetype()));
            kv.put("daysToRemind", Util.null2String(getDaysToRemind()));
            kv.put("passwordChangeReminder", Util.null2String(getPasswordChangeReminder()));
            kv.put("changePasswordDays", Util.null2String(getChangePasswordDays()));
            kv.put("openPasswordLock", Util.null2String(getOpenPasswordLock()));
            kv.put("sumPasswordLock", Util.null2String(getSumPasswordLock()));
            kv.put("passwordComplexity", Util.null2String(getPasswordComplexity()));
            kv.put("passwordReuse", Util.null2String(getPasswordReuse()));
            kv.put("passwordReuseNum", Util.null2String(getPasswordReuseNum()));
            kv.put("needDactylogram", Util.null2String(getNeedDactylogram()));
            kv.put("canModifyDactylogram", Util.null2String(getCanModifyDactylogram()));
            kv.put("needusbnetwork", Util.null2String(getNeedusbnetwork()));
            kv.put("needusbdefault", Util.null2String(getNeedusbdefault()));
            kv.put("mobileShowSet", Util.null2String(getMobileShowSet()));
            kv.put("mobileShowType", Util.null2String(getMobileShowType()));
            kv.put("mobileShowTypeDefault", Util.null2String(getMobileShowTypeDefault()));
            kv.put("loginMustUpPswd", Util.null2String(getLoginMustUpPswd()));
            kv.put("forbidLogin", Util.null2String(getForbidLogin()));
            kv.put("needusbHt", Util.null2String(getNeedusbHt()));
            kv.put("needusbdefaultHt", Util.null2String(getNeedusbdefaultHt()));
            kv.put("needusbDt", Util.null2String(getNeedusbDt()));
            kv.put("needusbdefaultDt", Util.null2String(getNeedusbdefaultDt()));
            kv.put("validitySec", Util.null2String(getValiditySec()));
            kv.put("needpassword", Util.null2String(getNeedpassword()));
            kv.put("checkkey", Util.null2String(getCheckkey()));
            kv.put("numvalidatewrong", Util.null2String(getNumvalidatewrong()));
            kv.put("checkUnJob", Util.null2String(getCheckUnJob()));
            kv.put("statusWithContract", Util.null2String(getStatusWithContract()));
            kv.put("checkSysValidate", Util.null2String(getCheckSysValidate()));

            kv.put("defaultResult", Util.null2String(getDefaultResult()));
            kv.put("defaultTree", Util.null2String(getDefaultTree()));
            kv.put("birthshowfieldcolor", Util.null2String(getBirthshowfieldcolor()));
            kv.put("birthshowcontentcolor", Util.null2String(getBirthshowcontentcolor()));
            kv.put("birthshowfieldWF", Util.null2String(getBirthshowfieldWF()));
            kv.put("needPasswordLockMin", Util.null2String(getNeedPasswordLockMin()));
            kv.put("passwordLockMin", Util.null2String(getPasswordLockMin()));

            kv.put("needCA", Util.null2String(getNeedCA()));
            kv.put("needCAdefault", Util.null2String(getneedCAdefault()));
            kv.put("cetificatePath", Util.null2String(getCetificatePath()));
            kv.put("mobileScanCA", Util.null2String(getMobileScanCA()));
            //入职一周年提醒设置 start
            kv.put("entryDialogStyle", Util.null2String(getEntryDialogStyle()));
            kv.put("entryValid", Util.null2String(getEntryValid()));
            kv.put("entryCongrats", Util.null2String(getEntryCongrats()));
            kv.put("entryCongratsColor", Util.null2String(getEntryCongratsColor()));
            kv.put("entryFont", Util.null2String(getEntryFont()));
            kv.put("entryFontSize", Util.null2String(getEntryFontSize()));
            //入职一周年提醒设置 end
            //忘记密码开关设置
            kv.put("needforgotpassword", Util.null2String(getNeedforgotpassword()));
            kv.put("forgotpasswordmode", Util.null2String(getForgotpasswordmode()));
            kv.put("forgotpasswordExpiration", Util.null2String(getForgotpasswordExpiration()));
            kv.put("forgotpasswordrelieve", Util.null2String(getForgotpasswordrelieve()));

            kv.put("secondNeedDynapass", Util.null2String(getSecondNeedDynapass()));//动态密码允许作为二次身份校验
            kv.put("secondDynapassValidityMin", Util.null2String(getSecondDynapassValidityMin()).length()==0?"0":Util.null2String(getSecondDynapassValidityMin()));//动态密码免密时间(分钟)
            kv.put("secondNeedusbDt", Util.null2String(getSecondNeedusbDt()));//动态令牌允许作为二次身份校验
            kv.put("secondValidityDtMin", Util.null2String(getSecondValidityDtMin()).length()==0?"0":Util.null2String(getSecondValidityDtMin()));//动态令牌免密时间(分钟)
            kv.put("secondPassword", Util.null2String(getSecondPassword()));//密码允许作为二次身份校验
            kv.put("secondPasswordMin", Util.null2String(getSecondPasswordMin()).length()==0?"0":Util.null2String(getSecondPasswordMin()));//密码免密时间(分钟)
            kv.put("addressCA", Util.null2String(getAddressCA()));//CA云服务地址
            kv.put("CADefault", Util.null2String(getCADefault()).length()==0?"0":Util.null2String(getCADefault()));//CA默认启用方式
            kv.put("secondCA", Util.null2String(getSecondCA()).length()==0?"0":Util.null2String(getSecondCA()));//CA允许作为二次身份校验
            kv.put("secondCAValidityMin", Util.null2String(getSecondCAValidityMin()).length()==0?"0":Util.null2String(getSecondCAValidityMin()));//CA免密时间(分钟)
            kv.put("secondFace", Util.null2String(getSecondFace()).length()==0?"0":Util.null2String(getSecondFace()));//人脸识别允许作为二次身份校验
            kv.put("secondFaceValidityMin", Util.null2String(getSecondFaceValidityMin()).length()==0?"0":Util.null2String(getSecondFaceValidityMin()));//人脸识别免密时间(分钟)
            kv.put("addressCL", Util.null2String(getAddressCL()));//契约锁云服务地址
            kv.put("appKey", Util.null2String(getAppKey()));//契约锁提供
            kv.put("appSecret", Util.null2String(getAppSecret()));//契约锁提供
            kv.put("clAuthtype", Util.null2String(getClAuthtype()));//契约锁认证方式
            kv.put("clAuthTypeDefault", Util.null2String(getClAuthTypeDefault()));//默认契约锁认证方式
            kv.put("hideSuccessStatusPage", Util.null2String(getHideSuccessStatusPage()).length()==0?"0":Util.null2String(getHideSuccessStatusPage()));//默认契约锁认证方式
            kv.put("needCL", Util.null2String(getNeedCL()).length()==0?"0":Util.null2String(getNeedCL()));//是否启用契约锁作为辅助校验
            kv.put("needCLdefault", Util.null2String(getNeedCLdefault()).length()==0?"0":Util.null2String(getNeedCLdefault()));//默认启用方式
            kv.put("secondCL", Util.null2String(getSecondCL()).length()==0?"0":Util.null2String(getSecondCL()));//契约锁允许作为二次身份校验
            kv.put("secondCLValidityMin", Util.null2String(getSecondCLValidityMin()).length()==0?"0":Util.null2String(getSecondCLValidityMin()));//契约锁免密时间(分钟)
            kv.put("qrCode", Util.null2String(getQRCode()).length()==0?"0":Util.null2String(getQRCode()));
            kv.put("qrCodeDefault", Util.null2String(getQRCodeDefault()).length()==0?"0":Util.null2String(getQRCodeDefault()));
            kv.put("allowUserSetting", Util.null2String(getAllowUserSetting()).length()==0?"0":Util.null2String(getAllowUserSetting()));
            kv.put("initialPwdValidity", Util.null2String(getInitialPwdValidity()));//初始密码有效期
            kv.put("unRegPwdLock", Util.null2String(getUnRegPwdLock()));//长时间未登录自动锁定
            kv.put("unRegPwdLockDays", Util.null2String(getUnRegPwdLockDays()));//多少天未登录自动锁定
            kv.put("weakPasswordDisable",Util.null2String(getWeakPasswordDisable()));//禁止保存弱密码
            kv.put("defaultPasswordEnable",Util.null2String(getDefaultPasswordEnable()));//启用初始密码
            kv.put("defaultPassword",Util.null2String(getDefaultPassword()));//初始密码
            kv.put("isOpenClassification",Util.null2String(getIsOpenClassification()));//是否开启分级保护
            kv.put("isCrc",Util.null2String(getIsCrc()).length()==0?"0":Util.null2String(getIsCrc()));//是否开启防篡改
            kv.put("newResourceMode", Util.null2String(getNewResourceMode()));//新建人员默认模式
            kv.put("secondDynapassDefault",Util.null2String(getSecondDynapassDefault()).length()==0?"0":Util.null2String(getSecondDynapassDefault()));
            kv.put("secondUsbDtDefault",Util.null2String(getSecondUsbDtDefault()).length()==0?"0":Util.null2String(getSecondUsbDtDefault()));
            kv.put("secondPasswordDefault",Util.null2String(getSecondPasswordDefault()).length()==0?"0":Util.null2String(getSecondPasswordDefault()));
            kv.put("secondCADefault",Util.null2String(getSecondCADefault()).length()==0?"0":Util.null2String(getSecondCADefault()));
            kv.put("secondCLDefault",Util.null2String(getSecondCLDefault()).length()==0?"0":Util.null2String(getSecondCLDefault()));
            kv.put("secondFaceDefault",Util.null2String(getSecondFaceDefault()).length()==0?"0":Util.null2String(getSecondFaceDefault()));

            new BaseBean().writeLog("==zj==(kv)" + JSON.toJSONString(kv.get("appSecret")));
            ConnStatement statement = new ConnStatement();
            int updateCount = 0;
            ArrayList<String> lsParam = new ArrayList<String>();
            String sql = "update HrmSettings set ";
            if (kv != null && !kv.isEmpty()) {
                Set<String> key = kv.keySet();
                int idx = 0;
                for (Iterator<String> it = key.iterator(); it.hasNext(); ) {
                    idx++;
                    String s = it.next();
                    lsParam.add(kv.get(s));
                    if (idx == 1) {
                        sql = sql + s + "=?";
                    } else {
                        sql = sql + ", " + s + "=?";
                    }
                }
            }
            try {
                statement.setStatementSql(sql);
                String param="";
                if (lsParam != null && !lsParam.isEmpty()) {
                    for (int i = 0; i < lsParam.size(); i++) {
                        if(rs.getDBType().equalsIgnoreCase("postgresql"))
                            param=lsParam.get(i)==""?null:lsParam.get(i);
                        else
                            param=Util.null2String(lsParam.get(i));
                        statement.setString(i + 1, param);
                    }
                }
                updateCount = statement.executeUpdate();
            } catch (Exception e) {
                writeLog(e);
                throw e;
            } finally {
                try {
                    statement.close();
                } catch (Exception ex) {
                }
            }

            if (updateCount == 0) {//如果没有初始化，插入数据
                statement = new ConnStatement();
                lsParam = new ArrayList<String>();
                String sql1 = "insert into HrmSettings (";
                String sql2 = ") values(";
                String sql3 = "";
                if (kv != null && !kv.isEmpty()) {
                    Set<String> key = kv.keySet();
                    int idx = 0;
                    for (Iterator<String> it = key.iterator(); it.hasNext(); ) {
                        idx++;
                        String s = it.next();
                        lsParam.add(kv.get(s));
                        if (sql3.length() > 0) sql3 += ",";
                        sql3 += "'" + kv.get(s) + "'";
                        if (idx == 1) {
                            sql1 = sql1 + s;
                            sql2 = sql2 + "?";
                        } else {
                            sql1 = sql1 + "," + s;
                            sql2 = sql2 + ",?";
                        }
                    }
                }
                sql = sql1 + sql2 + ")";
                try {
                    statement.setStatementSql(sql);
                    String param="";
                    if (lsParam != null && !lsParam.isEmpty()) {
                        for (int i = 0; i < lsParam.size(); i++) {
                            if(rs.getDBType().equalsIgnoreCase("postgresql"))
                                param=lsParam.get(i)==""?null:lsParam.get(i);
                            else
                                param=Util.null2String(lsParam.get(i));
                            statement.setString(i + 1, param);
                        }
                    }
                    statement.executeUpdate();
                } catch (Exception e) {
                    writeLog(e);
                    throw e;
                } finally {
                    try {
                        statement.close();
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        new HrmSettingComInfo().removeCache();
    }

    public String getNewResourceMode() {
        return newResourceMode;
    }

    public void setNewResourceMode(String newResourceMode) {
        this.newResourceMode = newResourceMode;
    }

    public void setBirthremindmode(String birthremindmode) {
        this.birthremindmode = birthremindmode;
    }

    public void setBirthremindperiod(String birthremindperiod) {
        this.birthremindperiod = birthremindperiod;
    }

    public void setBirthvalid(String birthvalid) {
        this.birthvalid = birthvalid;
    }

    public void setCongratulation(String congratulation) {
        this.congratulation = congratulation;
    }

    public void setCongratulation1(String congratulation1) {
        this.congratulation1 = congratulation1;
    }

    public void setDynapasslen(String dynapasslen) {
        this.dynapasslen = dynapasslen;
    }

    public void setDypadcon(String dypadcon) {
        this.dypadcon = dypadcon;
    }

    public void setFirmcode(String firmcode) {
        this.firmcode = firmcode;
    }

    public void setMinPasslen(String minPasslen) {
        this.minPasslen = minPasslen;
    }

    public void setNeeddynapass(String needdynapass) {
        this.needdynapass = needdynapass;
    }

    public void setNeedusb(String needusb) {
        this.needusb = needusb;
    }

    public void setUsbType(String usbType) {
        this.usbType = usbType;
    }

    public void setNeedvalidate(String needvalidate) {
        this.needvalidate = needvalidate;
    }

    public void setLogintype(String logintype) {
        this.logintype = logintype;
    }

    public void setRelogin(String relogin) {
        this.relogin = relogin;
    }

    public void setRemindperiod(String remindperiod) {
        this.remindperiod = remindperiod;
    }

    public void setUsercode(String usercode) {
        this.usercode = usercode;
    }

    public void setValid(String valid) {
        this.valid = valid;
    }

    public void setValidatenum(String validatenum) {
        this.validatenum = validatenum;
    }

    public void setValidatetype(String validatetype) {
        this.validatetype = validatetype;
    }

    public void setNumvalidatewrong(String numvalidatewrong) {
        this.numvalidatewrong = numvalidatewrong;
    }

    public String getBirthremindmode() {
        return birthremindmode;
    }

    public String getBirthremindperiod() {
        return birthremindperiod;
    }

    public String getBirthvalid() {
        return birthvalid;
    }

    public String getCongratulation() {
        return congratulation;
    }

    public String getCongratulation1() {
        return congratulation1;
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

    public String getDynapasslen() {
        return dynapasslen;
    }

    public String getDypadcon() {
        return dypadcon;
    }

    public String getFirmcode() {
        return firmcode;
    }

    public String getMinPasslen() {
        return minPasslen;
    }

    public String getNeeddynapass() {
        return needdynapass;
    }

    public String getNeedusb() {
        return needusb;
    }

    public String getUsbType() {
        return usbType;
    }

    public String getNeedvalidate() {
        return needvalidate;
    }

    public String getLogintype() {
        return logintype;
    }

    public String getRelogin() {
        return relogin;
    }

    public String getRemindperiod() {
        return remindperiod;
    }

    public String getUsercode() {
        return usercode;
    }

    public String getValid() {
        return valid;
    }

    public String getValidatenum() {
        return validatenum;
    }

    public String getValidatetype() {
        return validatetype;
    }

    public String getNumvalidatewrong() {
        return numvalidatewrong;
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

    public String getDaysToRemind() {
        return daysToRemind;
    }

    public void setDaysToRemind(String daysToRemind) {
        this.daysToRemind = daysToRemind;
    }

    public String getPasswordChangeReminder() {
        return passwordChangeReminder;
    }

    public void setPasswordChangeReminder(String passwordChangeReminder) {
        this.passwordChangeReminder = passwordChangeReminder;
    }

    public String getChangePasswordDays() {
        return changePasswordDays;
    }

    public void setChangePasswordDays(String changePasswordDays) {
        this.changePasswordDays = changePasswordDays;
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

    public void setNeedusbnetwork(String needusbnetwork) {
        this.needusbnetwork = needusbnetwork;
    }

    public String getNeedusbnetwork() {
        return needusbnetwork;
    }

    public String getNeeddynapassdefault() {
        return needdynapassdefault;
    }

    public void setNeeddynapassdefault(String needdynapassdefault) {
        this.needdynapassdefault = needdynapassdefault;
    }

    public String getNeedusbdefault() {
        return needusbdefault;
    }

    public void setNeedusbdefault(String needusbdefault) {
        this.needusbdefault = needusbdefault;
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

    public String getCheckIsEdit() {
        return checkIsEdit;
    }

    public void setCheckIsEdit(String checkIsEdit) {
        this.checkIsEdit = checkIsEdit;
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
        return needPasswordLockMin;
    }

    public void setNeedPasswordLockMin(String needPasswordLockMin) {
        this.needPasswordLockMin = needPasswordLockMin;
    }

    public String getPasswordLockMin() {
        return passwordLockMin;
    }

    public void setPasswordLockMin(String passwordLockMin) {
        this.passwordLockMin = passwordLockMin;
    }

    public String getCetificatePath() {
        return cetificatePath;
    }

    public void setCetificatePath(String cetificatePath) {
        this.cetificatePath = cetificatePath;
    }

    public String getNeedCA() {
        return needCA;
    }

    public void setNeedCA(String needCA) {
        this.needCA = needCA;
    }

    public String getneedCAdefault() {
        return needCAdefault;
    }

    public void setneedCAdefault(String needCAdefault) {
        this.needCAdefault = needCAdefault;
    }


    public String getMobileScanCA() {
        return mobileScanCA;
    }

    public void setMobileScanCA(String mobileScanCA) {
        this.mobileScanCA = mobileScanCA;
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

    public String getCanEditUserIcon() { return canEditUserIcon; }

    public void setCanEditUserIcon(String canEditUserIcon) { this.canEditUserIcon = canEditUserIcon; }
    public String getNeedCAdefault() {
        return needCAdefault;
    }

    public void setNeedCAdefault(String needCAdefault) {
        this.needCAdefault = needCAdefault;
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

    public String getTimeOutSwitch() {
        return timeOutSwitch;
    }

    public void setTimeOutSwitch(String timeOutSwitch) {
        this.timeOutSwitch = timeOutSwitch;
    }
}
