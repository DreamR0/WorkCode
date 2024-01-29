package com.engine.common.enums;

public enum EncryptMould {
    HRM("HRM", 179),
    WORKFLOW("WORKFLOW", 2118),
    MEETING("MEETING", 34076),
    WORKPLAN("WORKPLAN", 2211),
    FORMMODE("FORMMODE", 30235),
    MOBILEMODE("MOBILEMODE", 33641),
    ESB("ESB", 132216),
    GOVERN("GOVERN", 382561),
    INTEGRATION("INTEGRATION", 32269),
    COWORK("COWORK", 17855),
    FNA("FNA", 189),  //财务(预算)模块
    VOTING("VOTING",15109),//调查
    DOCUMENT("DOCUMENT", 58),  //文档模块
    CRM("CRM", 136),
    BLOG("BLOG",26467),
    PRJ("prj", 30046),
    CPT("CPT", 535),
    EM("EM", 534451)//EM消息
    ;
    /**
     * code
     */
    protected String code;

    protected int labelId;

    public String getCode() {
        return code;
    }

    public int getLableId() {
        return labelId;
    }

    EncryptMould(String code, int labelId) {
        this.code = code;
        this.labelId = labelId;
    }

    public static int getLabelIdByMouldCode(String mouldCode) {
        if (mouldCode.equals(EncryptMould.HRM.getCode())) {
            return EncryptMould.HRM.getLableId();
        } else if (mouldCode.equals(EncryptMould.WORKFLOW.getCode())) {
            return EncryptMould.WORKFLOW.getLableId();
        } else if (mouldCode.equals(EncryptMould.MEETING.getCode())) {
            return EncryptMould.MEETING.getLableId();
        } else if (mouldCode.equals(EncryptMould.WORKPLAN.getCode())) {
            return EncryptMould.WORKPLAN.getLableId();
        } else if (mouldCode.equals(EncryptMould.FORMMODE.getCode())) {
            return EncryptMould.FORMMODE.getLableId();
        } else if (mouldCode.equals(EncryptMould.MOBILEMODE.getCode())) {
            return EncryptMould.MOBILEMODE.getLableId();
        } else if (mouldCode.equals(EncryptMould.ESB.getCode())) {
            return EncryptMould.ESB.getLableId();
        }else if(mouldCode.equals(EncryptMould.GOVERN.getCode())){
            return EncryptMould.GOVERN.getLableId();
        } else if (mouldCode.equals(EncryptMould.INTEGRATION.getCode())) {
            return EncryptMould.INTEGRATION.getLableId();
        } else if (mouldCode.equals(EncryptMould.COWORK.getCode())) {
            return EncryptMould.COWORK.getLableId();
        }else if(mouldCode.equals(EncryptMould.FNA.getCode())) {
            return EncryptMould.FNA.getLableId();
        }else if(mouldCode.equals(EncryptMould.VOTING.getCode())) {
            return EncryptMould.VOTING.getLableId();
        }else if(mouldCode.equals(EncryptMould.DOCUMENT.getCode())) {
            return EncryptMould.DOCUMENT.getLableId();
        } else if(mouldCode.equals(EncryptMould.CRM.getCode())) {
            return EncryptMould.CRM.getLableId();
        } else if(mouldCode.equals(EncryptMould.BLOG.getCode())){
            return EncryptMould.BLOG.getLableId();
        }else if(mouldCode.equals(EncryptMould.PRJ.getCode())){
            return EncryptMould.PRJ.getLableId();
        }else if(mouldCode.equals(EncryptMould.CPT.getCode())){
            return EncryptMould.CPT.getLableId();
        }else if(mouldCode.equals(EncryptMould.EM.getCode())){
            return EncryptMould.EM.getLableId();
        }
        return 0;
    }
}
