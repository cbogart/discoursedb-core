/*******************************************************************************
 * Copyright (C)  2015 - 2016  Carnegie Mellon University
 * Author: Oliver Ferschke
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
package edu.cmu.cs.lti.discoursedb.core.model.macro;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.Description;
import org.springframework.hateoas.Identifiable;

import edu.cmu.cs.lti.discoursedb.core.model.TypedTimedBE;
import edu.cmu.cs.lti.discoursedb.core.model.user.Audience;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="contribution_has_audience")
@Description("A relation that associates a contribution with an audience.")
public class ContributionAudience extends TypedTimedBE implements Identifiable<Long>{

	@Id
	@Column(name="id_contribution_audience", nullable=false)
    @GeneratedValue(strategy = GenerationType.AUTO)	
	@Setter(AccessLevel.PRIVATE) 
	@Description("The primary key.")
	private Long id;
	
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_contribution")
    @Description("The contribution that should be related to an audience.")
    private Contribution contribution;
    
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "fk_audience")
    @Description("The audience that should be related to a contribution.")
    private Audience audience;
    		
}
