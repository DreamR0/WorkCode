package com.engine.odoc.entity.createDoc;

import weaver.orm.annotation.Id;
import weaver.orm.annotation.Table;

/**
 *
 * @ClassName: WorkflowCreateDoc
 * @Description: 流程创建文档 实体类
 * @author: suijp
 * @date: 2018年6月30日 上午10:32:56
 *
 *        Copyright (c) 2001-2018 泛微软件. 泛微协同商务系统,版权所有.
 */
@Table("workflow_createdoc")
public class WorkflowCreateDoc {
	@Id
	 private Integer id;
	 private Integer workflowid;
	 private String status;
	 private Integer flowcodefield;
	 private Integer flowdocfield;
	 private Integer flowdoccatfield;
	 private String defaultview;
	 private Integer usetempletnode;
	 private Integer documenttitlefield;
	 private Integer printnodeselect;
	 private String printnodes;
	 private String newtextnodes;
	 private String iscompellentmark;
	 private String iscancelcheck;
	 private String signaturenodes;
	 private String isworkflowdraft;
	 private String defaultdoctype;
	 private Integer extfile2doc;
	 private String ishidethetraces;
	 private Integer wfstatus;
	 private String savescriptnode;
	 private String papersstoragedirectory;
	 private Integer opentextdefaultselect;
	 private String opentextdefaultnode;
	 private String savetempfile;
	 private Integer cleancopynodeselect;
	 private String cleancopynodes;
	 private Integer odoctype;
	 private String issuednum;
	 private String receivenum;
	 private Integer topictype;
	 private Integer urgencydegree;
	 private Integer secretlevel;
	 private String sendunit;
	 private String receiveunit;
	 private Integer issueduserid;
	 private String writtendate;
	 private String issueddate;
	 private String mustsignaturenodes;
	 private String useeditmouldnodes;
	 private String istextinform;
	 private Integer istextinformnodeselect;
	 private String istextinformnode;
	 private String istextinformcanedit;
	 private String autocleancopy;
	 private String issavetracebyclean;
     private String synctitlenodes;
     private String documenttextposition;
     private String documenttextproportion;
     private String iscolumnshow;
	 private String flowattachfiled;
	 private String reuploadtext;
	 private String reselectmould;
	 //==zj
	 private Integer changeTextNodeSelect;
	 private String  changeTextNodes;


	private String uploadpdf;
	private String uploadofd;
	private String uploadword;

	private Integer useyozoorwps;
	private String displaytab;
	private Integer savebackform;
	private Integer savedocremind;
	private Integer wfattachkeep;

	private String useiwebofficenodes;

	private String isopendocumentcompare;
    private String olddocumenttype;
	private String olddocumentvalue;
	private String comparedocumenttype;
	private String comparedocumentvalue;
	private String prefinishdocnodes;

	private String isshowattachmentinform;//移动端正文附件是否显示在表单
	private String autoprettifynodes;
	private String autoprettifynodesselect;
	private String manualprettifynodes;
	private String manualprettifynodesselect;
	private String ofdreadernodeselect;
	private String ofdreaderdefaultnode;
	private String replacezw4th;//套红节点是否允许替换正文

	public String getReplacezw4th() {
		return replacezw4th;
	}

	public void setReplacezw4th(String replacezw4th) {
		this.replacezw4th = replacezw4th;
	}

	public String getIsshowattachmentinform() {
		return isshowattachmentinform;
	}

	public void setIsshowattachmentinform(String isshowattachmentinform) {
		this.isshowattachmentinform = isshowattachmentinform;
	}

	public String getUseiwebofficenodes()
	{
		return useiwebofficenodes;
	}

	public void setUseiwebofficenodes(String useiwebofficenodes)
	{
		this.useiwebofficenodes = useiwebofficenodes;
	}
	public Integer getUseyozoorwps()
	{
		return useyozoorwps;
	}

