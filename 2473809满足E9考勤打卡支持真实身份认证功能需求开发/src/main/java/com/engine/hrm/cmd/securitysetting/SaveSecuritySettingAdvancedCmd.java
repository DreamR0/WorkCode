package com.engine.hrm.cmd.securitysetting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateAuditType;
import com.engine.common.constant.BizLogSmallType4DataEncrypt;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQGroupComInfo;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.systeminfo.SystemEnv;

import weaver.hrm.loginstrategy.LoginStrategyComInfo;
import weaver.hrm.loginstrategy.style.LoginStrategy;

/**
 * 保存安全设置高级设置
 * @author lvyi
 *
 */
public class SaveSecuritySettingAdvancedCmd extends AbstractCommonCommand<Map<String, Object>>{
	private SimpleBizLogger logger;

	public SaveSecuritySettingAdvancedCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;

		this.logger = new SimpleBizLogger();
		BizLogContext bizLogContext = new BizLogContext();
		String fromEncrypt = Util.null2String(params.get("fromEncrypt"));
		if(fromEncrypt.equals("1")) {
			bizLogContext.setLogType(BizLogType.DATA_ENCRYPT);//模块类型
			bizLogContext.setBelongType(BizLogSmallType4DataEncrypt.SECOND_AUTH_BASE_SETTING);//所属大类型
			bizLogContext.setLogSmallType(BizLogSmallType4DataEncrypt.SECOND_AUTH_BASE_SETTING);//当前小类型
			bizLogContext.setTargetName(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(527509,weaver.general.ThreadVarLanguage.getLang())+"-"+weaver.systeminfo.SystemEnv.getHtmlLabelName(16261,weaver.general.ThreadVarLanguage.getLang())+"");
		}else{
			bizLogContext.setLogType(BizLogType.HRM_ENGINE);//模块类型
			bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_SECURITYSETTING);//所属大类型
			bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(19332, user.getLanguage()));
			bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_SECURITYSETTING);//当前小类型
			bizLogContext.setTargetName(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(32496,weaver.general.ThreadVarLanguage.getLang())+"-"+weaver.systeminfo.SystemEnv.getHtmlLabelName(16261,weaver.general.ThreadVarLanguage.getLang())+"");
		}
		bizLogContext.setOperateAuditType(BizLogOperateAuditType.WARNING);
		bizLogContext.setParams(params);//当前request请求参数
		logger.setUser(user);//当前操作人
		String mainSql = "select distinct * from HrmSettings";
		logger.setMainSql(mainSql);//主表sql
		logger.before(bizLogContext);

		SimpleBizLogger.SubLogInfo subLogInfo = logger.getNewSubLogInfo();

	}

	@Override
	public Map<String,Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		ChgPasswdReminder reminder=new ChgPasswdReminder();
		RemindSettings settings=reminder.getRemindSettings();
		try{
			if(!HrmUserVarify.checkUserRight("OtherSettings:Edit",user)&&!EncryptConfigBiz.hasRight(user)) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
				return retmap;
			}

			int needdynapass=Util.getIntValue((String)params.get("needdynapass"),0);
			int needdynapassdefault=Util.getIntValue((String)params.get("needdynapassdefault"),0);
			int dynapasslen=Util.getIntValue((String)params.get("dynapasslen"),0);
			String dypadcon=Util.null2String(params.get("dypadcon"));//动态密码内容
			String validitySec = Util.null2String(params.get("validitySec"));
			String needpassword = Util.null2String(params.get("needpassword"));
			String mobileScanCA = Util.null2String(params.get("mobileScanCA"));

			String needusbDt = Util.null2String(params.get("needusbDt"));
			String needusbdefaultDt = Util.null2String(params.get("needusbdefaultDt"));
			String secondNeedDynapass = Util.null2String(params.get("secondNeedDynapass"));//动态密码允许作为二次身份校验
			String secondDynapassValidityMin = Util.null2String(params.get("secondDynapassValidityMin"));//动态密码免密时间(分钟)
			String secondNeedusbDt = Util.null2String(params.get("secondNeedusbDt"));//动态令牌允许作为二次身份校验
			String secondValidityDtMin = Util.null2String(params.get("secondValidityDtMin"));//动态令牌免密时间(分钟)
			//String secondPassword = Util.null2String(params.get("secondPassword"));//密码允许作为二次身份校验
			String secondPassword = "1";//密码允许作为二次身份校验
			String secondPasswordMin = Util.null2String(params.get("secondPasswordMin"));//密码免密时间(分钟)
			String addressCA = Util.null2String(params.get("addressCA"));//CA云服务地址
			String CADefault = Util.null2String(params.get("CADefault"));//CA默认启用方式
			String secondCA = Util.null2String(params.get("secondCA"));//CA允许作为二次身份校验
			String secondCAValidityMin = Util.null2String(params.get("secondCAValidityMin"));//CA免密时间(分钟)
			String addressCL = Util.null2String(params.get("addressCL"));//契约锁云服务地址
			String appKey = Util.null2String(params.get("appKey"));//契约锁企业AppKey
			String appSecret = Util.null2String(params.get("appSecret"));//契约锁AppKey秘钥
			String clAuthtype = Util.null2String(params.get("clAuthtype"));//契约锁认证方式
			String clAuthTypeDefault = Util.null2String(params.get("clAuthTypeDefault"));//默认契约锁认证方式
			String hideSuccessStatusPage = Util.null2String(params.get("hideSuccessStatusPage"));//契约锁认证通过后隐藏成功页
			String needCL = Util.null2String(params.get("needCL"));//契约锁辅助身份认证
			String needCLdefault = Util.null2String(params.get("needCLdefault"));//契约锁提供
			String secondCL = Util.null2String(params.get("secondCL"));//契约锁允许作为二次身份校验
			String secondCLValidityMin = Util.null2String(params.get("secondCLValidityMin"));//契约锁免密时间(分钟)
			String secondFace = Util.null2String(params.get("secondFace"));//人脸识别
			String secondFaceValidityMin = Util.null2String(params.get("secondFaceValidityMin"));//人脸识别免密时间(分钟)
			String secondDynapassDefault = Util.null2String(params.get("secondDynapassDefault"));
			String secondUsbDtDefault = Util.null2String(params.get("secondUsbDtDefault"));
			String secondPasswordDefault = Util.null2String(params.get("secondPasswordDefault"));
			String secondCADefault = Util.null2String(params.get("secondCADefault"));
			String secondCLDefault = Util.null2String(params.get("secondCLDefault"));
			String secondFaceDefault = Util.null2String(params.get("secondFaceDefault"));
			String allowUserSetting = Util.null2String(params.get("allowUserSetting"));

			String fromEncrypt = Util.null2String(params.get("fromEncrypt"));
			if(fromEncrypt.equals("1")){
				settings.setDynapasslen(dynapasslen);
				settings.setDypadcon(dypadcon);
				settings.setValiditySec(validitySec);
				settings.setSecondNeedDynapass(secondNeedDynapass);//动态密码允许作为二次身份校验
				settings.setSecondDynapassValidityMin(secondDynapassValidityMin);//动态密码免密时间(分钟)
				settings.setSecondNeedusbDt(secondNeedusbDt);//动态令牌允许作为二次身份校验
				settings.setSecondValidityDtMin(secondValidityDtMin);//动态令牌免密时间(分钟)
				settings.setSecondPassword(secondPassword);//密码允许作为二次身份校验
				settings.setSecondPasswordMin(secondPasswordMin);//密码免密时间(分钟)
				settings.setAddressCA(addressCA);//CA云服务地址
				settings.setCADefault(CADefault);//CA默认启用方式
				settings.setSecondCA(secondCA);//CA允许作为二次身份校验
				settings.setSecondCAValidityMin(secondCAValidityMin);//CA免密时间(分钟)
				settings.setAddressCL(addressCL);//契约锁云服务地址
				settings.setAppKey(appKey);
				settings.setAppSecret(appSecret);
				settings.setClAuthtype(clAuthtype);
				settings.setClAuthTypeDefault(clAuthTypeDefault);
				settings.setHideSuccessStatusPage(hideSuccessStatusPage);
//				settings.setNeedCL(needCL);
//				settings.setNeedCLdefault(needCLdefault);
				settings.setSecondCL(secondCL);//契约锁允许作为二次身份校验
				settings.setSecondCLValidityMin(secondCLValidityMin);//契约锁免密时间(分钟)
				settings.setSecondFace(secondFace);//人脸识别允许作为二次身份校验
				settings.setSecondFaceValidityMin(secondFaceValidityMin);//人脸识别免密时间(分钟)
				if(secondNeedusbDt.equals("1")){
					settings.setNeedusbDt("1");
				}
				if(secondCA.equals("1")){
					settings.setMobileScanCA("1");
				}
				settings.setAllowUserSetting(allowUserSetting);//允许个人设置
			}else {
				// 网段策略
				LoginStrategy segmentStrategy=LoginStrategy.SEGMENT_STRATEGY ;
				String segmentValue =Util.null2String(params.get(segmentStrategy.toString()));
				String segmentDefaultValue = Util.null2String(params.get(segmentStrategy.getDefaultFlagName())) ;

				LoginStrategyComInfo loginStrategyComInfo = new LoginStrategyComInfo() ;
				loginStrategyComInfo.saveLoginStrategy(segmentStrategy,segmentValue,segmentDefaultValue);

				//PC端强制扫码登录
				String qrCode = Util.null2String(params.get("qrCode"));//允许作为登录辅助校验
				String qrCodeDefault = Util.null2String(params.get("qrCodeDefault"));//默认启用方式

				settings.setNeeddynapass(needdynapass);
				settings.setNeeddynapassdefault(needdynapassdefault);
				settings.setDynapasslen(dynapasslen);
				settings.setDypadcon(dypadcon);
				settings.setValiditySec(validitySec);
				settings.setNeedpassword(needpassword);

				settings.setNeedusbDt(needusbDt);
				settings.setNeedusbdefaultDt(needusbdefaultDt);
				settings.setMobileScanCA(mobileScanCA);
				settings.setSecondNeedDynapass(secondNeedDynapass);//动态密码允许作为二次身份校验
				settings.setSecondDynapassValidityMin(secondDynapassValidityMin);//动态密码免密时间(分钟)
				settings.setSecondNeedusbDt(secondNeedusbDt);//动态令牌允许作为二次身份校验
				settings.setSecondValidityDtMin(secondValidityDtMin);//动态令牌免密时间(分钟)
				settings.setSecondPassword(secondPassword);//密码允许作为二次身份校验
				settings.setSecondPasswordMin(secondPasswordMin);//密码免密时间(分钟)
				settings.setAddressCA(addressCA);//CA云服务地址
				settings.setCADefault(CADefault);//CA默认启用方式
				settings.setSecondCA(secondCA);//CA允许作为二次身份校验
				settings.setSecondCAValidityMin(secondCAValidityMin);//CA免密时间(分钟)
				settings.setAddressCL(addressCL);//契约锁云服务地址
				settings.setAppKey(appKey);
				settings.setAppSecret(appSecret);
				settings.setClAuthtype(clAuthtype);
				settings.setClAuthTypeDefault(clAuthTypeDefault);
				settings.setHideSuccessStatusPage(hideSuccessStatusPage);
				settings.setNeedCL(needCL);
				settings.setNeedCLdefault(needCLdefault);
				settings.setSecondCL(secondCL);//契约锁允许作为二次身份校验
				settings.setSecondCLValidityMin(secondCLValidityMin);//契约锁免密时间(分钟)
				settings.setSecondFace(secondFace);//人脸识别允许作为二次身份校验
				settings.setSecondFaceValidityMin(secondFaceValidityMin);//人脸识别免密时间(分钟)
				settings.setQRCode(qrCode);
				settings.setQRCodeDefault(qrCodeDefault);
			}
			settings.setSecondDynapassDefault(secondDynapassDefault);
			settings.setSecondUsbDtDefault(secondUsbDtDefault);
			settings.setSecondPasswordDefault(secondPasswordDefault);
			settings.setSecondCADefault(secondCADefault);
			settings.setSecondCLDefault(secondCLDefault);
			settings.setSecondFaceDefault(secondFaceDefault);

			if(!secondDynapassDefault.equals("1")&&!secondUsbDtDefault.equals("1")&&!secondPasswordDefault.equals("1")&&
					!secondCADefault.equals("1")&&!secondCLDefault.equals("1")&&!secondFaceDefault.equals("1")){//不能取消所有默认二次认证
				secondPasswordDefault = "1";
				settings.setSecondPasswordDefault(secondPasswordDefault);
			}

			//开启契约锁认证后，如果开启考勤打卡身份认证，关闭契约锁认证时，如果之前开启契约锁认证的考勤组，讲人脸识别关闭
/*
			new RecordSet().executeUpdate("update kq_group set locationfacecheck=null, wififacecheck=null where (locationfacecheck = 1 and locationfacechecktype=2) or (wififacecheck = 1 and wififacechecktype=2)");
*/
			new KQGroupComInfo().removeCache();
			reminder.setRemindSettings(settings);
			retmap.put("status", "1");
		} catch (Exception e) {
			writeLog(e);
			retmap.put("status", "-1");
			retmap.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005268,weaver.general.ThreadVarLanguage.getLang())+"");
		}
		return retmap;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

	@Override
	public List<BizLogContext> getLogContexts() {
		return logger.getBizLogContexts();
	}

}