package com.customization.qc2241192;

import com.engine.kq.wfset.bean.SplitBean;
import weaver.conn.RecordSet;
import weaver.general.Base64;
import weaver.general.BaseBean;
import weaver.general.Util;

public class SetCompanion {

    public void set(String prefix, RecordSet rs1, SplitBean splitBean){
        String companion = Util.null2s(rs1.getString(prefix+"companion"), "");
        new BaseBean().writeLog("==zj==(获取companion)"+companion);
        splitBean.setCompanion(companion);
    }
}