	public void setUseyozoorwps(Integer useyozoorwps)
	{
		this.useyozoorwps = useyozoorwps;
	}

	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getWorkflowid() {
		return workflowid;
	}
	public void setWorkflowid(Integer workflowid) {
		this.workflowid = workflowid;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Integer getFlowcodefield() {
		return flowcodefield;
	}
	public void setFlowcodefield(Integer flowcodefield) {
		this.flowcodefield = flowcodefield;
	}
	public Integer getFlowdocfield() {
		return flowdocfield;
	}
	public void setFlowdocfield(Integer flowdocfield) {
		this.flowdocfield = flowdocfield;
	}
	public Integer getFlowdoccatfield() {
		return flowdoccatfield;
	}
	public void setFlowdoccatfield(Integer flowdoccatfield) {
		this.flowdoccatfield = flowdoccatfield;
	}
	public String getDefaultview() {
		return defaultview;
	}
	public void setDefaultview(String defaultview) {
		this.defaultview = defaultview;
	}
	public Integer getUsetempletnode() {
		return usetempletnode;
	}
	public void setUsetempletnode(Integer usetempletnode) {
		this.usetempletnode = usetempletnode;
	}
	public Integer getDocumenttitlefield() {
		return documenttitlefield;
	}
	public void setDocumenttitlefield(Integer documenttitlefield) {
		this.documenttitlefield = documenttitlefield;
	}
	public String getPrintnodes() {
		return printnodes;
	}
	public void setPrintnodes(String printnodes) {
		this.printnodes = printnodes;
	}
	public String getNewtextnodes() {
		return newtextnodes;
	}
	public void setNewtextnodes(String newtextnodes) {
		this.newtextnodes = newtextnodes;
	}
	public String getIscompellentmark() {
		return iscompellentmark;
	}
	public void setIscompellentmark(String iscompellentmark) {
		this.iscompellentmark = iscompellentmark;
	}
	public String getIscancelcheck() {
		return iscancelcheck;
	}
	public void setIscancelcheck(String iscancelcheck) {
		this.iscancelcheck = iscancelcheck;
	}
	public String getSignaturenodes() {
		return signaturenodes;
	}
	public void setSignaturenodes(String signaturenodes) {
		this.signaturenodes = signaturenodes;
	}
	public String getIsworkflowdraft() {
		return isworkflowdraft;
	}
	public void setIsworkflowdraft(String isworkflowdraft) {
		this.isworkflowdraft = isworkflowdraft;
	}
	public String getDefaultdoctype() {
		return defaultdoctype;
	}
	public void setDefaultdoctype(String defaultdoctype) {
		this.defaultdoctype = defaultdoctype;
	}
	public Integer getExtfile2doc() {
		return extfile2doc;
	}
	public void setExtfile2doc(Integer extfile2doc) {
		this.extfile2doc = extfile2doc;
	}
	public String getIshidethetraces() {
		return ishidethetraces;
	}
	public void setIshidethetraces(String ishidethetraces) {
		this.ishidethetraces = ishidethetraces;
	}
	public Integer getWfstatus() {
		return wfstatus;
	}
	public void setWfstatus(Integer wfstatus) {
		this.wfstatus = wfstatus;
	}
	public String getSavescriptnode() {
		return savescriptnode;
	}
	public void setSavescriptnode(String savescriptnode) {
		this.savescriptnode = savescriptnode;
	}
	public String getPapersstoragedirectory() {
		return papersstoragedirectory;
	}
	public void setPapersstoragedirectory(String papersstoragedirectory) {
		this.papersstoragedirectory = papersstoragedirectory;
	}
	public String getOpentextdefaultnode() {
		return opentextdefaultnode;
	}
	public void setOpentextdefaultnode(String opentextdefaultnode) {
		this.opentextdefaultnode = opentextdefaultnode;
	}
	public String getSavetempfile() {
		return savetempfile;
	}
	public void setSavetempfile(String savetempfile) {
		this.savetempfile = savetempfile;
	}
	public String getCleancopynodes() {
		return cleancopynodes;
	}
	public void setCleancopynodes(String cleancopynodes) {
		this.cleancopynodes = cleancopynodes;
	}
	public Integer getOdoctype() {
		return odoctype;
	}
	public void setOdoctype(Integer odoctype) {
		this.odoctype = odoctype;
	}
	public String getIssuednum() {
		return issuednum;
	}

	public String getIssavetracebyclean() {
		return issavetracebyclean;
	}

	public void setIssavetracebyclean(String issavetracebyclean) {
		this.issavetracebyclean = issavetracebyclean;
	}

	public void setIssuednum(String issuednum) {
		this.issuednum = issuednum;
	}
	public Integer getTopictype() {
		return topictype;
	}
	public void setTopictype(Integer topictype) {
		this.topictype = topictype;
	}
	public Integer getUrgencydegree() {
		return urgencydegree;
	}
	public void setUrgencydegree(Integer urgencydegree) {
		this.urgencydegree = urgencydegree;
	}
	public Integer getSecretlevel() {
		return secretlevel;
	}
	public void setSecretlevel(Integer secretlevel) {
		this.secretlevel = secretlevel;
	}
	public String getSendunit() {
		return sendunit;
	}
	public void setSendunit(String sendunit) {
		this.sendunit = sendunit;
	}
	public String getReceiveunit() {
		return receiveunit;
	}
	public void setReceiveunit(String receiveunit) {
		this.receiveunit = receiveunit;
	}
	public Integer getIssueduserid() {
		return issueduserid;
	}
	public void setIssueduserid(Integer issueduserid) {
		this.issueduserid = issueduserid;
	}
	public String getWrittendate() {
		return writtendate;
	}
	public void setWrittendate(String writtendate) {
		this.writtendate = writtendate;
	}
	public String getIssueddate() {
		return issueddate;
	}
	public void setIssueddate(String issueddate) {
		this.issueddate = issueddate;
	}
	public String getMustsignaturenodes() {
		return mustsignaturenodes;
	}
	public void setMustsignaturenodes(String mustsignaturenodes) {
		this.mustsignaturenodes = mustsignaturenodes;
	}
	public String getUseeditmouldnodes() {
		return useeditmouldnodes;
	}
	public void setUseeditmouldnodes(String useeditmouldnodes) {
		this.useeditmouldnodes = useeditmouldnodes;
	}
	public String getIstextinform() {
		return istextinform;
	}
	public String getIsTextInFormcanedit() {
		return istextinformcanedit;
	}
	public void setIstextinform(String istextinform) {
		this.istextinform = istextinform;
	}
	public void setIstextinformcanedit(String istextinformcanedit) {
		this.istextinformcanedit = istextinformcanedit;
	}

	public String getUploadpdf() {
		return uploadpdf;
	}

	public String getUploadofd() {
		return uploadofd;
	}

	public void setUploadofd(String uploadofd) {
		this.uploadofd = uploadofd;
	}

	public void setUploadpdf(String uploadpdf) {
		this.uploadpdf = uploadpdf;
	}

	public String getAutocleancopy() {
		return autocleancopy;
	}

	public void setAutocleancopy(String autocleancopy) {
		this.autocleancopy = autocleancopy;
	}
    public String getDocumenttextposition() {
        return documenttextposition;
    }

    public void setDocumenttextposition(String documenttextposition) {
        this.documenttextposition = documenttextposition;
    }

    public String getDocumenttextproportion() {
        return documenttextproportion;
    }

    public void setDocumenttextproportion(String documenttextproportion) {
        this.documenttextproportion = documenttextproportion;
    }

    public String getIscolumnshow() {
        return iscolumnshow;
    }

    public void setIscolumnshow(String iscolumnshow) {
        this.iscolumnshow = iscolumnshow;
    }

	public String getFlowattachfiled() {
		return flowattachfiled;
	}

	public void setFlowattachfiled(String flowattachfiled) {
		this.flowattachfiled = flowattachfiled;
	}

	public String getReuploadtext() {
		return reuploadtext;
	}

	public void setReuploadtext(String reuploadtext) {
		this.reuploadtext = reuploadtext;
	}

	public String getReselectmould() {
		return reselectmould;
	}

	public void setReselectmould(String reselecmould) {
		this.reselectmould = reselecmould;
	}
	public String getDisplaytab() {
		return displaytab;
	}

	public void setDisplaytab(String displaytab) {
		this.displaytab = displaytab;
	}
	public String getSynctitlenodes() {
		return synctitlenodes;
	}

	public void setSynctitlenodes(String synctitlenodes) {
		this.synctitlenodes = synctitlenodes;
	}

	public Integer getSavebackform(){
		return savebackform;
	}

	public void setSavebackform(Integer savebackform){
		this.savebackform = savebackform;
	}

	public Integer getSavedocremind(){
		return savedocremind;
	}

	public void setSavedocremind(Integer savedocremind){
		this.savedocremind = savedocremind;
	}

	public Integer getWfattachkeep() {
		return wfattachkeep;
	}

	public void setWfattachkeep(Integer wfattachkeep) {
		this.wfattachkeep = wfattachkeep;
	}

	public Integer getPrintnodeselect() {
		return printnodeselect;
	}

	public void setPrintnodeselect(Integer printnodeselect) {
		this.printnodeselect = printnodeselect;
	}

	public Integer getOpentextdefaultselect() {
		return opentextdefaultselect;
	}

	public void setOpentextdefaultselect(Integer opentextdefaultselect) {
		this.opentextdefaultselect = opentextdefaultselect;
	}

	public Integer getCleancopynodeselect() {
		return cleancopynodeselect;
	}

	public void setCleancopynodeselect(Integer cleancopynodeselect) {
		this.cleancopynodeselect = cleancopynodeselect;
	}

	public Integer getIstextinformnodeselect() {
		return istextinformnodeselect;
	}

	public void setIstextinformnodeselect(Integer istextinformnodeselect) {
		this.istextinformnodeselect = istextinformnodeselect;
	}

	public String getIstextinformnode() {
		return istextinformnode;
	}

	public void setIstextinformnode(String istextinformnode) {
		this.istextinformnode = istextinformnode;
	}

	public String getReceivenum(){ return receivenum;}

	public void setReceivenum(String receivenum){ this.receivenum = receivenum; }

	public String getIsopendocumentcompare() {
        return isopendocumentcompare;
    }

    public void setIsopendocumentcompare(String isopendocumentcompare) {
        this.isopendocumentcompare = isopendocumentcompare;
    }

    public String getOlddocumenttype() {
        return olddocumenttype;
    }

    public void setOlddocumenttype(String olddocumenttype) {
        this.olddocumenttype = olddocumenttype;
    }

    public String getOlddocumentvalue() {
        return olddocumentvalue;
    }

    public void setOlddocumentvalue(String olddocumentvalue) {
        this.olddocumentvalue = olddocumentvalue;
    }

    public String getComparedocumenttype() {
        return comparedocumenttype;
    }

    public void setComparedocumenttype(String comparedocumenttype) {
        this.comparedocumenttype = comparedocumenttype;
    }

    public String getComparedocumentvalue() {
        return comparedocumentvalue;
    }

    public void setComparedocumentvalue(String comparedocumentvalue) {
        this.comparedocumentvalue = comparedocumentvalue;
    }

	public String getPrefinishdocnodes() {
		return prefinishdocnodes;
	}

	public void setPrefinishdocnodes(String prefinishdocnodes) {
		this.prefinishdocnodes = prefinishdocnodes;
	}

	public String getUploadword() {
		return uploadword;
	}

	public void setUploadword(String uploadword) {
		this.uploadword = uploadword;
	}
	public String getAutoprettifynodes() {
		return autoprettifynodes;
	}

	public void setAutoprettifynodes(String autoprettifynodes) {
		this.autoprettifynodes = autoprettifynodes;
	}

	public String getAutoprettifynodesselect() {
		return autoprettifynodesselect;
	}

	public void setAutoprettifynodesselect(String autoprettifynodesselect) {
		this.autoprettifynodesselect = autoprettifynodesselect;
	}

	public String getManualprettifynodes() {
		return manualprettifynodes;
	}

	public void setManualprettifynodes(String manualprettifynodes) {
		this.manualprettifynodes = manualprettifynodes;
	}

	public String getManualprettifynodesselect() {
		return manualprettifynodesselect;
	}

	public void setManualprettifynodesselect(String manualprettifynodesselect) {
		this.manualprettifynodesselect = manualprettifynodesselect;
	}

	public String getOfdreadernodeselect() {
		return ofdreadernodeselect;
	}

	public void setOfdreadernodeselect(String ofdreadernodeselect) {
		this.ofdreadernodeselect = ofdreadernodeselect;
	}

	public String getOfdreaderdefaultnode() {
		return ofdreaderdefaultnode;
	}

	public void setOfdreaderdefaultnode(String ofdreaderdefaultnode) {
		this.ofdreaderdefaultnode = ofdreaderdefaultnode;
	}

	public Integer getChangeTextNodeSelect() {
		return changeTextNodeSelect;
	}

	public void setChangeTextNodeSelect(Integer changeTextNodeSelect) {
		this.changeTextNodeSelect = changeTextNodeSelect;
	}

	public String getChangeTextNodes() {
		return changeTextNodes;
	}

	public void setChangeTextNodes(String changeTextNodes) {
		this.changeTextNodes = changeTextNodes;
	}
}
