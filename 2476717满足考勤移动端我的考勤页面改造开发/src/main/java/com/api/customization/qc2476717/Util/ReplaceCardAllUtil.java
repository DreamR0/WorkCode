package com.api.customization.qc2476717.Util;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2476717.Bean.CardBean;
import com.weaver.formmodel.util.DateHelper;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.interfaces.schedule.BaseCronJob;

import java.util.ArrayList;
import java.util.List;

public class ReplaceCardAllUtil extends BaseCronJob {
    String tableName= new BaseBean().getPropValue("qc2476717","tableName");

    @Override
    public void execute() {
        RecordSet rs = new RecordSet();
        //先将建模表中的数据清空
        String sql = "delete from "+tableName;
        rs.executeUpdate(sql);
        //将所有补卡数据导入
            User user = new User(1);
            List<CardBean> cardBeanList = new ArrayList<>();

            String sqlBase = "";
            String sqlWhere = "";

            sqlBase = "select * from hrmschedulesign where 1=1 ";
            sqlWhere = " and signfrom  like 'card%' and isincom <> 0";
           rs.executeQuery(sqlBase+sqlWhere);
            while (rs.next()){
                String userId = Util.null2String(rs.getString("userid"));
                String signDate= Util.null2String(rs.getString("signDate"));
                //这里获取下当天班次名称
                ReplaceCardUtil replaceCardUtil = new ReplaceCardUtil();
                String serial=  replaceCardUtil.getSerial(userId,signDate);
                String signTime= Util.null2String(rs.getString("signTime"));
                if (signTime.length() > 6){
                    signTime = signTime.substring(0,5);
                }
                String signType= Util.null2String(rs.getString("signType"));

                CardBean cardBean = new CardBean();
                cardBean.setUserId(userId);
                cardBean.setSignDate(signDate);
                cardBean.setSerial(serial);
                cardBean.setSignTime(signTime);
                cardBean.setSignType(signType);

                cardBeanList.add(cardBean);
            }

            //将获取到的数据插入到建模表中
            for (int i = 0; i < cardBeanList.size(); i++) {
                CardBean cardBean = cardBeanList.get(i);
                String userId = cardBean.getUserId();
                String signDate = cardBean.getSignDate();
                String signMonth = signDate.substring(0,7);//补卡年月
                String serial = cardBean.getSerial();
                String signTime = cardBean.getSignTime();
                String signType = cardBean.getSignType();
                if ("1".equals(signType)){
                    signType = "上班卡";
                }else {
                    signType = "下班卡";
                }
                String kqryid = "";
                String qdrq2 = signDate;

                int modeDataId = getModeDataId(tableName, user);
                if (modeDataId > 0){
                    sqlBase = "update " + tableName + " set userid = '" + userId + "',signdate = '" + signDate + "',serial = '" + serial + "',signtime = '" + signTime + "',signtype = '" + signType + "',kqryid = '"+kqryid+"',qdrq2 = '"+qdrq2 + "',bkyf = '"+signMonth+"' where id = '" + modeDataId + "'";
                    rs.executeUpdate(sqlBase);
                }
            }
    }

    /**
     * 构建建模表数据
     * @param tableName
     * @param user
     * @return
     */
    public int getModeDataId(String tableName, User user){
        int modeId = getModeId(tableName);
        if (modeId==-1){
            return -1;
        }
        int id = ModeDataIdUpdate.getInstance().getModeDataNewIdByUUID(tableName, modeId, user.getUID(), (user.getLogintype()).equals("1") ? 0 : 1,
                DateHelper.getCurrentDate(), DateHelper.getCurrentTime());
        ModeRightInfo ModeRightInfo = new ModeRightInfo();
        ModeRightInfo.setNewRight(true);
        ModeRightInfo.editModeDataShare(user.getUID(),Integer.parseInt(modeId+""),id);//新建的时候添加共享
        ModeRightInfo.addDocShare(user.getUID(),Integer.parseInt(modeId+""),id);//新建的时候添加文档共享
        return id;
    }

    /**
     * 获取建模id
     * @param tablename
     * @return
     */
    public int getModeId(String tablename){
        RecordSet rs = new RecordSet();
        int modeid = -1;
        String sql = "select id as modeid,formid from modeinfo where FORMID = (select id from workflow_bill where TABLENAME = ?)";
        rs.executeQuery(sql,tablename);
        if (rs.next()){
            modeid = rs.getInt("modeid");
        }
        return modeid;
    }
}
