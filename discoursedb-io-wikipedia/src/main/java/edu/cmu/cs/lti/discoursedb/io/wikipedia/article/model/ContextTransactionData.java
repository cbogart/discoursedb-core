package edu.cmu.cs.lti.discoursedb.io.wikipedia.article.model;

import java.util.Date;

/**
 * Holds meta information about a context that is currently being created.
 * This includes the Dates of the first and the last content item and the DiscourseDB id of the created context.
 * 
 * @author Oliver Ferschke
 *
 */
public class ContextTransactionData{
	private Date firstContent;
	private Date lastContent;
	private Long contextId;
	
	public ContextTransactionData() {
		super();
		this.firstContent = null;
		this.lastContent = null;
		this.contextId = null;
	}
	public ContextTransactionData(Date firstContent, Date lastContent, Long contextId) {
		super();
		this.firstContent = firstContent;
		this.lastContent = lastContent;
		this.contextId = contextId;
	}
	public Date getFirstContent() {
		return firstContent;
	}
	public void setFirstContent(Date firstContent) {
		this.firstContent = firstContent;
	}
	public Date getLastContent() {
		return lastContent;
	}
	public void setLastContent(Date lastContent) {
		this.lastContent = lastContent;
	}
	public Long getContextId() {
		return contextId;
	}
	public void setContextId(Long contextId) {
		this.contextId = contextId;
	}
	public boolean isAvailable() {
		return firstContent!=null&&lastContent!=null;
	}
}