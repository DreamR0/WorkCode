/*
 *
 * Copyright (c) 2001-2016 泛微软件.
 * 泛微协同商务系统,版权所有.
 *
 */
package weaver.hrm.settings;

import com.alibaba.fastjson.JSON;
import com.cloudstore.dev.api.bean.MessageBean;
import com.cloudstore.dev.api.bean.MessageType;
import com.cloudstore.dev.api.util.Util_Message;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.general.*;
import weaver.system.SysRemindWorkflow;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.msg.PoppupRemindInfoUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * @author ludifu 密码变更提醒类
 * @version 1.0
 */
public class ChgPasswdReminder {
    /**
     * 提醒设置对象
     */
    private RemindSettings settings = new RemindSettings();

    /**
     * 获取人力资源其他信息
     *
     * @return RemindSettings
     */
    public RemindSettings getRemindSettings() {
        HrmSettingsComInfo sci = new HrmSettingsComInfo();
        settings.setRemindperiod(sci.getRemindperiod());
        settings.setValid(sci.getValid());
        settings.setRemindperTime(sci.getRemindperTime());

        settings.setBirthremindperiod(sci.getBirthremindperiod());
        settings.setBirthremindperTime(sci.getBirthremindperTime());
        settings.setEpremindperTime(sci.getEpremindperTime());
        settings.setBirthvalid(sci.getBirthvalid());
        settings.setBirthDispatchImg(sci.getBirthDispatchImg());
        settings.setBirthDispatchImgId(sci.getBirthDispatchImgId());
        settings.setBirthDispatchShowField(sci.getBirthDispatchShowField());
        settings.setCongratulation(sci.getCongratulation());
        settings.setCongratulation1(sci.getCongratulation1());
        settings.setBirthremindmode(sci.getBirthremindmode());
        settings.setBirthdialogstyle(sci.getBirthdialogstyle());
        settings.setBirthshowfield(sci.getBirthshowfield());
        settings.setBrithalarmscope(sci.getBrithalarmscope());
        settings.setBirthvalidadmin(sci.getBirthvalidadmin());
        settings.setBrithalarmadminscope(sci.getBrithalarmadminscope());
        settings.setContractvalid(sci.getContractvalid());
        settings.setContractremindperiod(sci.getContractremindperiod());
        settings.setContractremindperTime(sci.getContractremindperTime());
        settings.setEntervalid(sci.getEntervalid());

        settings.setNeedusb(sci.getNeedusb());
        settings.setNeedusbdefault(sci.getNeedusbdefault());
        settings.setFirmcode(sci.getFirmcode());
        settings.setUsercode(sci.getUsercode());
        settings.setUsbType(sci.getUsbType());
        settings.setLogintype(sci.getLogintype());
        settings.setRelogin(sci.getRelogin());
        settings.setDypadcon(sci.getDypadcon());

        settings.setDaysToRemind(sci.getDaysToRemind());
        settings.setPasswordChangeReminder(sci.getPasswordChangeReminder());
        settings.setChangePasswordDays(sci.getChangePasswordDays());
        settings.setOpenPasswordLock(sci.getOpenPasswordLock());
        settings.setSumPasswordLock(sci.getSumPasswordLock());
        settings.setPasswordComplexity(sci.getPasswordComplexity());
        settings.setPasswordReuse(sci.getPasswordReuse());
        settings.setPasswordReuseNum(sci.getPasswordReuseNum());
        //add by sean.yang 2006-02-09 for TD3609
        settings.setNeedvalidate(Util.getIntValue((sci.getNeedvalidate())));
        settings.setValidatetype(Util.getIntValue((sci.getValidatetype())));
        settings.setValidatenum(Util.getIntValue((sci.getValidatenum())));
        settings.setNumvalidatewrong(Util.getIntValue((sci.getNumvalidatewrong()), 0));
        //td5709,xiaofeng
        settings.setMinPasslen(Util.getIntValue((sci.getMinPasslen())));
        settings.setNeeddynapass(Util.getIntValue((sci.getNeeddynapass())));
        settings.setNeeddynapassdefault(Util.getIntValue((sci.getNeeddynapassdefault())));
        settings.setDynapasslen(Util.getIntValue((sci.getDynapasslen())));

        settings.setNeedDactylogram(sci.getNeedDactylogram());
        settings.setCanModifyDactylogram(sci.getCanModifyDactylogram());

        settings.setNeedusbnetwork(sci.getNeedusbnetwork());
        settings.setMobileShowSet(sci.getMobileShowSet());
        settings.setMobileShowType(sci.getMobileShowType());
        settings.setMobileShowTypeDefault(sci.getMobileShowTypeDefault());
        //added by wcd 2014-12-22 start
        settings.setForbidLogin(sci.getForbidLogin());
        settings.setLoginMustUpPswd(sci.getLoginMustUpPswd());
        settings.setNeedusbHt(sci.getNeedusbHt());
        settings.setNeedusbdefaultHt(sci.getNeedusbdefaultHt());
        settings.setNeedusbDt(sci.getNeedusbDt());
        settings.setNeedusbdefaultDt(sci.getNeedusbdefaultDt());
        settings.setValiditySec(sci.getValiditySec());
        settings.setNeedpassword(sci.getNeedpassword());
        settings.setCheckkey(sci.getCheckkey());
        settings.setCheckUnJob(sci.getCheckUnJob());
        settings.setCheckIsEdit(sci.getCheckIsEdit());
        settings.setStatusWithContract(sci.getStatusWithContract());
        settings.setCheckSysValidate(sci.getCheckSysValidate());

        settings.setDefaultResult(sci.getDefaultResult());
        settings.setDefaultTree(sci.getDefaultTree());
        settings.setBirthshowfieldcolor(sci.getBirthshowfieldcolor());
        settings.setBirthshowcontentcolor(sci.getBirthshowcontentcolor());
        settings.setBirthshowfieldWF(sci.getBirthshowfieldWF());
        settings.setNeedPasswordLockMin(sci.getNeedPasswordLockMin());
        settings.setPasswordLockMin(sci.getPasswordLockMin());

        settings.setNeedca(sci.getNeedCA());
        settings.setNeedcadefault(sci.getneedCAdefault());

        // mobile ca
        settings.setMobileScanCA(sci.getMobileScanCA());
        //入职一周年提醒设置 start
        settings.setEntryDialogStyle(sci.getEntryDialogStyle());
        settings.setEntryValid(sci.getEntryValid());
        settings.setEntryCongrats(sci.getEntryCongrats());
        settings.setEntryCongratsColor(sci.getEntryCongratsColor());
        settings.setEntryFont(sci.getEntryFont());
        settings.setEntryFontSize(sci.getEntryFontSize());
        //入职一周年提醒设置 end
        //忘记密码设置
        settings.setNeedforgotpassword(sci.getNeedforgotpassword());
        settings.setForgotpasswordmode(sci.getForgotpasswordmode());
        settings.setCanEditPhoto(sci.getCanEditPhoto());
        settings.setCanEditUserIcon(sci.getCanEditUserIcon());

        settings.setSecondNeedDynapass(sci.getSecondNeedDynapass());//动态密码允许作为二次身份校验
        settings.setSecondDynapassValidityMin(sci.getSecondDynapassValidityMin());//动态密码免密时间(分钟)
        settings.setSecondNeedusbDt(sci.getSecondNeedusbDt());//动态令牌允许作为二次身份校验
        settings.setSecondValidityDtMin(sci.getSecondValidityDtMin());//动态令牌免密时间(分钟)
        settings.setSecondPassword(sci.getSecondPassword());//密码允许作为二次身份校验
        settings.setSecondPasswordMin(sci.getSecondPasswordMin());//密码免密时间(分钟)
        settings.setAddressCA(sci.getAddressCA());//CA云服务地址
        settings.setCADefault(sci.getCADefault());//CA默认启用方式
        settings.setSecondCA(sci.getSecondCA());//CA允许作为二次身份校验
        settings.setSecondCAValidityMin(sci.getSecondCAValidityMin());//CA免密时间(分钟)
        settings.setAddressCL(sci.getAddressCL());//契约锁云服务地址
        settings.setAppKey(sci.getAppKey());//契约锁企业AppKey
        settings.setAppSecret(sci.getAppSecret());//契约锁AppKey秘钥
        settings.setClAuthtype(sci.getClAuthtype());//契约锁认证方式
        settings.setClAuthTypeDefault(sci.getClAuthTypeDefault());//默认契约锁认证方式
        settings.setHideSuccessStatusPage(sci.getHideSuccessStatusPage());//契约锁认证通过后隐藏成功页
        settings.setNeedCL(sci.getNeedCL());//契约锁允许作为二次身份校验
        settings.setNeedCLdefault(sci.getNeedCLdefault());
        settings.setSecondCL(sci.getSecondCL());//契约锁允许作为二次身份校验
        settings.setSecondCLValidityMin(sci.getSecondCLValidityMin());//契约锁免密时间(分钟)
        settings.setSecondFace(sci.getSecondFace());//人脸识别允许作为二次身份校验
        settings.setSecondFaceValidityMin(sci.getSecondFaceValidityMin());//人脸识别免密时间(分钟)
        settings.setQRCode(sci.getQRCode());
        settings.setQRCodeDefault(sci.getQRCodeDefault());
        settings.setAllowUserSetting(sci.getAllowUserSetting());
        settings.setInitialPwdValidity(sci.getInitialPwdValidity());//初始密码有效期
        settings.setUnRegPwdLock(sci.getUnRegPwdLock());//是否开启长时间未登录自动锁定
        settings.setUnRegPwdLockDays(sci.getUnRegPwdLockDays());//多长时间未登录自动锁定
        settings.setForgotpasswordExpiration(sci.getForgotpasswordExpiration());
        settings.setForgotpasswordrelieve(sci.getForgotpasswordrelieve());
        settings.setDefaultPasswordEnable(sci.getDefaultPasswordEnable());
        settings.setDefaultPassword(sci.getDefaultPassword());
        settings.setWeakPasswordDisable(sci.getWeakPasswordDisable());
        settings.setIsOpenClassification(sci.getIsOpenClassification());
        settings.setIsCrc(sci.getIsCrc());
        //added by wcd 2014-12-22 end
        settings.setNewResourceMode(sci.getNewResourceMode());

        return settings;
    }

