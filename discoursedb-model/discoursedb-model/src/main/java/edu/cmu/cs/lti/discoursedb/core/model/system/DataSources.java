package edu.cmu.cs.lti.discoursedb.core.model.system;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;

import edu.cmu.cs.lti.discoursedb.core.model.CoreBaseEntity;

/**
 *
 */
@Entity
@SelectBeforeUpdate 
@DynamicUpdate
@DynamicInsert
@Table(name="data_sources")
public class DataSources extends CoreBaseEntity implements Serializable{

	private static final long serialVersionUID = -6582983183583393074L;

	private long id;
	
    private Set<DataSourceInstance> sources = new HashSet<DataSourceInstance>();
	
	public DataSources(){}

	@Id
	@Column(name="id_data_sources", nullable=false)
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(fetch=FetchType.LAZY,cascade=CascadeType.ALL,mappedBy="sourceAggregate")
	public Set<DataSourceInstance> getSources() {
		return sources;
	}

	public void setSources(Set<DataSourceInstance> sources) {
		this.sources = sources;
	}
	
}
