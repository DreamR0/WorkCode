package com.customization.qc2505403;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQGroupMemberComInfo;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;

public class IpCheckUtil {

    /**
     * 当考勤打卡为pc端时，判断是指定范围ip
     *
     * @param user
     * @param request
     * @return
     */
    public Boolean ipCheck(User user, HttpServletRequest request) {
        Boolean isIpHave = false;
        RecordSet rs = new RecordSet();
        String sql = "";
        String clientAddress = Util.getIpAddr(request);//获取当前打卡ip地址

        try {
            //获取当前人员，当天的考勤组
            KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
            String kqGroupId = kqGroupMemberComInfo.getKQGroupId(user.getUID() + "", DateUtil.getCurrentDate());
            //获取考勤组的ip范围设置
            sql = "select * from kq_group where id = '" + kqGroupId + "'";
            rs.executeQuery(sql);
            if (rs.next()) {
                String officeIpCheck = rs.getString("officeIpCheck");
                if ("".equals(officeIpCheck) || officeIpCheck == null){
                    //如果没有设置ip范围，就走标准
                    return true;
                }
                String[] ipSplit = officeIpCheck.split(";");
                for (int i = 0; i < ipSplit.length; i++) {
                    if (clientAddress.equals(ipSplit[i])) {
                        //当打卡ip属于设置的ip范围内，就返回true
                        isIpHave = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(检测ip报错)" + JSON.toJSONString(e));
        }
        return isIpHave;
    }
}