    /**
     * 获取人力资源其他信息
     *
     * @param settings
     */
    public void setRemindSettings(RemindSettings settings) {
        HrmSettingsComInfo sci = new HrmSettingsComInfo();
        sci.setRemindperiod(settings.getRemindperiod());
        sci.setValid(settings.getValid());
        sci.setRemindperTime(settings.getRemindperTime());

        sci.setBirthremindperiod(settings.getBirthremindperiod());
        sci.setBirthvalid(settings.getBirthvalid());
        sci.setBirthremindperTime(settings.getBirthremindperTime());
        sci.setEpremindperTime(settings.getEpremindperTime());
        sci.setBirthDispatchImg(settings.getBirthDispatchImg());
        sci.setBirthDispatchImgId(settings.getBirthDispatchImgId());
        sci.setBirthDispatchShowField(settings.getBirthDispatchShowField());
        sci.setCongratulation(settings.getCongratulation());
        sci.setCongratulation1(settings.getCongratulation1());
        sci.setBirthremindmode(settings.getBirthremindmode());
        sci.setBirthdialogstyle(settings.getBirthdialogstyle());
        sci.setBirthshowfield(settings.getBirthshowfield());
        sci.setBrithalarmscope(settings.getBrithalarmscope());
        sci.setBirthvalidadmin(settings.getBirthvalidadmin());
        sci.setBrithalarmadminscope(settings.getBrithalarmadminscope());
        sci.setContractvalid(settings.getContractvalid());
        sci.setContractremindperiod(settings.getContractremindperiod());
        sci.setContractremindperTime(settings.getContractremindperTime());
        sci.setEntervalid(settings.getEntervalid());
        sci.setNeedusb(settings.getNeedusb());
        sci.setNeedusbdefault(settings.getNeedusbdefault());
        sci.setUsbType(settings.getUsbType());
        sci.setFirmcode(settings.getFirmcode());
        sci.setUsercode(settings.getUsercode());
        sci.setLogintype(settings.getLogintype());
        sci.setRelogin(settings.getRelogin());

        sci.setDaysToRemind(settings.getDaysToRemind());
        sci.setPasswordChangeReminder(settings.getPasswordChangeReminder());
        sci.setChangePasswordDays(settings.getChangePasswordDays());
        sci.setOpenPasswordLock(settings.getOpenPasswordLock());
        sci.setSumPasswordLock(settings.getSumPasswordLock());
        sci.setPasswordComplexity(settings.getPasswordComplexity());
        sci.setPasswordReuse(settings.getPasswordReuse());
        sci.setPasswordReuseNum(settings.getPasswordReuseNum());

        //add by sean.yang 2006-02-09 for TD3609
        sci.setNeedvalidate("" + settings.getNeedvalidate());
        sci.setValidatetype("" + settings.getValidatetype());
        sci.setValidatenum("" + settings.getValidatenum());
        sci.setNumvalidatewrong("" + settings.getNumvalidatewrong());
        //td5709,xiaofeng
        sci.setMinPasslen("" + settings.getMinPasslen());
        sci.setNeeddynapass("" + settings.getNeeddynapass());
        sci.setNeeddynapassdefault("" + settings.getNeeddynapassdefault());
        sci.setDynapasslen("" + settings.getDynapasslen());
        sci.setDypadcon("" + settings.getDypadcon());

        sci.setNeedDactylogram(settings.getNeedDactylogram());
        sci.setCanModifyDactylogram(settings.getCanModifyDactylogram());
        sci.setNeedusbnetwork(settings.getNeedusbnetwork());
        sci.setMobileShowSet(settings.getMobileShowSet());
        sci.setMobileShowType(settings.getMobileShowType());
        sci.setMobileShowTypeDefault(settings.getMobileShowTypeDefault());
        //added by wcd 2014-12-22 start
        sci.setForbidLogin(settings.getForbidLogin());
        sci.setLoginMustUpPswd(settings.getLoginMustUpPswd());
        sci.setNeedusbHt(settings.getNeedusbHt());
        sci.setNeedusbdefaultHt(settings.getNeedusbdefaultHt());
        sci.setNeedusbDt(settings.getNeedusbDt());
        sci.setNeedusbdefaultDt(settings.getNeedusbdefaultDt());
        sci.setValiditySec(settings.getValiditySec());
        sci.setNeedpassword(settings.getNeedpassword());
        sci.setCheckkey(settings.getCheckkey());
        sci.setCheckUnJob(settings.getCheckUnJob());
        sci.setStatusWithContract(settings.getStatusWithContract());
        sci.setCheckSysValidate(settings.getCheckSysValidate());
        //added by wcd 2014-12-22 end
        sci.setDefaultResult(settings.getDefaultResult());
        sci.setDefaultTree(settings.getDefaultTree());
        sci.setBirthshowfieldcolor(settings.getBirthshowfieldcolor());
        sci.setBirthshowcontentcolor(settings.getBirthshowcontentcolor());
        sci.setBirthshowfieldWF(settings.getBirthshowfieldWF());
        sci.setNeedPasswordLockMin(settings.getNeedPasswordLockMin());
        sci.setPasswordLockMin(settings.getPasswordLockMin());

        sci.setNeedCA(settings.getNeedca());
        sci.setneedCAdefault(settings.getNeedcadefault());
        sci.setCetificatePath(settings.getCetificatePath());


        sci.setMobileScanCA(settings.getMobileScanCA());
        //入职一周年提醒设置 start
        sci.setEntryDialogStyle(settings.getEntryDialogStyle());
        sci.setEntryValid(settings.getEntryValid());
        sci.setEntryCongrats(settings.getEntryCongrats());
        sci.setEntryCongratsColor(settings.getEntryCongratsColor());
        sci.setEntryFont(settings.getEntryFont());
        sci.setEntryFontSize(settings.getEntryFontSize());
        //入职一周年提醒设置 end

        //忘记密码开关设置
        sci.setNeedforgotpassword(settings.getNeedforgotpassword());
        sci.setForgotpasswordmode(settings.getForgotpasswordmode());
        sci.setForgotpasswordExpiration(settings.getForgotpasswordExpiration());
		sci.setForgotpasswordrelieve(settings.getForgotpasswordrelieve());
        sci.setSecondNeedDynapass(settings.getSecondNeedDynapass());//动态密码允许作为二次身份校验
        sci.setSecondDynapassValidityMin(settings.getSecondDynapassValidityMin());//动态密码免密时间(分钟)
        sci.setSecondNeedusbDt(settings.getSecondNeedusbDt());//动态令牌允许作为二次身份校验
        sci.setSecondValidityDtMin(settings.getSecondValidityDtMin());//动态令牌免密时间(分钟)
        sci.setSecondPassword(settings.getSecondPassword());//密码允许作为二次身份校验
        sci.setSecondPasswordMin(settings.getSecondPasswordMin());//密码免密时间(分钟)
        sci.setAddressCA(settings.getAddressCA());//CA云服务地址
        sci.setCADefault(settings.getCADefault());//CA默认启用方式
        sci.setSecondCA(settings.getSecondCA());//CA允许作为二次身份校验
        sci.setSecondCAValidityMin(settings.getSecondCAValidityMin());//CA免密时间(分钟)
        sci.setAddressCL(settings.getAddressCL());//契约锁云服务地址
        new BaseBean().writeLog("==zj==(settings.getAppSecret())" + JSON.toJSONString(settings.getAppSecret()));
        sci.setAppKey(settings.getAppKey());//契约锁企业AppKey
        sci.setAppSecret(settings.getAppSecret());//契约锁AppKey秘钥
        sci.setClAuthtype(settings.getClAuthtype());//契约锁认证方式
        sci.setClAuthTypeDefault(settings.getClAuthTypeDefault());//默认契约锁认证方式
        sci.setHideSuccessStatusPage(settings.getHideSuccessStatusPage());//契约锁认证通过后隐藏成功页
        sci.setNeedCL(settings.getNeedCL());//契约锁辅助身份认证
        sci.setNeedCLdefault(settings.getNeedCLdefault());
        sci.setSecondCL(settings.getSecondCL());//契约锁允许作为二次身份校验
        sci.setSecondCLValidityMin(settings.getSecondCLValidityMin());//契约锁免密时间(分钟)
        sci.setSecondFace(settings.getSecondFace());//人脸识别允许作为二次身份校验
        sci.setSecondFaceValidityMin(settings.getSecondFaceValidityMin());//人脸识别免密时间(分钟)
        sci.setQRCode(settings.getQRCode());
        sci.setQRCodeDefault(settings.getQRCodeDefault());
        sci.setAllowUserSetting(settings.getAllowUserSetting());
        sci.setInitialPwdValidity(settings.getInitialPwdValidity());//初始密码有效期
        sci.setUnRegPwdLock(settings.getUnRegPwdLock());//是否开启长时间未登录自动锁定
        sci.setUnRegPwdLockDays(settings.getUnRegPwdLockDays());//多长时间未登录自动锁定
        sci.setWeakPasswordDisable(settings.getWeakPasswordDisable());//禁止保存弱密码
        sci.setDefaultPasswordEnable(settings.getDefaultPasswordEnable());//启用初始密码
        sci.setDefaultPassword(settings.getDefaultPassword());//初始密码
        sci.setIsOpenClassification(settings.getIsOpenClassification());
        sci.setIsCrc(settings.getIsCrc());
        sci.setNewResourceMode(settings.getNewResourceMode());
        sci.setSecondDynapassDefault(settings.getSecondDynapassDefault());
        sci.setSecondUsbDtDefault(settings.getSecondUsbDtDefault());
        sci.setSecondPasswordDefault(settings.getSecondPasswordDefault());
        sci.setSecondCADefault(settings.getSecondCADefault());
        sci.setSecondCLDefault(settings.getSecondCLDefault());
        sci.setSecondFaceDefault(settings.getSecondFaceDefault());

        sci.saveHrmSettings();

    }


