package edu.cmu.cs.lti.discoursedb.macro;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;

import edu.cmu.cs.lti.discoursedb.annotation.AnnotationInstance;
import edu.cmu.cs.lti.discoursedb.annotation.Annotations;

@Entity
@SelectBeforeUpdate 
@DynamicUpdate
@DynamicInsert
@Table(name="contribution")
public class Contribution implements Serializable {

	private static final long serialVersionUID = -2489956863731652149L;

	private long id;
	
	private Content firstRevision;
	
	private Content currentRevision;
	
	private Timestamp startTime;
	
	private Timestamp endTime;

	private ContributionType type;
	
	private Annotations annotations;

	public Contribution(){}

	@Id
	@Column(name="id_contribution", nullable=false)
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Content getFirstRevision() {
		return firstRevision;
	}

	public void setFirstRevision(Content firstRevision) {
		this.firstRevision = firstRevision;
	}

	public Content getCurrentRevision() {
		return currentRevision;
	}

	public void setCurrentRevision(Content currentRevision) {
		this.currentRevision = currentRevision;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public ContributionType getType() {
		return type;
	}

	public void setType(ContributionType type) {
		this.type = type;
	}

	public Annotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}


}