    /**
     * 密码变更提醒
     *
     * @param period 提前天数
     * @throws Exception
     */
    public void remind(int period) throws Exception {
        boolean isSended = isSended(3);
        if (isSended) {
            new BaseBean().writeLog("今天已经推送过密码变更提醒，不再重复推送");
            return;
        }

        //密码变更提醒时需要区分是否集成了LDAP
        String mode = Prop.getPropValue(GCONST.getConfigFile(), "authentic");
        String col = "loginid";
        if (mode != null && mode.equals("ldap")) {
            //loginid、account字段整合 qc:128484
            col = "loginid";
            //col = "account";
        }

        //获取今天的日期
        String today = TimeUtil.getCurrentDateString();
        //查找密码到期的在职员工(AD账号不支持密码到期提醒的功能)
        String sql = "select id,passwdchgdate from HrmResource where ( accounttype!=1 or accounttype is null) and (status = 0 or status = 1 or status = 2 or status = 3) and " + col + " is not null  and ( isadaccount!=1 or isadaccount is null)";  //ad帐号不密码过期提醒
        Set<String> userIdSet = new HashSet<String>();
        RecordSet recordset = new RecordSet();
        recordset.executeQuery(sql);
        while (recordset.next()) {
            String id = recordset.getString("id");
            String date = recordset.getString("passwdchgdate");
            //日期格式错误
            if (date == null || date.length() < 10 || date.indexOf("-") <= 0) {
                date = "";
            }
            //判断密码修改日期是否在有效的日期内
            if (date.equals("") || TimeUtil.dateInterval(TimeUtil.dateAdd(date, period), today) >= 0) {
                userIdSet.add(id);
            }
        }
        new BaseBean().writeLog("ChgPasswdReminder》》发送开始");
        String content = SystemEnv.getHtmlLabelName(524913, ThreadVarLanguage.getLang());
        String linkUrl = "/spa/hrm/index_mobx.html#/main/hrm/password"; //PC端链接
        String linkMobileUrl = "/spa/hrm/static4mobile/index.html#/passwordSetting"; //移动端链接
        try {
            MessageBean messageBean = Util_Message.createMessage(MessageType.HR_PASSWORD_CHANGE, userIdSet, content, content, linkUrl, linkMobileUrl);
            messageBean.setCreater(1);//创建人id
            Util_Message.store(messageBean);
        } catch (IOException e) {
            new BaseBean().writeLog(e);
            e.printStackTrace();
        }
        new BaseBean().writeLog("ChgPasswdReminder》》发送结束");
        //记录发动历史
        recordPasswordRemindHistory();
    }

    /**
     * 记录发放记录
     */
    private void recordPasswordRemindHistory() {
        String today = TimeUtil.getCurrentDateString();
        String sql = "insert into HrmRemindHistory(type, reminddate, remindedresourceid) values(" + 3 + ",'" + today + "',null)";
        RecordSet recordset = new RecordSet();
        recordset.executeUpdate(sql);
    }

    /**
     * 判断是否发送过
     *
     * @param type
     * @return
     */
    private boolean isSended(int type) {
        String today = TimeUtil.getCurrentDateString();
        String sql = "select * from HrmRemindHistory where type=" + type + " and reminddate='" + today + "'";
        RecordSet recordset = new RecordSet();
        recordset.executeSql(sql);

        if (recordset.next())
            return true;
        else
            return false;

    }

    /**
     * 发送密码变更提醒
     *
     * @param title     提醒标题
     * @param submiter  提交人
     * @param accepters 接收人
     * @param remark    提醒内容
     * @return
     * @throws Exception
     */
    public int sendMsg(String title, int submiter, String accepters, String remark) throws Exception {

        SysRemindWorkflow sysremindwf = new SysRemindWorkflow();
        return sysremindwf.make(title, 0, 0, 0, 0, submiter, accepters, remark);
    }


}
